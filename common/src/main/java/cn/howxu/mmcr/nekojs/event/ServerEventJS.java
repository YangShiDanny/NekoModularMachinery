package cn.howxu.mmcr.nekojs.event;

import cn.howxu.mmcr.nekojs.MMCRApiJS;

/**
 * Event object received by {@code MMCREvents.server} callbacks.
 *
 * <p>Nothing can be <em>registered</em> from here. MMCR opens its structure window during
 * {@code FMLCommonSetupEvent} and its recipe window during {@code ModifyDefaultComponentsEvent},
 * both long before NekoJS loads {@code SERVER} scripts. Exposing structure or recipe builders here
 * would hand out objects whose declarations are silently dropped — the KubeJS integration avoids
 * that by owning a dedicated content-reload transaction, which this bridge does not have.
 *
 * <p>Use this event for read-only work and for assembling requirement maps with
 * {@link MMCRApiJS} (for example {@code api.machineRecipe(...)}). To actually add a reloadable
 * recipe, register it as data JSON through NekoJS's {@code ServerEvents.recipes} — see
 * {@code event.recipes.mmcr.machine_recipe(...)} in the wiki.
 */
public final class ServerEventJS {
    private final MMCRApiJS api = new MMCRApiJS();

    public MMCRApiJS getAPI() { return api; }
}
