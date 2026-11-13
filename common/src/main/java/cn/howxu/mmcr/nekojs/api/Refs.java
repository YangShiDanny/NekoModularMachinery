package cn.howxu.mmcr.nekojs.api;

/** Opaque references produced by MMCRApiJS and consumed by the builders. */
public final class Refs {
    private Refs() {}

    public static final class LevelSlotRef {
        public final String typeId;
        public LevelSlotRef(String typeId) { this.typeId = typeId; }
    }
    public static final class ModifierUseRef {
        public final String modifierId;
        public final cn.howxu.mmcr.publicapi.structure.BlockCondition replacement;
        public ModifierUseRef(String modifierId, cn.howxu.mmcr.publicapi.structure.BlockCondition replacement) {
            this.modifierId = modifierId; this.replacement = replacement;
        }
    }
}
