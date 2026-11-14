package cn.howxu.mmcr.nekojs;

import cn.howxu.mmcr.nekojs.api.ConstantValues;
import cn.howxu.mmcr.nekojs.api.Refs;
import cn.howxu.mmcr.nekojs.version.Ids;
import cn.howxu.mmcr.publicapi.recipe.modifier.*;
import cn.howxu.mmcr.publicapi.structure.BlockCondition;
import cn.howxu.mmcr.publicapi.structure.BlockConditions;

import java.util.*;

/**
 * Script-facing facade returned by MMCR.getAPI() and event.getAPI().
 * Mirrors MMCR's KubeJSApi; returns MMCR Public API views plus plain maps for recipe JSON.
 */
public final class MMCRApiJS {

    // ---- constants ----
    public ConstantValues.ScreenScope screenScope() { return ConstantValues.SCREEN_SCOPE; }
    public ConstantValues.RecipeIO recipeIO() { return ConstantValues.RECIPE_IO; }
    public ConstantValues.OutputPolicy outputPolicy() { return ConstantValues.OUTPUT_POLICY; }
    public ConstantValues.ModifierOperation modifierOperation() { return ConstantValues.MODIFIER_OPERATION; }
    public ConstantValues.ModifierScope modifierScope() { return ConstantValues.MODIFIER_SCOPE; }

    // ---- ids ----
    public Object id(String value) { return Ids.parse(value); }

    // ---- block predicates ----
    public BlockCondition block(String blockId) { return BlockConditions.block(Ids.parse(blockId)); }
    public BlockCondition state(String stateId) { return BlockConditions.state(stateId); }
    public BlockCondition tag(String tagId) { return BlockConditions.tag(Ids.blockTag(tagId)); }
    public BlockCondition air() { return BlockConditions.state("minecraft:air"); }
    public BlockCondition any() { return BlockConditions.any(); }
    public BlockCondition coupler() { return BlockConditions.coupler(); }

    public BlockCondition anyOf(Object... values) {
        List<BlockCondition> list = new ArrayList<>();
        for (Object value : values) list.add(toCondition(value));
        return BlockConditions.any(list.toArray(new BlockCondition[0]));
    }

    public BlockCondition itemInput() { return BlockConditions.itemInput(); }
    public BlockCondition itemOutput() { return BlockConditions.itemOutput(); }
    public BlockCondition fluidInput() { return BlockConditions.fluidInput(); }
    public BlockCondition fluidOutput() { return BlockConditions.fluidOutput(); }
    public BlockCondition energyInput() { return BlockConditions.energyInput(); }
    public BlockCondition energyOutput() { return BlockConditions.energyOutput(); }
    public BlockCondition chemicalInput() { return BlockConditions.chemicalInput(); }
    public BlockCondition chemicalOutput() { return BlockConditions.chemicalOutput(); }
    public BlockCondition radioactiveChemicalInput() { return BlockConditions.radioactiveChemicalInput(); }
    public BlockCondition radioactiveChemicalOutput() { return BlockConditions.radioactiveChemicalOutput(); }
    public BlockCondition heatInput() { return BlockConditions.heatInput(); }
    public BlockCondition heatOutput() { return BlockConditions.heatOutput(); }
    public BlockCondition itemPorts() { return BlockConditions.itemPorts(); }
    public BlockCondition fluidPorts() { return BlockConditions.fluidPorts(); }
    public BlockCondition energyPorts() { return BlockConditions.energyPorts(); }
    public BlockCondition chemicalPorts() { return BlockConditions.chemicalPorts(); }
    public BlockCondition heatPorts() { return BlockConditions.heatPorts(); }
    public BlockCondition ports() { return BlockConditions.ports(); }
    public BlockCondition upgradeBus() { return BlockConditions.upgradeBus(); }
    public BlockCondition parallelControllers() { return BlockConditions.parallelControllers(); }
    public BlockCondition factoryController() { return BlockConditions.factoryController(); }
    public BlockCondition smartInterface() { return BlockConditions.smartInterface(); }
    public BlockCondition dataStorage() { return BlockConditions.dataStorage(); }
    public BlockCondition networkInterface() { return BlockConditions.networkInterface(); }

    /** Accepts a BlockCondition, a block id string, or a nested array of either. */
    public static BlockCondition toCondition(Object value) {
        if (value == null) return null;
        if (value instanceof BlockCondition condition) return condition;
        if (value instanceof String s) return BlockConditions.block(Ids.parse(s));
        if (value instanceof Object[] array) {
            List<BlockCondition> list = new ArrayList<>();
            for (Object item : array) list.add(toCondition(item));
            return BlockConditions.any(list.toArray(new BlockCondition[0]));
        }
        if (value instanceof Collection<?> collection) {
            List<BlockCondition> list = new ArrayList<>();
            for (Object item : collection) list.add(toCondition(item));
            return BlockConditions.any(list.toArray(new BlockCondition[0]));
        }
        throw new IllegalArgumentException("MMCR structure predicate must be a block condition, id string or array: " + value);
    }

    // ---- modifiers / levels ----
    public NumericModifierSpec modifier(String target, String scope, double value, String operation, boolean chance) {
        return Modifiers.numeric(target, enumValue(cn.howxu.mmcr.publicapi.recipe.modifier.ModifierScope.class, scope),
                value, enumValue(cn.howxu.mmcr.publicapi.recipe.modifier.ModifierOperation.class, operation), chance);
    }
    public ParallelizationModifierSpec modifier(String target, String scope, boolean value) {
        if (!"parallelized".equals(target) || !"recipe".equals(scope))
            throw new IllegalArgumentException("Boolean modifier must be modifier('parallelized','recipe',bool)");
        return Modifiers.parallelized(value);
    }
    public ModifierBundle modifierDefinition(Object... specs) { return bundle(specs); }
    private static ModifierBundle bundle(Object[] specs) {
        List<ModifierSpec> list = new ArrayList<>();
        for (Object spec : specs) {
            if (spec instanceof ModifierSpec ms) list.add(ms);
            else throw new IllegalArgumentException("Not a modifier spec: " + spec);
        }
        return Modifiers.bundle(list);
    }
    private static <E extends Enum<E>> E enumValue(Class<E> type, String name) {
        return Enum.valueOf(type, name.toUpperCase(Locale.ROOT));
    }

    public Refs.LevelSlotRef levelSlot(String typeId) { return new Refs.LevelSlotRef(typeId); }
    public Refs.ModifierUseRef modifierUse(String modifierId, Object replacement) {
        return new Refs.ModifierUseRef(modifierId, toCondition(replacement));
    }

    // ---- recipe JSON requirement helpers (for ServerEvents.recipes event.custom) ----
    public Map<String, Object> itemInputRequirement(String itemId, long count) {
        return ConstantValues.req("minecraft:item", "input", "item", itemId, "count", count);
    }
    public Map<String, Object> itemOutputRequirement(String itemId, long count, double chance) {
        Map<String, Object> stack = new LinkedHashMap<>();
        stack.put("id", itemId); stack.put("count", count);
        return ConstantValues.req("minecraft:item", "output", "stack", stack, "chance", chance);
    }
    public Map<String, Object> fluidInputRequirement(String fluidId, long amount) {
        // MMCR parses this field with NeoForge's FluidIngredient codec, whose simple form is the
        // plain fluid id (HolderSetCodec accepts a bare id string); a nested {"fluid": id} object
        // is not a shape it understands.
        return ConstantValues.req("minecraft:fluid", "input", "fluid", fluidId, "amount", amount);
    }
    public Map<String, Object> fluidOutputRequirement(String fluidId, long amount, double chance) {
        Map<String, Object> stack = new LinkedHashMap<>();
        stack.put("id", fluidId); stack.put("amount", amount);
        return ConstantValues.req("minecraft:fluid", "output", "stack", stack, "chance", chance);
    }
    public Map<String, Object> energyRequirement(String io, long fePerTick) {
        return ConstantValues.req("neoforge:energy", io, "fe_per_tick", fePerTick);
    }
    public Map<String, Object> machineRecipe(String recipePool, long tickTime, Object... requirements) {
        Map<String, Object> json = new LinkedHashMap<>();
        json.put("type", "mmcr:machine_recipe");
        json.put("recipe_pool", recipePool);
        json.put("tick_time", tickTime);
        json.put("requirements", Arrays.asList(requirements));
        return json;
    }

    // ---- number formatting (self-contained, no MC dependency) ----
    public String readableNumber(long value) { return compact(value, false); }
    public String readableNumberFull(long value) { return compact(value, true); }
    public String readableNumberExact(long value) { return String.format(Locale.ROOT, "%,d", value); }
    private static String compact(long value, boolean keepZeros) {
        if (value < 0) throw new IllegalArgumentException("value must be >= 0");
        String[] units = {"", "k", "M", "G", "T", "P", "E", "Z", "Y"};
        double v = value;
        int i = 0;
        while (v >= 1000 && i < units.length - 1) { v /= 1000; i++; }
        if (i == 0) return Long.toString(value);
        String text = String.format(Locale.ROOT, "%.2f", v);
        if (!keepZeros) { while (text.endsWith("0")) text = text.substring(0, text.length() - 1);
                          if (text.endsWith(".")) text = text.substring(0, text.length() - 1); }
        return text + units[i];
    }
}
