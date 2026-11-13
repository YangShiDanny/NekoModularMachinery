package cn.howxu.mmcr.nekojs.builder;

import cn.howxu.mmcr.nekojs.MMCRNekoJS;
import cn.howxu.mmcr.nekojs.bridge.DeclarationStore;
import cn.howxu.mmcr.nekojs.version.Ids;
import cn.howxu.mmcr.publicapi.Recipes;
import cn.howxu.mmcr.publicapi.recipe.RecipeDraft;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.material.Fluid;

/**
 * Programmatic recipe builder backed by MMCR's {@link RecipeDraft}.
 *
 * <p>MMCR collects static recipes once, during startup ({@code ModifyDefaultComponentsEvent}),
 * so this is only useful from {@code MMCREvents.startup}. For reloadable recipes prefer data JSON
 * with {@code "type": "mmcr:machine_recipe"} and {@code /nekojs reload}.
 */
public final class RecipeBuilderJS {
    private final String id;
    private final RecipeDraft draft;

    public RecipeBuilderJS(String id) {
        this.id = id;
        if (!Recipes.isRegistrationOpen()) {
            throw new IllegalStateException("MMCR recipe registration is closed; build recipe '"
                    + id + "' from MMCREvents.startup, or use data JSON + /nekojs reload");
        }
        this.draft = Recipes.recipe(Ids.parse(id));
    }

    // ---- pooling / timing ----
    public RecipeBuilderJS recipePool(String pool) { draft.recipePool(Ids.parse(pool)); return this; }
    public RecipeBuilderJS duration(int ticks) { draft.duration(ticks); return this; }
    public RecipeBuilderJS tickTime(int ticks) { return duration(ticks); }
    public RecipeBuilderJS priority(int value) { draft.priority(value); return this; }
    public RecipeBuilderJS maxThreads(int value) { draft.maxThreads(value); return this; }
    public RecipeBuilderJS parallelized(boolean value) { draft.parallelized(value); return this; }
    public RecipeBuilderJS allowPartialOutputs(boolean value) { draft.allowPartialOutputs(value); return this; }
    public RecipeBuilderJS cancelIfPerTickFails(boolean value) { draft.cancelIfPerTickFails(value); return this; }

    // ---- items ----
    public RecipeBuilderJS inputItem(Item item, int count) { draft.inputItem(item, count); return this; }
    public RecipeBuilderJS inputItem(Ingredient ingredient, int count) { draft.inputItem(ingredient, count); return this; }
    public RecipeBuilderJS inputItem(TagKey<Item> tag, int count) { draft.inputItem(tag, count); return this; }
    public RecipeBuilderJS outputItem(Item item, int count) { draft.outputItem(item, count); return this; }
    public RecipeBuilderJS outputItem(ItemStack stack) { draft.outputItem(stack); return this; }
    public RecipeBuilderJS outputChance(ItemStack stack, float chance) { draft.outputChance(stack, chance); return this; }

    // ---- fluids ----
    public RecipeBuilderJS inputFluid(Fluid fluid, int amount) { draft.inputFluid(fluid, amount); return this; }
    public RecipeBuilderJS inputFluid(Fluid fluid, int amount, float chance) { draft.inputFluid(fluid, amount, chance); return this; }
    public RecipeBuilderJS outputFluid(Fluid fluid, int amount) { draft.outputFluid(fluid, amount); return this; }

    // ---- energy ----
    public RecipeBuilderJS inputEnergy(long fePerTick) { draft.inputEnergy(fePerTick); return this; }
    public RecipeBuilderJS outputEnergy(long fePerTick) { draft.outputEnergy(fePerTick); return this; }

    // ---- requirements ----
    public RecipeBuilderJS levelRequirement(String typeId, String levelId) {
        draft.levelRequirement(Ids.parse(typeId), Ids.parse(levelId)); return this;
    }
    public RecipeBuilderJS stageRequirement(int minStage) { draft.stageRequirement(minStage); return this; }
    public RecipeBuilderJS requiredHost(String hostId) { draft.requiredHost(Ids.parse(hostId)); return this; }
    public RecipeBuilderJS modifier(String modifierId) { draft.modifier(Ids.parse(modifierId)); return this; }

    public void build() {
        DeclarationStore.putRecipe(id, draft.build());
        MMCRNekoJS.LOG.debug("MMCR/NekoJS declared recipe {}", id);
    }
}
