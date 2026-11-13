package cn.howxu.mmcr.nekojs.version;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

/** MC 1.21.1 mapping helpers. Shared code uses `var` so it compiles on both mappings. */
public final class Ids {
    private Ids() {}
    public static ResourceLocation parse(String value) { return ResourceLocation.parse(value); }
    public static ResourceLocation[] parseAll(String... values) {
        ResourceLocation[] out = new ResourceLocation[values.length];
        for (int i = 0; i < values.length; i++) out[i] = parse(values[i]);
        return out;
    }
    public static TagKey<Block> blockTag(String value) { return TagKey.create(Registries.BLOCK, parse(value)); }
    public static String toString(Object id) { return String.valueOf(id); }
}
