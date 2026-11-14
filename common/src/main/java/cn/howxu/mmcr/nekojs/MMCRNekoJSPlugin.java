package cn.howxu.mmcr.nekojs;

import cn.howxu.mmcr.nekojs.bridge.DeclarationStore;
import cn.howxu.mmcr.nekojs.bridge.MMCRStartupReplay;
import cn.howxu.mmcr.nekojs.recipe.MachineRecipeHandler;
import cn.howxu.mmcr.nekojs.recipe.MachineRecipeSchema;
import com.tkisor.nekojs.api.NekoJSPlugin;
import com.tkisor.nekojs.api.ScriptType;
import com.tkisor.nekojs.api.annotation.RegisterNekoJSPlugin;
import com.tkisor.nekojs.api.data.BindingRegistry;
import com.tkisor.nekojs.api.event.EventGroupRegistry;
import com.tkisor.nekojs.api.recipe.RecipeNamespaceEntry;
import com.tkisor.nekojs.core.plugin.RecipeNamespaceRegister;
import com.tkisor.nekojs.core.plugin.RecipeSchemaRegister;
import com.tkisor.nekojs.wrapper.event.server.RecipeEventJS;

/**
 * NekoJS plugin entry point, discovered through {@code @RegisterNekoJSPlugin}.
 *
 * <p>Only {@code cn.howxu.mmcr.publicapi.*} is referenced — never {@code internal.*} — so this
 * addon keeps working across MMCR patch releases.
 *
 * <p>Ordering note: NekoJS loads {@code STARTUP} scripts inside its own mod constructor, i.e.
 * before NeoForge posts {@code FMLConstructModEvent}. MMCR collects machine definitions there,
 * which is exactly why machine/structure/level/modifier declarations live in {@code startup_scripts}.
 */
@RegisterNekoJSPlugin(priority = 900, requiredMods = {"mmcr", "nekojs"})
public final class MMCRNekoJSPlugin implements NekoJSPlugin {

    @Override
    public void registerBinding(BindingRegistry registry) {
        registry.register("MMCR", new MMCRBinding());
    }

    @Override
    public void registerEvents(EventGroupRegistry registry) {
        registry.register(MMCRNekoEvents.GROUP);
    }

    @Override
    public void registerRecipeSchemas(RecipeSchemaRegister registry) {
        registry.register(MachineRecipeSchema.NAMESPACE, MachineRecipeSchema.NAME, MachineRecipeSchema.build());
    }

    @Override
    public void registerRecipeNamespaces(RecipeNamespaceRegister registry) {
        // event.recipes.mmcr.machine_recipe(pool, ticks, requirements)
        //
        // The handler keeps the event so it can hand the finished JSON back through
        // RecipeEventJS.custom(JsonObject), which registers the recipe *and* returns the
        // RecipeJsonBuilder the script chains .id(...) on. Returning the bare JsonObject made
        // .id(...) fail with "Unknown identifier: id", and no recipe was ever registered.
        registry.register(RecipeNamespaceEntry.of(MachineRecipeSchema.NAMESPACE,
                event -> event instanceof RecipeEventJS js
                        ? new MachineRecipeHandler(js)
                        : new MachineRecipeHandler(),
                MachineRecipeHandler.class));
    }

    @Override
    public void beforeScriptsLoaded(ScriptType type) {
        if (type == ScriptType.STARTUP) {
            DeclarationStore.resetStartup();
        } else if (type == ScriptType.SERVER) {
            DeclarationStore.beginServer();
        }
    }

    @Override
    public void afterScriptsLoaded(ScriptType type) {
        switch (type) {
            case STARTUP -> {
                MMCRNekoEvents.postStartup();
                DeclarationStore.markStartupPosted();
                var machines = DeclarationStore.machines();
                MMCRNekoJS.LOG.info("MMCR/NekoJS startup declarations: {} machine(s) {}, {} structure(s), "
                                + "{} level(s), {} modifier(s)",
                        machines.size(), machines.keySet(),
                        DeclarationStore.structures().size(),
                        DeclarationStore.levels().size(),
                        DeclarationStore.modifiers().size());
                // MMCR's definition window already closed inside its own construct event, which
                // NeoForge fires per mod right after that mod's constructor. Re-open it now that
                // the scripts have actually declared something.
                MMCRStartupReplay.replayIfNeeded();
            }
            case SERVER -> {
                MMCRNekoEvents.postServer();
                DeclarationStore.markServerPosted();
                MMCRNekoJS.LOG.debug("MMCR/NekoJS server scripts loaded ({} startup recipe(s) already committed)",
                        DeclarationStore.recipes().size());
            }
            case CLIENT -> MMCRNekoEvents.postClient();
            default -> { }
        }
    }

    @Override
    public void afterInit() {
        MMCRNekoJS.LOG.info("MMCR <-> NekoJS bridge ready (KubeJS-free)");
    }
}
