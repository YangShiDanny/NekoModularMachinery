package cn.howxu.mmcr.nekojs.builder;

import cn.howxu.mmcr.nekojs.bridge.DeclarationStore;
import cn.howxu.mmcr.nekojs.version.Ids;
import cn.howxu.mmcr.publicapi.structure.level.Levels;
import net.minecraft.network.chat.Component;

/** Startup-time level type builder. */
public final class LevelTypeBuilderJS {
    private final String id;
    private String displayNameKey;
    public LevelTypeBuilderJS(String id) { this.id = id; }
    public LevelTypeBuilderJS displayNameKey(String key) { this.displayNameKey = key; return this; }
    public void register() {
        String key = displayNameKey != null ? displayNameKey : "level." + id.replace(':', '.');
        DeclarationStore.putLevelType(id, Levels.type(Ids.parse(id), Component.translatable(key)));
    }
}
