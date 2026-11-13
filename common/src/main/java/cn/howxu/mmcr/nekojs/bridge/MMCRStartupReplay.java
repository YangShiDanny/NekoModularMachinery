package cn.howxu.mmcr.nekojs.bridge;

import cn.howxu.mmcr.internal.registration.StartupContentRegistration;
import cn.howxu.mmcr.nekojs.MMCRNekoJS;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;

/**
 * Re-opens MMCR's machine-definition window when NekoJS lost the race against it.
 *
 * <p><b>Why this exists.</b> NeoForge does not post {@code FMLConstructModEvent} once after every
 * mod constructor; {@code ModLoader.lambda$constructMods$0} fires it <em>per mod</em>, immediately
 * after that mod's own constructor returns, inside the parallel worker that built it. MMCR collects
 * its machine definitions from that event ({@code StartupContentRegistration.registerProductionForModStartup}
 * → {@code ServiceLoader.load(MachineDefinitionProvider)}).
 *
 * <p>NekoJS, on the other hand, only runs {@code startup_scripts} inside {@code NekoJSMod}'s
 * constructor. Because mod construction follows the load order ({@code mmcr} before {@code nekojs}),
 * MMCR's window is already closed — with an empty store — before the first script line executes.
 * The definitions window therefore never sees a script-declared machine, and MMCR later rejects the
 * machine's structure with {@code Unknown machine definition}.
 *
 * <p><b>What we do about it.</b> The moment NekoJS reports its startup scripts finished, we rewind
 * MMCR's startup phase with {@code StartupContentRegistration.resetForTesting()} and run
 * {@code registerProductionForModStartup(bus)} again. Nothing has been committed at that point
 * ({@code completeProductionForModStartup} only runs at {@code FMLCommonSetupEvent}, which is still
 * ahead of us), so the second pass simply re-collects — this time with a populated store — and MMCR's
 * structure window later accepts the machine.
 *
 * <p>Both entry points are public members of MMCR 0.1.7 on both MC lines. If a future MMCR renames
 * or removes them the replay degrades to a loud log line instead of taking the game down.
 */
public final class MMCRStartupReplay {
    private MMCRStartupReplay() {}

    private static boolean attempted;

    /** System property escape hatch: {@code -Dmmcrnekojs.disableStartupReplay=true}. */
    private static final String DISABLE_PROPERTY = "mmcrnekojs.disableStartupReplay";

    /**
     * Called from {@code MMCREvents.startup} dispatch completion, i.e. after the scripts declared
     * their machines but before {@code FMLCommonSetupEvent} opens MMCR's structure window.
     */
    public static synchronized void replayIfNeeded() {
        if (attempted) return;
        if (Boolean.getBoolean(DISABLE_PROPERTY)) {
            MMCRNekoJS.LOG.warn("MMCR/NekoJS startup replay disabled by -D{}=true; "
                    + "script-declared machines will stay unknown to MMCR", DISABLE_PROPERTY);
            return;
        }

        var machines = DeclarationStore.machines();
        if (machines.isEmpty()) return; // nothing to submit; MMCR's own pass is enough

        // Whether MMCR's window already ran or not, replaying is the correct move: MMCR's pass is
        // either behind us (its usual position) or still ahead (then our pass simply becomes the
        // one that counts, with a populated store). No content is committed before
        // FMLCommonSetupEvent, so rewinding cannot destroy anything.
        boolean windowAlreadyRan = DeclarationStore.isDefinitionsServed();

        attempted = true;
        try {
            // Undo the empty first pass: phase -> NOT_STARTED, pending definitions -> null.
            StartupContentRegistration.resetForTesting();
            IEventBus bus = mmcrEventBus();
            if (bus != null) {
                StartupContentRegistration.registerProductionForModStartup(bus);
            } else {
                StartupContentRegistration.registerProductionForModStartup();
            }
            MMCRNekoJS.LOG.info("MMCR/NekoJS submitted {} machine definition(s) through a {} window: {}",
                    machines.size(),
                    windowAlreadyRan ? "reopened" : "pre-empted",
                    machines.keySet());
        } catch (Throwable t) {
            // Never let a compat shim abort mod loading.
            MMCRNekoJS.LOG.error("MMCR/NekoJS could not replay the definition window ({}). "
                            + "Script-declared machines will not exist this session. "
                            + "This usually means MMCR's internal registration API moved.",
                    t.toString());
        }
    }

    /**
     * MMCR's own mod bus, so that the {@code RegisterMachineDefinitionsEvent} we re-post still
     * reaches listeners other addons registered on MMCR's bus.
     */
    private static IEventBus mmcrEventBus() {
        try {
            ModList modList = ModList.get();
            if (modList != null) {
                var container = modList.getModContainerById("mmcr").orElse(null);
                if (container != null) {
                    IEventBus bus = container.getEventBus();
                    if (bus != null) return bus;
                }
            }
        } catch (RuntimeException e) {
            MMCRNekoJS.LOG.debug("MMCR/NekoJS could not resolve mmcr's mod bus: {}", e.toString());
        }
        // Caller falls back to MMCR's no-arg overload, which posts on NeoForge.EVENT_BUS. That is
        // enough for the ServiceLoader half of the window, which is where our definitions come from.
        return null;
    }
}
