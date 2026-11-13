package cn.howxu.mmcr.nekojs.recipe;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import java.util.Map;

/**
 * Backs {@code event.recipes.mmcr.machine_recipe(pool, tickTime, requirements)} from NekoJS
 * server scripts.
 *
 * <p>Recipes produced here are plain {@code mmcr:machine_recipe} JSON, handed back to NekoJS's
 * recipe pipeline. MMCR reads that directory itself, so these survive {@code /nekojs reload} —
 * unlike the startup-only {@link cn.howxu.mmcr.nekojs.builder.RecipeBuilderJS}.
 *
 * <p>The wrapper deliberately does not depend on the {@code RecipeEventJS} type: NekoJS has one
 * shape per script generation, so the factory in
 * {@link cn.howxu.mmcr.nekojs.MMCRNekoJSPlugin} passes the event through {@code Object}.
 */
public final class MachineRecipeHandler {

    /** Typed constructor mirroring MMCR's KubeJS builder signature. */
    public JsonObject machine_recipe(Object recipePool, Object tickTime, Object requirements) {
        JsonObject json = new JsonObject();
        json.addProperty("type", "mmcr:machine_recipe");
        json.addProperty("recipe_pool", String.valueOf(recipePool));
        json.addProperty("tick_time", toInt(tickTime, 20));
        json.add("requirements", toArray(requirements));
        return json;
    }

    /** Convenience overload: {@code machine_recipe(pool, requirements)} with the default 20 ticks. */
    public JsonObject machine_recipe(Object recipePool, Object requirements) {
        return machine_recipe(recipePool, 20, requirements);
    }

    /** Full form taking a single JSON object; unknown keys pass straight through to MMCR. */
    public JsonObject machine_recipe(JsonObject json) {
        JsonObject copy = json.deepCopy();
        copy.addProperty("type", "mmcr:machine_recipe");
        return copy;
    }

    private static int toInt(Object value, int fallback) {
        if (value instanceof Number number) return number.intValue();
        if (value instanceof String text) {
            try {
                return Integer.parseInt(text.trim());
            } catch (NumberFormatException ignored) {
                return fallback;
            }
        }
        return fallback;
    }

    static JsonArray toArray(Object value) {
        JsonArray array = new JsonArray();
        if (value == null) return array;
        if (value instanceof Iterable<?> iterable) {
            for (Object item : iterable) addElement(array, item);
            return array;
        }
        if (value instanceof Object[] objects) {
            for (Object item : objects) addElement(array, item);
            return array;
        }
        addElement(array, value);
        return array;
    }

    private static void addElement(JsonArray array, Object item) {
        if (item instanceof JsonElement element) {
            array.add(element);
        } else if (item instanceof Number number) {
            array.add(number.longValue());
        } else if (item instanceof Boolean bool) {
            array.add(bool);
        } else if (item instanceof CharSequence text) {
            array.add(text.toString());
        } else if (item instanceof Map<?, ?> map) {
            JsonObject object = new JsonObject();
            map.forEach((k, v) -> object.add(String.valueOf(k), toElement(v)));
            array.add(object);
        } else if (item != null) {
            array.add(String.valueOf(item));
        }
    }

    private static JsonElement toElement(Object value) {
        if (value instanceof JsonElement element) return element;
        if (value instanceof Number number) return new JsonPrimitive(number);
        if (value instanceof Boolean bool) return new JsonPrimitive(bool);
        if (value instanceof Map<?, ?> map) {
            JsonObject object = new JsonObject();
            map.forEach((k, v) -> object.add(String.valueOf(k), toElement(v)));
            return object;
        }
        if (value instanceof Iterable<?> iterable) {
            JsonArray array = new JsonArray();
            for (Object item : iterable) array.add(toElement(item));
            return array;
        }
        return new JsonPrimitive(String.valueOf(value));
    }
}
