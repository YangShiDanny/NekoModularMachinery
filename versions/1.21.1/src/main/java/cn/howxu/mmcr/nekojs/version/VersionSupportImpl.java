package cn.howxu.mmcr.nekojs.version;

import cn.howxu.mmcr.publicapi.recipe.RecipeDraft;
import cn.howxu.mmcr.publicapi.structure.PortTierLimits;

import java.util.Locale;
import java.util.Set;

/**
 * MC 1.21.1 implementation. Backed by MMCR 1.21.1, which exposes all ten port families
 * (including Create stress, Ars Nouveau source, Botania mana and PneumaticCraft air).
 */
public final class VersionSupportImpl implements VersionSupport {

    private static final Set<String> FAMILIES = Set.of(
            "item_input_bus", "item_output_bus",
            "fluid_input_hatch", "fluid_output_hatch",
            "energy_input_hatch", "energy_output_hatch",
            "chemical_input_hatch", "chemical_output_hatch",
            "radioactive_chemical_input_hatch", "radioactive_chemical_output_hatch",
            "heat_input_hatch", "heat_output_hatch",
            "stress_input_hatch", "stress_output_hatch",
            "source_input_hatch", "source_output_hatch",
            "mana_input_hatch", "mana_output_hatch",
            "air_input_hatch", "air_output_hatch");

    @Override
    public boolean supports(String portFamily) { return FAMILIES.contains(portFamily); }

    @Override
    public boolean anyTier(PortTierLimits.Builder builder, String portFamily) {
        switch (portFamily) {
            case "item_input_bus" -> builder.anyItemInput();
            case "item_output_bus" -> builder.anyItemOutput();
            case "fluid_input_hatch" -> builder.anyFluidInput();
            case "fluid_output_hatch" -> builder.anyFluidOutput();
            case "energy_input_hatch" -> builder.anyEnergyInput();
            case "energy_output_hatch" -> builder.anyEnergyOutput();
            case "chemical_input_hatch" -> builder.anyChemicalInput();
            case "chemical_output_hatch" -> builder.anyChemicalOutput();
            case "radioactive_chemical_input_hatch" -> builder.anyRadioactiveChemicalInput();
            case "radioactive_chemical_output_hatch" -> builder.anyRadioactiveChemicalOutput();
            case "heat_input_hatch" -> builder.anyHeatInput();
            case "heat_output_hatch" -> builder.anyHeatOutput();
            case "stress_input_hatch" -> builder.anyStressInput();
            case "stress_output_hatch" -> builder.anyStressOutput();
            case "source_input_hatch" -> builder.anySourceInput();
            case "source_output_hatch" -> builder.anySourceOutput();
            case "mana_input_hatch" -> builder.anyManaInput();
            case "mana_output_hatch" -> builder.anyManaOutput();
            case "air_input_hatch" -> builder.anyAirInput();
            case "air_output_hatch" -> builder.anyAirOutput();
            default -> { return false; }
        }
        return true;
    }

    @Override
    public boolean minTier(PortTierLimits.Builder builder, String portFamily, String tier) {
        String t = tier.toUpperCase(Locale.ROOT);
        try {
            switch (portFamily) {
                case "item_input_bus" -> builder.minItemInput(PortTierLimits.ItemTier.valueOf(t));
                case "item_output_bus" -> builder.minItemOutput(PortTierLimits.ItemTier.valueOf(t));
                case "fluid_input_hatch" -> builder.minFluidInput(PortTierLimits.FluidTier.valueOf(t));
                case "fluid_output_hatch" -> builder.minFluidOutput(PortTierLimits.FluidTier.valueOf(t));
                case "energy_input_hatch" -> builder.minEnergyInput(PortTierLimits.EnergyTier.valueOf(t));
                case "energy_output_hatch" -> builder.minEnergyOutput(PortTierLimits.EnergyTier.valueOf(t));
                case "chemical_input_hatch" -> builder.minChemicalInput(PortTierLimits.ChemicalTier.valueOf(t));
                case "chemical_output_hatch" -> builder.minChemicalOutput(PortTierLimits.ChemicalTier.valueOf(t));
                // Untiered families: any tier is the finest granularity MMCR offers.
                default -> { return anyTier(builder, portFamily); }
            }
        } catch (IllegalArgumentException e) {
            return false;
        }
        return true;
    }

    // ---- recipe capabilities present on 1.21.1 ----
    @Override public RecipeDraft inputStress(RecipeDraft draft, double stress, double minRpm) { return draft.inputStress(stress, minRpm); }
    @Override public RecipeDraft outputStress(RecipeDraft draft, double stress, double rpm) { return draft.outputStress(stress, rpm); }
    @Override public RecipeDraft inputSource(RecipeDraft draft, long amount) { return draft.inputSource(amount); }
    @Override public RecipeDraft outputSource(RecipeDraft draft, long amount) { return draft.outputSource(amount); }
    @Override public RecipeDraft inputMana(RecipeDraft draft, long amount) { return draft.inputMana(amount); }
    @Override public RecipeDraft outputMana(RecipeDraft draft, long amount) { return draft.outputMana(amount); }
    @Override public RecipeDraft inputAir(RecipeDraft draft, long airPerTick, float minPressure) { return draft.inputAir(airPerTick, minPressure); }
    @Override public RecipeDraft outputAir(RecipeDraft draft, long airPerTick) { return draft.outputAir(airPerTick); }
}
