package ar.edu.utn.frc.tup.piii.engine.manager;

import ar.edu.utn.frc.tup.piii.engine.exception.InvalidStatusEffectException;
import ar.edu.utn.frc.tup.piii.engine.exception.PokemonAsleepException;
import ar.edu.utn.frc.tup.piii.engine.exception.PokemonParalyzedException;
import ar.edu.utn.frc.tup.piii.engine.model.ActivePokemonState;
import ar.edu.utn.frc.tup.piii.engine.model.AttackModifierResult;
import ar.edu.utn.frc.tup.piii.engine.model.CoinFlipper;
import ar.edu.utn.frc.tup.piii.engine.model.StatusEffectType;
import ar.edu.utn.frc.tup.piii.engine.statuseffect.AsleepEffect;
import ar.edu.utn.frc.tup.piii.engine.statuseffect.BurnedEffect;
import ar.edu.utn.frc.tup.piii.engine.statuseffect.ConfusedEffect;
import ar.edu.utn.frc.tup.piii.engine.statuseffect.ParalyzedEffect;
import ar.edu.utn.frc.tup.piii.engine.statuseffect.PoisonedEffect;
import ar.edu.utn.frc.tup.piii.engine.statuseffect.StatusEffect;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Manages all active status conditions on a single Pokémon.
 * Enforces rotation-slot exclusivity, processes between-turns effects,
 * and gates attack/retreat actions. FR-011 through FR-017.
 */
public class StatusEffectManager {

    private static final int CONFUSION_SELF_DAMAGE_COUNTERS = 3;

    private static final List<StatusEffectType> BETWEEN_TURNS_ORDER = List.of(
            StatusEffectType.ENVENENADO,
            StatusEffectType.QUEMADO,
            StatusEffectType.DORMIDO,
            StatusEffectType.PARALIZADO
    );

    private final Map<StatusEffectType, StatusEffect> activeEffects = new HashMap<>();
    private final CoinFlipper coinFlipper;

    /**
     * Constructs a StatusEffectManager with the given CoinFlipper.
     *
     * @param coinFlipper the coin flipper used for probabilistic outcomes (must not be null)
     */
    public StatusEffectManager(final CoinFlipper coinFlipper) {
        this.coinFlipper = coinFlipper;
    }

    /**
     * Applies the status effect of the given type to this Pokémon.
     * If the new effect occupies the rotation slot, any existing rotation-slot effect
     * is removed first. Applying the same marker effect twice is idempotent.
     *
     * @param type the type of status effect to apply
     * @throws InvalidStatusEffectException if {@code type} is null
     */
    public void apply(final StatusEffectType type) {
        if (type == null) {
            throw new InvalidStatusEffectException("Status effect type must not be null");
        }
        StatusEffect newEffect = buildEffect(type);
        if (newEffect.isRotationSlot()) {
            activeEffects.entrySet().removeIf(e -> e.getValue().isRotationSlot());
        }
        activeEffects.put(type, newEffect);
    }

    /**
     * Removes the status effect of the given type, if present. No-op if absent.
     *
     * @param type the type of status effect to remove
     */
    public void remove(final StatusEffectType type) {
        activeEffects.remove(type);
    }

    /**
     * Removes all active status effects.
     */
    public void clearAll() {
        activeEffects.clear();
    }

    /**
     * Returns whether the given status effect is currently active.
     *
     * @param type the type to check
     * @return {@code true} if active
     */
    public boolean has(final StatusEffectType type) {
        return activeEffects.containsKey(type);
    }

    /**
     * Returns an unmodifiable snapshot of the currently active effect types.
     *
     * @return immutable set of active StatusEffectType values
     */
    public Set<StatusEffectType> activeEffects() {
        return Set.copyOf(activeEffects.keySet());
    }

    /**
     * Returns whether the Pokémon is allowed to attack this turn.
     *
     * @return {@code true} if no active effect blocks attacks
     */
    public boolean canAttack() {
        return activeEffects.values().stream().noneMatch(StatusEffect::blocksAttack);
    }

    /**
     * Returns whether the Pokémon is allowed to retreat this turn.
     *
     * @return {@code true} if no active effect blocks retreating
     */
    public boolean canRetreat() {
        return activeEffects.values().stream().noneMatch(StatusEffect::blocksRetreat);
    }

    /**
     * Processes pre-attack modifiers.
     * Throws if the Pokémon is asleep or paralyzed.
     * Resolves confusion via a coin flip when confused.
     *
     * @param attacker the mutable state of the attacking Pokémon
     * @return {@link AttackModifierResult.Proceed} if the attack may continue,
     *         or {@link AttackModifierResult.ConfusionFailed} if confusion triggers self-damage
     * @throws PokemonAsleepException     if DORMIDO is active
     * @throws PokemonParalyzedException  if PARALIZADO is active
     */
    public AttackModifierResult onAttackAttempt(final ActivePokemonState attacker) {
        if (has(StatusEffectType.DORMIDO)) {
            throw new PokemonAsleepException("Pokemon is asleep and cannot attack");
        }
        if (has(StatusEffectType.PARALIZADO)) {
            throw new PokemonParalyzedException("Pokemon is paralyzed and cannot attack");
        }
        if (has(StatusEffectType.CONFUNDIDO)) {
            boolean headsResult = coinFlipper.flip();
            if (!headsResult) {
                attacker.addDamageCounters(CONFUSION_SELF_DAMAGE_COUNTERS);
                return new AttackModifierResult.ConfusionFailed(CONFUSION_SELF_DAMAGE_COUNTERS);
            }
        }
        return new AttackModifierResult.Proceed();
    }

    /**
     * Processes all between-turns effects in the canonical order:
     * ENVENENADO → QUEMADO → DORMIDO → PARALIZADO.
     * Effects that signal removal are collected and removed after iteration
     * to avoid ConcurrentModificationException.
     *
     * @param pokemon the mutable state of the active Pokémon
     */
    public void processBetweenTurns(final ActivePokemonState pokemon) {
        List<StatusEffectType> toRemove = new ArrayList<>();
        for (StatusEffectType type : BETWEEN_TURNS_ORDER) {
            StatusEffect effect = activeEffects.get(type);
            if (effect != null) {
                boolean shouldRemove = effect.processBetweenTurns(pokemon, coinFlipper);
                if (shouldRemove) {
                    toRemove.add(type);
                }
            }
        }
        toRemove.forEach(activeEffects::remove);
    }

    private StatusEffect buildEffect(final StatusEffectType type) {
        return switch (type) {
            case DORMIDO -> new AsleepEffect();
            case QUEMADO -> new BurnedEffect();
            case CONFUNDIDO -> new ConfusedEffect();
            case PARALIZADO -> new ParalyzedEffect();
            case ENVENENADO -> new PoisonedEffect();
        };
    }
}
