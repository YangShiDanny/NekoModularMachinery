package cn.howxu.mmcr.nekojs;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;

/**
 * Mod entry point.
 *
 * <p>NeoForge only scans a mod's classes for {@code @EventBusSubscriber} when a {@link Mod}
 * instance exists for that id. Without this class, MMCR's registration events would never be
 * observed and no script declaration would reach the game. Everything else lives behind the
 * NekoJS plugin ({@link MMCRNekoJSPlugin}) and the {@code ServiceLoader} provider
 * ({@code bridge.DefinitionsProvider}).
 */
@Mod(MMCRNekoJS.MODID)
public final class MMCRNekoJS {

    public static final String MODID = "mmcr_nekojs";
    public static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger("MMCR/NekoJS");

    public MMCRNekoJS(IEventBus modBus, ModContainer container) {
        // Intentionally empty: MMCR drives every registration window itself.
        LOG.debug("MMCR/NekoJS bridge constructed");
    }
}
