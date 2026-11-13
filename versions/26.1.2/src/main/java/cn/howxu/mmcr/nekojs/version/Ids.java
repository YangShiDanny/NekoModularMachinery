package cn.howxu.mmcr.nekojs.version;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

/** MC 26.1.2 mapping helpers. Shared code uses `var` so it compiles on both mappings. */
public final class Ids {
    private Ids() {}
    public static Identifier parse(String value) { return Identifier.parse(value); }
    public static Identifier[] parseAll(String... values) {
        Identifier[] out = new Identifier[values.length];
        for (int i = 0; i < values.length; i++) out[i] = parse(values[i]);
        return out;
    }
    public static TagKey<Block> blockTag(String value) { return TagKey.create(Registries.BLOCK, parse(value)); }
    public static String toString(Object id) { return String.valueOf(id); }
}
