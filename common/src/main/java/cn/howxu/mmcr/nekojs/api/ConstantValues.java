package cn.howxu.mmcr.nekojs.api;

import java.util.Map;

/** Small POJOs so constant objects are readable and probe-friendly from JS. */
public final class ConstantValues {
    private ConstantValues() {}

    public static final class ScreenScope {
        public final String CONTROLLER = "controller";
        public final String OPERATION = "operation";
    }
    public static final class RecipeIO {
        public final String INPUT = "input";
        public final String OUTPUT = "output";
    }
    public static final class OutputPolicy {
        public final String REQUIRE_FULL = "require_full";
        public final String ALLOW_PARTIAL = "allow_partial";
    }
    public static final class ModifierOperation {
        public final String ADD = "add";
        public final String MULTIPLY = "multiply";
        public final String SUBTRACT = "subtract";
        public final String DIVIDE = "divide";
    }
    public static final class ModifierScope {
        public final String INPUT = "input";
        public final String OUTPUT = "output";
        public final String MACHINE = "machine";
        public final String RECIPE = "recipe";
    }

    public static final ScreenScope SCREEN_SCOPE = new ScreenScope();
    public static final RecipeIO RECIPE_IO = new RecipeIO();
    public static final OutputPolicy OUTPUT_POLICY = new OutputPolicy();
    public static final ModifierOperation MODIFIER_OPERATION = new ModifierOperation();
    public static final ModifierScope MODIFIER_SCOPE = new ModifierScope();

    /** Requirement helpers return plain maps; they drop straight into event.custom({...}). */
    public static Map<String, Object> req(String type, String io, Object... kv) {
        java.util.LinkedHashMap<String, Object> m = new java.util.LinkedHashMap<>();
        m.put("type", type);
        m.put("io", io);
        for (int i = 0; i + 1 < kv.length; i += 2) m.put(String.valueOf(kv[i]), kv[i + 1]);
        return m;
    }
}
