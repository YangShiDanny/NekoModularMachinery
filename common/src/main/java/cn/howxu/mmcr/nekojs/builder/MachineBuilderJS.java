package cn.howxu.mmcr.nekojs.builder;

import cn.howxu.mmcr.nekojs.MMCRNekoJS;
import cn.howxu.mmcr.nekojs.bridge.DeclarationStore;
import cn.howxu.mmcr.nekojs.version.Ids;
import cn.howxu.mmcr.publicapi.Machines;
import cn.howxu.mmcr.publicapi.behavior.RecipeHooks;
import cn.howxu.mmcr.publicapi.behavior.TickHooks;
import cn.howxu.mmcr.publicapi.machine.MachineKind;
import cn.howxu.mmcr.publicapi.machine.MachineSpec;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Machine builder, mirroring MMCR's KubeJS {@code MachineBuilderJS}.
 *
 * <p>Machines are the very first thing MMCR collects, from {@code FMLConstructModEvent}.
 * NekoJS STARTUP scripts finish before that, so {@code MMCREvents.startup} is the only valid
 * place to call {@link #register()}.
 */
public final class MachineBuilderJS {
    private final String id;
    private final List<String> pools = new ArrayList<>();
    private String displayNameKey;
    private String frontTexture, sideTexture, topTexture, bottomTexture;
    private String basicBlock, controllerBaseTexture, formedPortBaseTexture;
    private String idleOverlay, activeOverlay;
    private String runningSound, finishSound;
    private boolean allowVerticalFacing, fullyRotationallySymmetric, requireVerticalFacing;
    private boolean allowModifiers, allowMultithreading, allowParallelism;
    private long maxParallelAmount = 1;
    private int factoryThreads;
    private int networkInterfaceMax = -1, networkInterfaceConnections = -1;
    private Consumer<TickHooks> tickHandler;
    private Consumer<RecipeHooks> recipeHandler;
    private String role;
    private final List<String> acceptedModules = new ArrayList<>();

    public MachineBuilderJS(String id) {
        this.id = Objects.requireNonNull(id, "machine id");
        this.pools.add(id); // default pool: one named after the machine
    }

    // ---- identity / pooling ----
    public MachineBuilderJS displayNameKey(String key) { this.displayNameKey = key; return this; }

    public MachineBuilderJS recipePool(String... ids) {
        pools.clear();
        for (String pool : ids) pools.add(pool);
        return this;
    }

    public MachineBuilderJS recipePool(String pool) { return recipePool(new String[]{pool}); }

    // ---- host / module roles ----

    /**
     * Declares this machine a {@code HOST} and lists the module machines it accepts.
     *
     * <p>A host <b>must</b> declare at least one accepted module, and a non-host must not declare
     * any - MMCR rejects both cases when it validates the definition. Couplers in the structure
     * only provide the physical anchor; they do not imply the acceptance relation.
     */
    public MachineBuilderJS host(String... moduleIds) {
        this.role = "HOST";
        this.acceptedModules.clear();
        if (moduleIds != null) {
            for (String module : moduleIds) {
                if (module != null && !module.isBlank()) acceptedModules.add(module);
            }
        }
        return this;
    }

    /** Declares this machine a {@code MODULE}: a host can attach it through a module coupler. */
    public MachineBuilderJS module() {
        this.role = "MODULE";
        this.acceptedModules.clear();
        return this;
    }

    /** Raw form, accepting {@code NORMAL} / {@code HOST} / {@code MODULE} (case-insensitive). */
    public MachineBuilderJS role(String kind) { this.role = kind; return this; }

    // ---- appearance ----
    public MachineBuilderJS appearance(String blockId) { this.basicBlock = blockId; return this; }

    public MachineBuilderJS controllerTextures(String front, String side, String top, String bottom) {
        this.frontTexture = front;
        this.sideTexture = side;
        this.topTexture = top;
        this.bottomTexture = bottom;
        return this;
    }

    /** Uniform texture on every face; {@code front} wins on the controller face. */
    public MachineBuilderJS controllerTextures(String front, String otherFaces) {
        this.frontTexture = front;
        this.sideTexture = otherFaces;
        this.topTexture = otherFaces;
        this.bottomTexture = otherFaces;
        return this;
    }

    public MachineBuilderJS controllerFrontTexture(String id) { this.frontTexture = id; return this; }
    public MachineBuilderJS controllerSideTexture(String id) { this.sideTexture = id; return this; }
    public MachineBuilderJS controllerTopTexture(String id) { this.topTexture = id; return this; }
    public MachineBuilderJS controllerBottomTexture(String id) { this.bottomTexture = id; return this; }
    public MachineBuilderJS controllerBaseTexture(String id) { this.controllerBaseTexture = id; return this; }
    public MachineBuilderJS formedPortBaseTexture(String id) { this.formedPortBaseTexture = id; return this; }
    public MachineBuilderJS controllerIdleOverlayTexture(String id) { this.idleOverlay = id; return this; }
    public MachineBuilderJS controllerActiveOverlayTexture(String id) { this.activeOverlay = id; return this; }

    // ---- sounds ----
    public MachineBuilderJS runningSound(String id) { this.runningSound = id; return this; }
    public MachineBuilderJS finishSound(String id) { this.finishSound = id; return this; }

    // ---- controller orientation ----
    public MachineBuilderJS allowVerticalFacing() { this.allowVerticalFacing = true; return this; }
    public MachineBuilderJS fullyRotationallySymmetric() { this.fullyRotationallySymmetric = true; return this; }
    public MachineBuilderJS requireVerticalFacing() { this.requireVerticalFacing = true; return this; }

    // ---- modifiers / parallelism / factory ----
    public MachineBuilderJS allowModifiers() { this.allowModifiers = true; return this; }
    public MachineBuilderJS allowMultithreading() { this.allowMultithreading = true; return this; }
    public MachineBuilderJS allowParallelism() { this.allowParallelism = true; return this; }
    public MachineBuilderJS maxParallelAmount(long amount) { this.maxParallelAmount = amount; return this; }
    public MachineBuilderJS maxParallelism(long amount) { return maxParallelAmount(amount); }
    public MachineBuilderJS factoryThreads(int threads) { this.factoryThreads = threads; return this; }

    /** Enables the integrated network interface with the given caps. */
    public MachineBuilderJS networkInterface(int maxCount, int maxConnections) {
        this.networkInterfaceMax = maxCount;
        this.networkInterfaceConnections = maxConnections;
        return this;
    }

    /** Callbacks receive MMCR's {@link TickHooks} / {@link RecipeHooks} objects. */
    public MachineBuilderJS tickBehavior(Consumer<TickHooks> handler) { this.tickHandler = handler; return this; }
    public MachineBuilderJS recipeBehavior(Consumer<RecipeHooks> handler) { this.recipeHandler = handler; return this; }

    public MachineBuilderJS register() {
        // Deliberately no Machines.isRegistrationOpen() gate. That flag is only true while
        // MMCR's own mod constructor has already run begin() and the content coordinator has not
        // committed yet — which makes it depend on whether MMCR or NekoJS happens to be
        // constructed first. The draft below is pure data; MMCR validates it when it reads
        // DeclarationStore through the ServiceLoader during FMLConstructModEvent.
        var draft = Machines.machine(Ids.parse(id));
        draft.recipePool(Ids.parseAll(pools.toArray(new String[0])));
        if (displayNameKey != null) draft.displayNameKey(displayNameKey);

        if (frontTexture != null || sideTexture != null || topTexture != null || bottomTexture != null
                || allowVerticalFacing || fullyRotationallySymmetric || requireVerticalFacing) {
            draft.controller(c -> {
                if (frontTexture != null && sideTexture != null && topTexture != null && bottomTexture != null) {
                    c.textures(Ids.parse(frontTexture), Ids.parse(sideTexture),
                            Ids.parse(topTexture), Ids.parse(bottomTexture));
                } else {
                    if (frontTexture != null) c.frontTexture(Ids.parse(frontTexture));
                    if (sideTexture != null) c.sideTexture(Ids.parse(sideTexture));
                    if (topTexture != null) c.topTexture(Ids.parse(topTexture));
                    if (bottomTexture != null) c.bottomTexture(Ids.parse(bottomTexture));
                }
                c.allowVerticalFacing(allowVerticalFacing)
                        .fullyRotationallySymmetric(fullyRotationallySymmetric)
                        .requireVerticalFacing(requireVerticalFacing);
            });
        }

        if (basicBlock != null || controllerBaseTexture != null || formedPortBaseTexture != null
                || idleOverlay != null || activeOverlay != null) {
            draft.appearance(a -> {
                if (basicBlock != null) a.machineBasicBlock(Ids.parse(basicBlock));
                if (controllerBaseTexture != null) a.controllerBaseTexture(Ids.parse(controllerBaseTexture));
                if (formedPortBaseTexture != null) a.formedPortBaseTexture(Ids.parse(formedPortBaseTexture));
                if (idleOverlay != null) a.controllerIdleOverlayTexture(Ids.parse(idleOverlay));
                if (activeOverlay != null) a.controllerActiveOverlayTexture(Ids.parse(activeOverlay));
            });
        }

        if (factoryThreads > 0) {
            draft.factory(f -> f.hasFactory(true).threadLimit(factoryThreads));
        }

        draft.allowModifiers(allowModifiers)
                .allowMultithreading(allowMultithreading)
                .parallelizable(allowParallelism)
                .maxParallelism(maxParallelAmount)
                .maxParallelAmount((int) Math.min(maxParallelAmount, Integer.MAX_VALUE));

        if (networkInterfaceMax >= 0) {
            draft.networkInterface(networkInterfaceMax, Math.max(networkInterfaceConnections, 0));
        }
        if (runningSound != null) draft.runningSound(Ids.parse(runningSound));
        if (finishSound != null) draft.finishSound(Ids.parse(finishSound));
        if (tickHandler != null) draft.tickBehavior(tickHandler);
        if (recipeHandler != null) draft.recipeBehavior(recipeHandler);

        if (role != null && !role.isBlank()) {
            MachineKind kind = MachineKind.valueOf(role.trim().toUpperCase(Locale.ROOT));
            draft.role(kind);
            if (kind == MachineKind.HOST) {
                for (String module : acceptedModules) draft.acceptedModule(Ids.parse(module));
            } else if (!acceptedModules.isEmpty()) {
                MMCRNekoJS.LOG.warn("MMCR/NekoJS: machine {} is {} but declares accepted module(s) {}; "
                        + "MMCR rejects that, so they were dropped", id, kind, acceptedModules);
            }
        }

        MachineSpec spec = draft.build();
        DeclarationStore.putMachine(id, spec);
        return this;
    }
}
