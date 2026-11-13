package cn.howxu.mmcr.nekojs.bridge;

import cn.howxu.mmcr.nekojs.MMCRNekoJS;
import cn.howxu.mmcr.nekojs.version.Ids;
import cn.howxu.mmcr.publicapi.event.RegisterJeiRecipeInformationEvent;
import cn.howxu.mmcr.publicapi.event.RegisterJeiWorkstationsEvent;
import cn.howxu.mmcr.publicapi.event.RegisterMachineRecipesEvent;
import cn.howxu.mmcr.publicapi.event.RegisterMachineStructuresEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

/**
 * Feeds script declarations into MMCR's Public API registration events.
 *
 * <p>Structures/levels/modifiers arrive in {@code FMLCommonSetupEvent}, recipes in
 * {@code ModifyDefaultComponentsEvent}. Both are long after NekoJS finished loading
 * {@code STARTUP} scripts, so the declarations are always ready. JEI hooks are client-only
 * events MMCR posts while building its category list.
 */
@EventBusSubscriber(modid = MMCRNekoJS.MODID)
public final class MMCRRegistrars {
    private MMCRRegistrars() {}

    @SubscribeEvent
    public static void onStructures(RegisterMachineStructuresEvent event) {
        // Spec overloads: declarations were already frozen into specs during STARTUP.
        DeclarationStore.levelTypes().values().forEach(event::registerLevelType);
        DeclarationStore.levels().values().forEach(event::registerLevel);
        DeclarationStore.modifiers().forEach((id, bundle) -> event.registerModifier(Ids.parse(id), bundle));
        DeclarationStore.modifierItems().forEach((item, modifier) ->
                event.registerModifierItem(Ids.parse(item), Ids.parse(modifier)));
        DeclarationStore.structures().values().forEach(event::registerStructure);

        int structures = DeclarationStore.structures().size();
        if (structures > 0) {
            MMCRNekoJS.LOG.info("MMCR/NekoJS submitted {} structure(s), {} level(s), {} modifier(s)",
                    structures, DeclarationStore.levels().size(), DeclarationStore.modifiers().size());
        }
    }

    @SubscribeEvent
    public static void onRecipes(RegisterMachineRecipesEvent event) {
        var recipes = DeclarationStore.recipes();
        recipes.values().forEach(event::registerRecipe);
        if (!recipes.isEmpty()) {
            MMCRNekoJS.LOG.info("MMCR/NekoJS submitted {} static recipe(s)", recipes.size());
        }
    }

    @SubscribeEvent
    public static void onWorkstations(RegisterJeiWorkstationsEvent event) {
        DeclarationStore.poolWorkstations().forEach(pair ->
                event.addRecipePoolWorkstation(Ids.parse(pair[0]), Ids.parse(pair[1])));
        DeclarationStore.machineWorkstations().forEach(pair ->
                event.addMachineWorkstation(Ids.parse(pair[0]), Ids.parse(pair[1])));
    }

    @SubscribeEvent
    public static void onInformation(RegisterJeiRecipeInformationEvent event) {
        for (DeclarationStore.RecipeInfo info : DeclarationStore.recipeInfos()) {
            if (info.pool()) {
                event.registerRecipePool(Ids.parse(info.id()), info.key(), info.args());
            } else {
                event.registerRecipe(Ids.parse(info.id()), info.key(), info.args());
            }
        }
    }
}
