package cn.howxu.mmcr.nekojs.version;

import cn.howxu.mmcr.publicapi.structure.PortTierLimits;
import cn.howxu.mmcr.publicapi.recipe.RecipeDraft;

/**
 * Version-specific MMCR port families.
 *
 * <p>MMCR 1.21.1 exposes ten port families (item, fluid, energy, chemical, radioactive chemical,
 * heat, plus Create stress / Ars source / Botania mana / PneumaticCraft air). The 26.1.2 line
 * trimmed the builder to the six families it still ships, so those four families must be
 * referenced from a per-version implementation instead of the shared source set.
 *
 * <p>Every implementation returns {@code false} for a family it does not support; the shared
 * builder then logs a warning and skips the requirement.
 *
 * @see cn.howxu.mmcr.nekojs.builder.StructureBuilderJS
 */
public interface VersionSupport {
    /** Installed by each version's own subclass; resolved once through {@link #get()}. */
    VersionSupport INSTANCE = create();

    private static VersionSupport create() {
        try {
            return (VersionSupport) Class.forName("cn.howxu.mmcr.nekojs.version.VersionSupportImpl")
                    .getDeclaredConstructor().newInstance();
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("MMCR/NekoJS version support class is missing", e);
        }
    }

    static VersionSupport get() { return INSTANCE; }

    /** {@code true} when this MC line has the family at all. */
    boolean supports(String portFamily);

    /**
     * Applies an "any tier" requirement for {@code portFamily} to {@code builder}.
     *
     * @return {@code false} when the family is unavailable on this MC line
     */
    boolean anyTier(PortTierLimits.Builder builder, String portFamily);

    /**
     * Applies a "minimum tier" requirement. Falls back to {@link #anyTier} when the family has no
     * tiered enum but still exists.
     */
    boolean minTier(PortTierLimits.Builder builder, String portFamily, String tier);

    // ---- recipe draft capabilities that differ between lines ----

    default RecipeDraft inputStress(RecipeDraft draft, double stress, double minRpm) { return draft; }
    default RecipeDraft outputStress(RecipeDraft draft, double stress, double rpm) { return draft; }
    default RecipeDraft inputSource(RecipeDraft draft, long amount) { return draft; }
    default RecipeDraft outputSource(RecipeDraft draft, long amount) { return draft; }
    default RecipeDraft inputMana(RecipeDraft draft, long amount) { return draft; }
    default RecipeDraft outputMana(RecipeDraft draft, long amount) { return draft; }
    default RecipeDraft inputAir(RecipeDraft draft, long airPerTick, float minPressure) { return draft; }
    default RecipeDraft outputAir(RecipeDraft draft, long airPerTick) { return draft; }
}
