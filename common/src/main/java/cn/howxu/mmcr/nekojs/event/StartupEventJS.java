package cn.howxu.mmcr.nekojs.event;

import cn.howxu.mmcr.nekojs.MMCRApiJS;
import cn.howxu.mmcr.nekojs.api.Refs;
import cn.howxu.mmcr.nekojs.bridge.DeclarationStore;
import cn.howxu.mmcr.nekojs.builder.*;
import cn.howxu.mmcr.publicapi.recipe.modifier.ModifierBundle;

/**
 * Event object received by {@code MMCREvents.startup} callbacks.
 *
 * <p>This is the only window where declarations are accepted. NekoJS builds its STARTUP script
 * context inside {@code NekoJSMod}'s constructor, i.e. before {@code FMLConstructModEvent}, and
 * MMCR commits machines there, structures in {@code FMLCommonSetupEvent} and recipes in
 * {@code ModifyDefaultComponentsEvent}. All three are therefore downstream of this event —
 * which is also why structures and recipes cannot live in {@code server_scripts}.
 */
public final class StartupEventJS {
    private final MMCRApiJS api = new MMCRApiJS();

    public MMCRApiJS getAPI() { return api; }

    // ---- machines ----
    public MachineBuilderJS createMachine(String id) { return new MachineBuilderJS(id); }

    // ---- structures (startup only — see class javadoc) ----
    public StructureBuilderJS createStructure(String id) { return new StructureBuilderJS(id); }

    // ---- levels ----
    public LevelTypeBuilderJS createLevelType(String id) { return new LevelTypeBuilderJS(id); }
    public MachineLevelBuilderJS createLevel(String id) { return new MachineLevelBuilderJS(id); }

    /** Symbol value for a level slot inside a structure pattern, e.g. {@code set('L', levelSlot('my:voltage'))}. */
    public Refs.LevelSlotRef levelSlot(String typeId) { return new Refs.LevelSlotRef(typeId); }

    /** Symbol value for a modifier slot, optionally replacing it with another block. */
    public Refs.ModifierUseRef modifierSlot(String modifierId) {
        return new Refs.ModifierUseRef(modifierId, null);
    }

    public Refs.ModifierUseRef modifierSlot(String modifierId, Object replacement) {
        return new Refs.ModifierUseRef(modifierId, MMCRApiJS.toCondition(replacement));
    }

    // ---- modifiers ----
    public void registerModifier(String id, ModifierBundle definition) {
        DeclarationStore.registerModifier(id, definition);
    }

    public void registerModifier(String id, Object[] modifiers) {
        registerModifier(id, api.modifierDefinition(modifiers));
    }

    public void registerModifierItem(String itemId, String modifierId) {
        DeclarationStore.registerModifierItem(itemId, modifierId);
    }

    // ---- JEI workstations ----
    public void addRecipePoolWorkstation(String recipePoolId, String itemId) {
        DeclarationStore.addRecipePoolWorkstation(recipePoolId, itemId);
    }

    public void addMachineWorkstation(String machineId, String recipeTypeId) {
        DeclarationStore.addMachineWorkstation(machineId, recipeTypeId);
    }

    /** Compatibility alias used by older MMCR/KubeJS scripts. */
    public void addMachineWorkStation(String machineId, String recipeTypeId) {
        addMachineWorkstation(machineId, recipeTypeId);
    }

    /** Registers a controller screen text handler; the callback receives MMCR's text context. */
    public void registerControllerScreenText(String machineId,
                                             java.util.function.Consumer<Object> handler) {
        DeclarationStore.registerControllerScreenText(machineId,
                ctx -> handler.accept(ctx));
    }
}
