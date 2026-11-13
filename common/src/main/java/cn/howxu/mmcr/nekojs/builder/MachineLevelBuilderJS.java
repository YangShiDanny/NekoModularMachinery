package cn.howxu.mmcr.nekojs.builder;

import cn.howxu.mmcr.nekojs.bridge.DeclarationStore;
import cn.howxu.mmcr.nekojs.version.Ids;
import cn.howxu.mmcr.publicapi.recipe.modifier.ModifierBundle;
import cn.howxu.mmcr.publicapi.recipe.modifier.Modifiers;
import cn.howxu.mmcr.publicapi.structure.BlockConditions;
import cn.howxu.mmcr.publicapi.structure.level.Levels;

import java.util.List;

/**
 * Machine level builder. A level is one (type, block-state) pairing with a priority; MMCR picks
 * the highest-priority matching level for a formed machine.
 *
 * <p>Declare from {@code MMCREvents.startup} — the level registry freezes in
 * {@code FMLCommonSetupEvent}.
 */
public final class MachineLevelBuilderJS {
    private final String id;
    private String typeId;
    private String stateId;
    private int priority;
    private ModifierBundle modifier;

    public MachineLevelBuilderJS(String id) { this.id = id; }

    public MachineLevelBuilderJS type(String typeId) { this.typeId = typeId; return this; }
    public MachineLevelBuilderJS state(String stateId) { this.stateId = stateId; return this; }
    public MachineLevelBuilderJS priority(int value) { this.priority = value; return this; }
    public MachineLevelBuilderJS modifier(ModifierBundle bundle) { this.modifier = bundle; return this; }

    public void register() {
        if (typeId == null || stateId == null) {
            throw new IllegalStateException("MMCR level " + id + " requires type() and state()");
        }
        ModifierBundle bundle = modifier != null ? modifier : Modifiers.bundle(List.of());
        DeclarationStore.putLevel(id, Levels.level(Ids.parse(id), Ids.parse(typeId), priority,
                BlockConditions.state(stateId), bundle));
    }
}
