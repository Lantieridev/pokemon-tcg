package ar.edu.utn.frc.tup.piii.engine.pipeline;

import ar.edu.utn.frc.tup.piii.engine.model.StatusEffectType;

import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

/**
 * Parses an attack's effect-text descriptor and dispatches to the appropriate handler
 * using a pre-built {@code Map<AttackEffectType, BiConsumer>} — no switch, no instanceof.
 *
 * <p>Effect-text format: {@code "keyword"} or {@code "keyword:amount"}, e.g.:
 * <ul>
 *   <li>{@code ""} / {@code "none"} — no secondary effect</li>
 *   <li>{@code "poison"} — apply ENVENENADO to the defender</li>
 *   <li>{@code "burn"} — apply QUEMADO to the defender</li>
 *   <li>{@code "paralysis"} — apply PARALIZADO to the defender</li>
 *   <li>{@code "sleep"} — apply DORMIDO to the defender</li>
 *   <li>{@code "confusion"} — apply CONFUNDIDO to the defender</li>
 *   <li>{@code "heal:30"} — heal the attacker 30 HP (3 counters)</li>
 *   <li>{@code "self_damage:10"} — place 1 counter on the attacker</li>
 *   <li>{@code "discard_energy:1"} — discard 1 attached energy from the attacker</li>
 *   <li>{@code "coin_flip_extra:20"} — no-op here; handled earlier by PreDamageEffectsStep</li>
 * </ul>
 * </p>
 */
public final class AttackEffectResolver {

    private static final int DAMAGE_PER_COUNTER = 10;

    private static final Map<String, AttackEffectType> TEXT_TO_TYPE;

    static {
        final Map<String, AttackEffectType> m = new HashMap<>();
        m.put("poison",           AttackEffectType.APPLY_POISON);
        m.put("burn",             AttackEffectType.APPLY_BURN);
        m.put("paralysis",        AttackEffectType.APPLY_PARALYSIS);
        m.put("sleep",            AttackEffectType.APPLY_SLEEP);
        m.put("confusion",        AttackEffectType.APPLY_CONFUSION);
        m.put("heal",             AttackEffectType.HEAL_SELF);
        m.put("self_damage",      AttackEffectType.SELF_DAMAGE);
        m.put("discard_energy",   AttackEffectType.DISCARD_ENERGY);
        m.put("coin_flip_extra",  AttackEffectType.COIN_FLIP_EXTRA_DAMAGE);
        m.put("bench_damage",     AttackEffectType.BENCH_DAMAGE);
        m.put("move_energy",      AttackEffectType.MOVE_ENERGY);
        m.put("force_switch",     AttackEffectType.FORCE_SWITCH);
        TEXT_TO_TYPE = Collections.unmodifiableMap(m);
    }

    private final Map<AttackEffectType, BiConsumer<Integer, AttackContext>> handlers;

    /**
     * Constructs the resolver with all effect handlers pre-registered.
     */
    public AttackEffectResolver() {
        final Map<AttackEffectType, BiConsumer<Integer, AttackContext>> m =
                new EnumMap<>(AttackEffectType.class);
        m.put(AttackEffectType.NONE,
                (amount, ctx) -> { });
        m.put(AttackEffectType.COIN_FLIP_EXTRA_DAMAGE,
                (amount, ctx) -> { }); // handled in PreDamageEffectsStep
        m.put(AttackEffectType.APPLY_POISON,
                (amount, ctx) -> ctx.getDefenderStatusManager().apply(StatusEffectType.ENVENENADO));
        m.put(AttackEffectType.APPLY_BURN,
                (amount, ctx) -> ctx.getDefenderStatusManager().apply(StatusEffectType.QUEMADO));
        m.put(AttackEffectType.APPLY_PARALYSIS,
                (amount, ctx) -> ctx.getDefenderStatusManager().apply(StatusEffectType.PARALIZADO));
        m.put(AttackEffectType.APPLY_SLEEP,
                (amount, ctx) -> ctx.getDefenderStatusManager().apply(StatusEffectType.DORMIDO));
        m.put(AttackEffectType.APPLY_CONFUSION,
                (amount, ctx) -> ctx.getDefenderStatusManager().apply(StatusEffectType.CONFUNDIDO));
        m.put(AttackEffectType.HEAL_SELF,
                (amount, ctx) -> ctx.getAttacker().heal(amount));
        m.put(AttackEffectType.SELF_DAMAGE,
                (amount, ctx) -> ctx.getAttacker().addDamageCounters(amount / DAMAGE_PER_COUNTER));
        m.put(AttackEffectType.DISCARD_ENERGY,
                (amount, ctx) -> ctx.getAttacker().removeEnergies(amount));
        m.put(AttackEffectType.BENCH_DAMAGE,
                (amount, ctx) -> {
                    // Bench damage: apply N damage to each of the opponent's Benched Pokémon.
                    // Weakness and Resistance do not apply to Benched Pokémon (XY1 §3).
                    final int counters = amount / DAMAGE_PER_COUNTER;
                    ctx.getDefenderBench().forEach(benched -> benched.addDamageCounters(counters));
                });
        // FR-TODO: move_energy requires attacker bench runtime access — deferred.
        m.put(AttackEffectType.MOVE_ENERGY,
                (amount, ctx) -> { });
        // FR-TODO: force_switch requires opponent runtime access — deferred.
        m.put(AttackEffectType.FORCE_SWITCH,
                (amount, ctx) -> { });
        this.handlers = Collections.unmodifiableMap(m);
    }

    /**
     * Resolves the effect descriptor and executes the appropriate handler against {@code ctx}.
     *
     * @param ctx the current attack context (never null)
     */
    public void apply(final AttackContext ctx) {
        final String effectText = ctx.getEffectText();
        final AttackEffectType type = resolveType(effectText);
        final int amount = extractAmount(effectText);
        final BiConsumer<Integer, AttackContext> handler = handlers.get(type);
        if (handler != null) {
            handler.accept(amount, ctx);
        }
    }

    /**
     * Maps an effect-text descriptor to its {@link AttackEffectType}.
     * Package-private for direct test access.
     *
     * @param effectText the raw descriptor (may be null or blank)
     * @return resolved type, or {@link AttackEffectType#NONE} for unknown/blank input
     */
    AttackEffectType resolveType(final String effectText) {
        if (effectText == null || effectText.isBlank()) {
            return AttackEffectType.NONE;
        }
        final String key = effectText.contains(":")
                ? effectText.substring(0, effectText.indexOf(':'))
                : effectText;
        return TEXT_TO_TYPE.getOrDefault(key.toLowerCase(), AttackEffectType.NONE);
    }

    /**
     * Extracts the numeric amount from a descriptor of the form {@code "keyword:N"}.
     * Package-private for direct test access.
     *
     * @param effectText the raw descriptor
     * @return parsed amount, or {@code 0} if none present or parsing fails
     */
    int extractAmount(final String effectText) {
        if (effectText == null || !effectText.contains(":")) {
            return 0;
        }
        try {
            return Integer.parseInt(effectText.substring(effectText.indexOf(':') + 1));
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
