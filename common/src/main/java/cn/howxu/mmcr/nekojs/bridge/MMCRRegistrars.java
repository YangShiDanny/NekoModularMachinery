package cn.howxu.mmcr.nekojs.bridge;

import cn.howxu.mmcr.nekojs.MMCRNekoJS;
import cn.howxu.mmcr.nekojs.version.Ids;
import cn.howxu.mmcr.publicapi.event.RegisterJeiRecipeInformationEvent;
import cn.howxu.mmcr.publicapi.event.RegisterJeiWorkstationsEvent;
import cn.howxu.mmcr.publicapi.event.RegisterMachineRecipesEvent;
import cn.howxu.mmcr.publicapi.event.RegisterMachineStructuresEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

import java.util.ArrayList;
import java.util.List;

/**
 * Feeds script declarations into MMCR's Public API registration events.
 *
 * <p>Structures/levels/modifiers arrive in {@code FMLCommonSetupEvent}, recipes in
 * {@code ModifyDefaultComponentsEvent}. Both are long after NekoJS finished loading
 * {@code STARTUP} scripts, so the declarations are always ready. JEI hooks are client-only
 * events MMCR posts while building its category list.
 *
 * <p>Every individual registration is defensive: MMCR rejects — with an unchecked exception — any
 * structure/level/modifier whose machine is not part of the definitions window. A single unknown id
 * used to abort {@code FMLCommonSetupEvent} and take the whole game down, so failures are now
 * collected, logged once, and skipped.
 */
@EventBusSubscriber(modid = MMCRNekoJS.MODID)
public final class MMCRRegistrars {
    private MMCRRegistrars() {}

    @SubscribeEvent
    public static void onStructures(RegisterMachineStructuresEvent event) {
        List<String> skipped = new ArrayList<>();

        // Spec overloads: declarations were already frozen into specs during STARTUP.
        DeclarationStore.levelTypes().forEach((id, spec) ->
                attempt(skipped, "levelType " + id, () -> event.registerLevelType(spec)));
        DeclarationStore.levels().forEach((id, spec) ->
                attempt(skipped, "level " + id, () -> event.registerLevel(spec)));
        DeclarationStore.modifiers().forEach((id, bundle) ->
                attempt(skipped, "modifier " + id, () -> event.registerModifier(Ids.parse(id), bundle)));
        DeclarationStore.modifierItems().forEach((item, modifier) ->
                attempt(skipped, "modifierItem " + item, () ->
                        event.registerModifierItem(Ids.parse(item), Ids.parse(modifier))));
        DeclarationStore.structures().forEach((id, spec) ->
                attempt(skipped, "structure " + id, () -> event.registerStructure(spec)));

        int structures = DeclarationStore.structures().size() - countKind(skipped, "structure ");
        if (structures > 0) {
            MMCRNekoJS.LOG.info("MMCR/NekoJS submitted {} structure(s), {} level(s), {} modifier(s)",
                    structures, DeclarationStore.levels().size(), DeclarationStore.modifiers().size());
        }
        reportSkipped(skipped);
    }

    @SubscribeEvent
    public static void onRecipes(RegisterMachineRecipesEvent event) {
        List<String> skipped = new ArrayList<>();
        DeclarationStore.recipes().forEach((id, spec) ->
                attempt(skipped, "recipe " + id, () -> event.registerRecipe(spec)));

        int recipes = DeclarationStore.recipes().size() - skipped.size();
        if (recipes > 0) {
            MMCRNekoJS.LOG.info("MMCR/NekoJS submitted {} static recipe(s)", recipes);
        }
        reportSkipped(skipped);
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

    // ------------------------------------------------------------------ helpers

    private static void attempt(List<String> skipped, String what, Runnable action) {
        try {
            action.run();
        } catch (RuntimeException e) {
            skipped.add(what + " -> " + rootMessage(e));
        }
    }

    private static int countKind(List<String> skipped, String prefix) {
        int n = 0;
        for (String s : skipped) if (s.startsWith(prefix)) n++;
        return n;
    }

    private static String rootMessage(Throwable t) {
        Throwable root = t;
        while (root.getCause() != null && root.getCause() != root) root = root.getCause();
        return root.getMessage() == null ? root.toString() : root.getMessage();
    }

    /**
     * One aggregated error instead of a stack trace per declaration. The overwhelmingly common
     * cause is a machine that never reached MMCR's definition window — see
     * {@link MMCRStartupReplay} for the timing involved.
     */
    private static void reportSkipped(List<String> skipped) {
        if (skipped.isEmpty()) return;
        int shown = Math.min(skipped.size(), 10);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < shown; i++) {
            if (i > 0) sb.append('\n');
            sb.append("  - ").append(skipped.get(i));
        }
        if (skipped.size() > shown) {
            sb.append("\n  ... and ").append(skipped.size() - shown).append(" more");
        }
        MMCRNekoJS.LOG.error("MMCR/NekoJS skipped {} declaration(s) that MMCR refused:\n{}\n"
                        + "  Most likely cause: the machine was declared after MMCR's definition window "
                        + "closed. Look for 'replayed ... machine definition(s)' earlier in the log.",
                skipped.size(), sb);
    }
}
