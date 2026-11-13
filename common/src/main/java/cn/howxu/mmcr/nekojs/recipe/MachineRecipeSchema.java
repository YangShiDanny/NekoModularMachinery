package cn.howxu.mmcr.nekojs.recipe;

import cn.howxu.mmcr.nekojs.MMCRNekoJS;
import com.google.gson.JsonArray;
import com.google.gson.JsonPrimitive;
import com.tkisor.nekojs.api.recipe.definition.RecipeFieldDefinition;
import com.tkisor.nekojs.api.recipe.definition.RecipeFieldKind;
import com.tkisor.nekojs.api.recipe.definition.RecipeFieldRole;
import com.tkisor.nekojs.api.recipe.definition.RecipeTypeDefinition;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Java-side definition of MMCR's {@code mmcr:machine_recipe} schema.
 *
 * <p>Registering it here (rather than only through a script pack) means
 * {@code event.recipes.mmcr.machine_recipe(pool, ticks, requirements)} works out of the box,
 * and {@code KubeJS-style} positional constructors get validated before they reach MMCR.
 */
public final class MachineRecipeSchema {
    private MachineRecipeSchema() {}

    public static final String NAMESPACE = "mmcr";
    public static final String NAME = "machine_recipe";

    public static RecipeTypeDefinition build() {
        Map<String, RecipeFieldDefinition> fields = new LinkedHashMap<>();

        fields.put("recipe_pool", field("recipe_pool", RecipeFieldKind.STRING, false, false, null, RecipeFieldRole.OTHER));
        fields.put("tick_time", field("tick_time", RecipeFieldKind.INT, false, true, new JsonPrimitive(20), RecipeFieldRole.OTHER));
        fields.put("requirements", field("requirements", RecipeFieldKind.JSON, true, false, null, RecipeFieldRole.INPUT));
        fields.put("outputs", field("outputs", RecipeFieldKind.JSON, true, true, null, RecipeFieldRole.OUTPUT));
        fields.put("modifiers", field("modifiers", RecipeFieldKind.JSON, true, true, null, RecipeFieldRole.OTHER));
        fields.put("priority", field("priority", RecipeFieldKind.INT, false, true, new JsonPrimitive(0), RecipeFieldRole.OTHER));
        fields.put("max_threads", field("max_threads", RecipeFieldKind.INT, false, true, new JsonPrimitive(1), RecipeFieldRole.OTHER));
        fields.put("parallelized", field("parallelized", RecipeFieldKind.BOOLEAN, false, true, new JsonPrimitive(false), RecipeFieldRole.OTHER));
        fields.put("allow_partial_outputs", field("allow_partial_outputs", RecipeFieldKind.BOOLEAN, false, true, new JsonPrimitive(false), RecipeFieldRole.OTHER));
        fields.put("cancelIfPerTickFails", field("cancelIfPerTickFails", RecipeFieldKind.BOOLEAN, false, true, new JsonPrimitive(false), RecipeFieldRole.OTHER));
        fields.put("required_host_ids", field("required_host_ids", RecipeFieldKind.JSON, true, true, new JsonArray(), RecipeFieldRole.OTHER));

        return new RecipeTypeDefinition(
                NAMESPACE,
                NAME,
                "mmcr:machine_recipe",
                "mmcr_machine_recipe",
                List.of(List.of("recipe_pool", "tick_time", "requirements")),
                fields,
                List.of(),
                Map.of(),
                Map.of(),
                Map.of(),
                null);
    }

    private static RecipeFieldDefinition field(String name, RecipeFieldKind kind, boolean array,
                                               boolean optional, com.google.gson.JsonElement defaultValue,
                                               RecipeFieldRole role) {
        return new RecipeFieldDefinition(name, name, kind, array, optional, defaultValue, role);
    }

    /** Logs the schema contents once; called from plugin registration for discoverability. */
    public static void logRegistration() {
        MMCRNekoJS.LOG.debug("Registered NekoJS recipe schema {}:{}", NAMESPACE, NAME);
    }
}
