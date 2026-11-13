package cn.howxu.mmcr.nekojs.version;

import cn.howxu.mmcr.publicapi.recipe.RecipeDraft;
import cn.howxu.mmcr.publicapi.structure.PortTierLimits;

import java.util.Locale;
import java.util.Set;

/**
 * MC 26.1.2 implementation.
 *
 * <p>MMCR 26.1.2 ships six port families only (item, fluid, energy, chemical, radioactive
 * chemical, heat) and its {@link RecipeDraft} no longer carries Create stress, Ars Nouveau
 * source, Botania mana or PneumaticCraft air I/O. Those families are reported as unsupported so
 * the shared builder logs and skips them instead of failing to compile.
 */
public final class VersionSupportImpl implements VersionSupport {

    private static final Set<String> FAMILIES = Set.of(
            "item_input_bus", "item_output_bus",
            "fluid_input_hatch", "fluid_output_hatch",
            "energy_input_hatch", "energy_output_hatch",
            "chemical_input_hatch", "chemical_output_hatch",
            "radioactive_chemical_input_hatch", "radioactive_chemical_output_hatch",
            "heat_input_hatch", "heat_output_hatch");

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
                default -> { return anyTier(builder, portFamily); }
            }
        } catch (IllegalArgumentException e) {
            return false;
        }
        return true;
    }

    // Stress / source / mana / air keep the no-op defaults: this line has no such recipe I/O.
}
