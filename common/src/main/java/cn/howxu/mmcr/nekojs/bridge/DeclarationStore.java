package cn.howxu.mmcr.nekojs.bridge;

import cn.howxu.mmcr.publicapi.machine.MachineSpec;
import cn.howxu.mmcr.publicapi.presentation.ControllerTextContext;
import cn.howxu.mmcr.publicapi.recipe.RecipeSpec;
import cn.howxu.mmcr.publicapi.recipe.modifier.ModifierBundle;
import cn.howxu.mmcr.publicapi.structure.StructureSpec;
import cn.howxu.mmcr.publicapi.structure.level.LevelTypeSpec;
import cn.howxu.mmcr.publicapi.structure.level.MachineLevelSpec;

import java.util.*;
import java.util.function.Consumer;

/**
 * Collects declarations made by scripts until MMCR's registration events are posted.
 *
 * <p>MMCR opens three windows, in this order, all before the world list is shown:
 * <ol>
 *   <li><b>definitions</b> — collected during {@code FMLConstructModEvent}, fed by
 *       {@link java.util.ServiceLoader} via {@link DefinitionsProvider};</li>
 *   <li><b>structures</b> — posted on {@code NeoForge.EVENT_BUS} during {@code FMLCommonSetupEvent};</li>
 *   <li><b>recipes</b> — posted during {@code ModifyDefaultComponentsEvent}.</li>
 * </ol>
 *
 * <p>NekoJS loads {@code STARTUP} scripts inside {@code NekoJSMod}'s constructor, which NeoForge
 * runs <em>before</em> {@code FMLConstructModEvent}. Everything declared from a
 * {@code MMCREvents.startup} listener is therefore already in this store by the time MMCR asks,
 * which is why machine/structure/level/modifier declarations all belong in {@code startup_scripts}.
 *
 * <p>{@code SERVER} scripts load far too late for either window; they may only add
 * <em>data-driven</em> recipes (JSON) through {@code MMCREvents.server}, which MMCR reloads from
 * its recipe directory. See README section 7.
 */
public final class DeclarationStore {
    private DeclarationStore() {}

    // ---- window 1: machine definitions (ServiceLoader) ----
    private static final Map<String, MachineSpec> MACHINES = new LinkedHashMap<>();

    // ---- window 2: structures / levels / modifiers (NeoForge.EVENT_BUS) ----
    private static final Map<String, StructureSpec> STRUCTURES = new LinkedHashMap<>();
    private static final Map<String, LevelTypeSpec> LEVEL_TYPES = new LinkedHashMap<>();
    private static final Map<String, MachineLevelSpec> LEVELS = new LinkedHashMap<>();
    private static final Map<String, ModifierBundle> MODIFIERS = new LinkedHashMap<>();
    private static final Map<String, String> MODIFIER_ITEMS = new LinkedHashMap<>();

    // ---- window 3: static recipes (NeoForge.EVENT_BUS) ----
    private static final Map<String, RecipeSpec> RECIPES = new LinkedHashMap<>();

    // ---- client-side presentation data ----
    private static final Map<String, Consumer<ControllerTextContext>> SCREEN_TEXT = new LinkedHashMap<>();
    private static final List<String[]> POOL_WORKSTATIONS = new ArrayList<>();
    private static final List<String[]> MACHINE_WORKSTATIONS = new ArrayList<>();
    private static final List<RecipeInfo> RECIPE_INFOS = new ArrayList<>();

    /** One JEI category information entry; {@code pool} selects pool- vs machine-scoped lookup. */
    public record RecipeInfo(boolean pool, String id, String key, Object[] args) {}

    private static volatile boolean startupPosted;
    private static volatile boolean serverPosted;
    private static volatile boolean definitionsServed;

    // ------------------------------------------------------------------ lifecycle

    /** Called before STARTUP scripts load: forget everything a previous reload left behind. */
    public static synchronized void resetStartup() {
        MACHINES.clear();
        STRUCTURES.clear();
        LEVEL_TYPES.clear();
        LEVELS.clear();
        MODIFIERS.clear();
        MODIFIER_ITEMS.clear();
        RECIPES.clear();
        SCREEN_TEXT.clear();
        POOL_WORKSTATIONS.clear();
        MACHINE_WORKSTATIONS.clear();
        RECIPE_INFOS.clear();
        startupPosted = false;
        serverPosted = false;
        definitionsServed = false;
    }

    /**
     * Called before SERVER scripts load. Only recipe declarations are scoped to a server
     * reload — machine/structure/world definitions can never change after startup.
     */
    public static synchronized void beginServer() {
        RECIPES.clear();
        serverPosted = false;
    }

    public static synchronized void markStartupPosted() { startupPosted = true; }
    public static synchronized void markServerPosted() { serverPosted = true; }
    public static synchronized void markDefinitionsServed() { definitionsServed = true; }

    public static boolean isStartupPosted() { return startupPosted; }
    public static boolean isServerPosted() { return serverPosted; }
    public static boolean isDefinitionsServed() { return definitionsServed; }

    // ------------------------------------------------------------------ writers

    public static synchronized void putMachine(String id, MachineSpec spec) { MACHINES.put(id, spec); }
    public static synchronized void putStructure(String id, StructureSpec spec) { STRUCTURES.put(id, spec); }
    public static synchronized void putLevelType(String id, LevelTypeSpec spec) { LEVEL_TYPES.put(id, spec); }
    public static synchronized void putLevel(String id, MachineLevelSpec spec) { LEVELS.put(id, spec); }
    public static synchronized void registerModifier(String id, ModifierBundle bundle) { MODIFIERS.put(id, bundle); }
    public static synchronized void registerModifierItem(String itemId, String modifierId) {
        MODIFIER_ITEMS.put(itemId, modifierId);
    }
    public static synchronized void putRecipe(String id, RecipeSpec spec) { RECIPES.put(id, spec); }

    public static synchronized void registerControllerScreenText(String id, Consumer<ControllerTextContext> handler) {
        SCREEN_TEXT.put(id, handler);
    }
    public static synchronized void addRecipePoolWorkstation(String pool, String item) {
        POOL_WORKSTATIONS.add(new String[]{pool, item});
    }
    public static synchronized void addMachineWorkstation(String machine, String recipeType) {
        MACHINE_WORKSTATIONS.add(new String[]{machine, recipeType});
    }
    public static synchronized void addRecipeInfo(boolean pool, String id, String key, Object... args) {
        RECIPE_INFOS.add(new RecipeInfo(pool, id, key, args == null ? new Object[0] : args));
    }

    // ------------------------------------------------------------------ readers

    public static synchronized Map<String, MachineSpec> machines() { return new LinkedHashMap<>(MACHINES); }
    public static synchronized Map<String, StructureSpec> structures() { return new LinkedHashMap<>(STRUCTURES); }
    public static synchronized Map<String, LevelTypeSpec> levelTypes() { return new LinkedHashMap<>(LEVEL_TYPES); }
    public static synchronized Map<String, MachineLevelSpec> levels() { return new LinkedHashMap<>(LEVELS); }
    public static synchronized Map<String, ModifierBundle> modifiers() { return new LinkedHashMap<>(MODIFIERS); }
    public static synchronized Map<String, String> modifierItems() { return new LinkedHashMap<>(MODIFIER_ITEMS); }
    public static synchronized Map<String, RecipeSpec> recipes() { return new LinkedHashMap<>(RECIPES); }

    public static synchronized Map<String, Consumer<ControllerTextContext>> screenText() {
        return new LinkedHashMap<>(SCREEN_TEXT);
    }
    public static synchronized List<String[]> poolWorkstations() { return new ArrayList<>(POOL_WORKSTATIONS); }
    public static synchronized List<String[]> machineWorkstations() { return new ArrayList<>(MACHINE_WORKSTATIONS); }
    public static synchronized List<RecipeInfo> recipeInfos() { return new ArrayList<>(RECIPE_INFOS); }
}
