package cn.howxu.mmcr.nekojs.event;

import cn.howxu.mmcr.nekojs.MMCRApiJS;
import cn.howxu.mmcr.nekojs.builder.RecipeBuilderJS;

/**
 * Event object received by {@code MMCREvents.server} callbacks.
 *
 * <p>SERVER scripts load long after MMCR froze its machine/structure/recipe registries, so no
 * shape declaration can be accepted here. Use {@code MMCREvents.startup} for machines,
 * structures, levels and modifiers, and data JSON (reloadable via {@code /nekojs reload}) for
 * recipes.
 */
public final class ServerEventJS {
    private final MMCRApiJS api = new MMCRApiJS();

    public MMCRApiJS getAPI() { return api; }

    /**
     * Programmatic recipe builder. Prefer {@code ServerEvents.recipes} + {@code event.custom(...)}
     * or data JSON: MMCR collects static recipes during startup, so recipes built here after
     * world load are logged and ignored.
     */
    public RecipeBuilderJS createRecipe(String id) { return new RecipeBuilderJS(id); }
}
