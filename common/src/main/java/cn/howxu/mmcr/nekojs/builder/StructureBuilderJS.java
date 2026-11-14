package cn.howxu.mmcr.nekojs.builder;

import cn.howxu.mmcr.nekojs.MMCRApiJS;
import cn.howxu.mmcr.nekojs.MMCRNekoJS;
import cn.howxu.mmcr.nekojs.api.Refs;
import cn.howxu.mmcr.nekojs.bridge.DeclarationStore;
import cn.howxu.mmcr.nekojs.version.Ids;
import cn.howxu.mmcr.nekojs.version.VersionSupport;
import cn.howxu.mmcr.publicapi.Structures;
import cn.howxu.mmcr.publicapi.structure.BlockCondition;
import cn.howxu.mmcr.publicapi.structure.BlockConditions;
import cn.howxu.mmcr.publicapi.structure.PortTierLimits;

import java.util.*;

/**
 * Structure builder. Each {@code pattern(...)} call appends one z-slice, matching MMCR's
 * KubeJS builder: {@code pattern('SSS', 'SMS', 'SSS')} is a single layer of three rows.
 *
 * <p>Must be built from {@code MMCREvents.startup}; MMCR collects structures during
 * {@code FMLCommonSetupEvent} and never reopens that window.
 */
public final class StructureBuilderJS {
    private final String id;
    private final List<String[]> slices = new ArrayList<>();
    private final Map<Character, Object> symbols = new LinkedHashMap<>();
    private final Map<String, Object> portLimits = new LinkedHashMap<>();
    private final List<Object[]> tierRequirements = new ArrayList<>();
    private char controllerSymbol = 'C';
    private boolean stateSensitive = true;

    public StructureBuilderJS(String id) {
        this.id = Objects.requireNonNull(id, "machine id");
    }

    /** One z-slice; every string is one row, all rows must share the same length. */
    public StructureBuilderJS pattern(String... rows) { slices.add(rows); return this; }
    public StructureBuilderJS pattern(List<String> rows) { return pattern(rows.toArray(new String[0])); }

    /** Binds a single-character symbol to a block/state/tag/port predicate or a level/modifier slot. */
    public StructureBuilderJS set(String symbol, Object value) {
        if (symbol == null || symbol.isEmpty()) {
            throw new IllegalArgumentException("symbol must be exactly one character");
        }
        symbols.put(symbol.charAt(0), value);
        return this;
    }

    public StructureBuilderJS controller(String symbol) {
        if (symbol == null || symbol.isEmpty()) {
            throw new IllegalArgumentException("controller symbol must be exactly one character");
        }
        this.controllerSymbol = symbol.charAt(0);
        return this;
    }

    public StructureBuilderJS stateSensitive() { this.stateSensitive = true; return this; }
    public StructureBuilderJS stateInsensitive() { this.stateSensitive = false; return this; }

    /** Port count constraints: {@code {item_input_bus: 1}} or {@code {item_input_bus: [1, 4]}}. */
    public StructureBuilderJS portRequirements(Map<String, Object> map) { portLimits.putAll(map); return this; }

    /**
     * Minimum tier requirements, either canonical strings ({@code "item_input_bus>=NORMAL"}) or
     * two-element arrays ({@code ['item_input_bus', 'NORMAL']}). A family without {@code >=}
     * means "any tier of this family".
     */
    public StructureBuilderJS portTierRequirements(Object... values) {
        for (Object value : values) {
            if (value instanceof Object[] array) {
                tierRequirements.add(array);
            } else if (value instanceof Collection<?> collection) {
                tierRequirements.add(collection.toArray());
            } else {
                tierRequirements.add(new Object[]{value});
            }
        }
        return this;
    }

    public void build() {
        if (slices.isEmpty()) {
            throw new IllegalStateException("MMCR structure " + id + " declared no pattern()");
        }

        // MMCR only substitutes the controller block when the controller symbol still carries the
        // "automatic controller" predicate (PatternDefinition.bindController). PatternBuilder
        // installs that predicate inside controller(symbol) *only if nothing was bound first*, so a
        // stray set('<controller>', block) silently turns the controller cell into a plain block:
        // the structure then requires a block that the controller never is and can never form.
        // Drop such a binding instead of shipping a dead structure.
        if (symbols.get(controllerSymbol) != null) {
            symbols.remove(controllerSymbol);
            MMCRNekoJS.LOG.warn("MMCR/NekoJS structure {}: symbol '{}' is the controller, so "
                            + "set('{}', ...) is ignored - remove it from the script. "
                            + "MMCR places the controller block in that cell by itself.",
                    id, controllerSymbol, controllerSymbol);
        }

        var draft = Structures.structure();
        if (stateSensitive) draft.stateSensitive(); else draft.stateInsensitive();

        Map<Character, Refs.LevelSlotRef> levelSlots = new LinkedHashMap<>();
        Map<Character, Refs.ModifierUseRef> modifierUses = new LinkedHashMap<>();
        symbols.forEach((symbol, value) -> {
            if (value instanceof Refs.LevelSlotRef ref) levelSlots.put(symbol, ref);
            else if (value instanceof Refs.ModifierUseRef ref) modifierUses.put(symbol, ref);
        });

        // A level/modifier slot is validated through stage.requirements, but MMCR still demands a
        // predicate for every symbol that shows up in the pattern (PatternDefinition throws
        // "Unbound pattern symbol" otherwise) - MMCR's own builtin structures bind those cells to
        // an anyOf(...) of the level blocks. Do the same, derived from the levels declared so far.
        // fullStructure already selects the FULL stage: never call stage.full() here.
        var spec = draft.fullStructure(stage -> {
            stage.pattern(pattern -> {
                for (String[] slice : slices) pattern.layer(slice);
                symbols.forEach((symbol, value) -> {
                    if (value instanceof Refs.LevelSlotRef ref) {
                        pattern.where(symbol, levelSlotPredicate(ref.typeId));
                        return;
                    }
                    if (value instanceof Refs.ModifierUseRef ref) {
                        pattern.where(symbol, modifierSlotPredicate(ref));
                        return;
                    }
                    pattern.where(symbol, MMCRApiJS.toCondition(value));
                });
                pattern.controller(controllerSymbol);
            });

            if (!portLimits.isEmpty()) {
                stage.ports(ports -> portLimits.forEach((family, value) -> {
                    if (value instanceof Number count) {
                        ports.min(family, count.intValue());
                    } else if (value instanceof Collection<?> range && range.size() == 2) {
                        ports.range(family, asInt(range, 0), asInt(range, 1));
                    } else if (value instanceof Object[] range && range.length == 2) {
                        ports.range(family, asInt(range, 0), asInt(range, 1));
                    }
                }));
            }

            if (!tierRequirements.isEmpty()) {
                stage.portTiers(tiers -> applyTiers(tiers, tierRequirements));
            }

            if (!levelSlots.isEmpty() || !modifierUses.isEmpty()) {
                stage.requirements(constraints -> {
                    levelSlots.forEach((symbol, ref) -> constraints.levelSlot(symbol, Ids.parse(ref.typeId)));
                    modifierUses.forEach((symbol, ref) -> {
                        if (ref.replacement == null) {
                            constraints.modifier(symbol, Ids.parse(ref.modifierId));
                        } else {
                            constraints.modifier(symbol, Ids.parse(ref.modifierId), ref.replacement);
                        }
                    });
                });
            }
        }).build(Ids.parse(id));

        DeclarationStore.putStructure(id, spec);
    }

    /**
     * Pattern predicate for a level slot: the union of every declared level of that type. MMCR
     * picks the concrete level from the block found in the cell, so the pattern only has to accept
     * the candidates. Levels declared *after* this structure are not covered - hence the explicit
     * error instead of silently producing a structure nothing can match.
     */
    private BlockCondition levelSlotPredicate(String typeId) {
        var type = Ids.parse(typeId);
        List<BlockCondition> states = new ArrayList<>();
        DeclarationStore.levels().values().stream()
                .filter(spec -> type.equals(spec.typeId()))
                .forEach(spec -> states.add(spec.statePredicate()));
        if (states.isEmpty()) {
            throw new IllegalArgumentException("MMCR structure " + id + " has a level slot of type '"
                    + typeId + "' but no level of that type was declared yet. Call event.createLevel(...)"
                    + " for that type before this structure, or bind the cell with set(symbol, ...).");
        }
        return BlockConditions.any(states.toArray(new BlockCondition[0]));
    }

    /**
     * A modifier slot needs a block to place, because the pattern has to know what may sit in that
     * cell. {@code event.modifierSlot(id, block)} supplies it; without one there is nothing sensible
     * to accept, so fail loudly instead of leaving the symbol unbound.
     */
    private BlockCondition modifierSlotPredicate(Refs.ModifierUseRef ref) {
        if (ref.replacement != null) return ref.replacement;
        throw new IllegalArgumentException("MMCR structure " + id + " has modifier slot '"
                + ref.modifierId + "' without a placement block. Use event.modifierSlot('"
                + ref.modifierId + "', api.block('...')).");
    }

    private static int asInt(Object container, int index) {
        Object value = container instanceof Collection<?> c
                ? new ArrayList<>(c).get(index)
                : ((Object[]) container)[index];
        return ((Number) value).intValue();
    }

    /**
     * Parses {@code "family>=TIER"} / {@code "family"} / {@code ["family", "TIER"]} entries and
     * delegates to {@link VersionSupport}, which knows which port families this MC line has.
     */
    private static void applyTiers(PortTierLimits.Builder builder, List<Object[]> requirements) {
        VersionSupport support = VersionSupport.get();
        for (Object[] entry : requirements) {
            String family;
            String tier = null;
            if (entry.length == 1 && entry[0] instanceof String text && text.contains(">=")) {
                // "family>=TIER" packed into one element
                String[] parts = text.split(">=", 2);
                family = parts[0].trim();
                tier = parts[1].trim().toUpperCase(Locale.ROOT);
            } else {
                family = String.valueOf(entry[0]).trim();
                if (entry.length > 1 && entry[1] != null) {
                    tier = String.valueOf(entry[1]).trim().toUpperCase(Locale.ROOT);
                }
            }

            if (!support.supports(family)) {
                MMCRNekoJS.LOG.warn("MMCR port family '{}' does not exist on this Minecraft version; skipped", family);
                continue;
            }
            boolean applied = tier == null
                    ? support.anyTier(builder, family)
                    : support.minTier(builder, family, tier);
            if (!applied) {
                MMCRNekoJS.LOG.warn("Invalid MMCR port tier requirement '{}' (tier '{}')", family, tier);
            }
        }
    }
}
