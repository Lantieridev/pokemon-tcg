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
import ar.edu.utn.frc.tup.piii.engine.statuseffect.PrecisionBajaEffect;
import ar.edu.utn.frc.tup.piii.engine.statuseffect.StatusEffect;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
            StatusEffectType.PARALIZADO,
            StatusEffectType.PRECISION_BAJA
    );

    private final Map<StatusEffectType, StatusEffect> effectsByType = new HashMap<>();
    private CoinFlipper coinFlipper;
    private ar.edu.utn.frc.tup.piii.engine.session.PlayerRuntime playerRuntime;
    private String disabledAttackName;
    private boolean damagePreventedNextTurn;
    private boolean damagePreventedIf60OrLessNextTurn;
    private boolean damageReducedBy20NextTurn;
    private String selfDisabledAttackName;
    private boolean selfDisabledAttackSetThisTurn;
    private boolean selfDisabledNextTurn;
    private boolean selfDisabledNextTurnSetThisTurn;
    private boolean retreatBlockedNextTurn;
    private boolean retreatBlockedNextTurnSetThisTurn;
    private boolean drawStepBlocked;
    private boolean excitingShakeActiveNextTurn;
    private boolean excitingShakeActiveNextTurnSetThisTurn;
    private boolean strongGustUsedLastTurn;
    private boolean strongGustUsedLastTurnSetThisTurn;
    private int poisonDamageCounters = 1;

    /**
     * Constructs a StatusEffectManager with the given CoinFlipper.
     *
     * @param coinFlipper the coin flipper used for probabilistic outcomes (must not be null)
     * @throws NullPointerException if {@code coinFlipper} is null
     */
    public StatusEffectManager(final CoinFlipper coinFlipper) {
        this.coinFlipper = Objects.requireNonNull(coinFlipper, "coinFlipper must not be null");
    }

    public void setCoinFlipper(final CoinFlipper coinFlipper) {
        this.coinFlipper = Objects.requireNonNull(coinFlipper, "coinFlipper must not be null");
    }

    /**
     * Binds the owning {@link ar.edu.utn.frc.tup.piii.engine.session.PlayerRuntime} once
     * the circular dependency can be resolved (StatusEffectManager is built before
     * PlayerRuntime exists). Set-once semantics: subsequent calls fail fast.
     *
     * @param playerRuntime the owning runtime (must not be null)
     * @throws NullPointerException  if {@code playerRuntime} is null
     * @throws IllegalStateException if a runtime has already been bound
     */
    public void setPlayerRuntime(final ar.edu.utn.frc.tup.piii.engine.session.PlayerRuntime playerRuntime) {
        Objects.requireNonNull(playerRuntime, "playerRuntime must not be null");
        if (this.playerRuntime != null) {
            throw new IllegalStateException("playerRuntime already bound; set-once semantics");
        }
        this.playerRuntime = playerRuntime;
    }

    public ar.edu.utn.frc.tup.piii.engine.session.PlayerRuntime getPlayerRuntime() {
        return playerRuntime;
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
        apply(type, 1);
    }

    public void apply(final StatusEffectType type, final int poisonDamage) {
        if (type == null) {
            throw new InvalidStatusEffectException("Status effect type must not be null");
        }
        if (type == StatusEffectType.ENVENENADO) {
            this.poisonDamageCounters = poisonDamage;
        }
        
        if (ar.edu.utn.frc.tup.piii.engine.pipeline.PassiveAbilityRegistry.preventStatusEffect(type, playerRuntime)) {
            return;
        }
        StatusEffect newEffect = buildEffect(type);
        if (newEffect.isRotationSlot()) {
            effectsByType.entrySet().removeIf(e -> e.getValue().isRotationSlot());
        }
        effectsByType.put(type, newEffect);
    }

    /**
     * Checks if the active Pokemon meets the conditions for Sweet Veil protection.
     * If so, clears all Special Conditions immediately.
     */
    public void checkSweetVeil() {
        if (ar.edu.utn.frc.tup.piii.engine.pipeline.PassiveAbilityRegistry.preventStatusEffect(null, playerRuntime)) {
            clearAll();
        }
    }

    /**
     * Removes the status effect of the given type, if present. No-op if absent.
     *
     * @param type the type of status effect to remove
     */
    public void remove(final StatusEffectType type) {
        effectsByType.remove(type);
    }

    /**
     * Removes all active status effects.
     */
    public void clearAll() {
        effectsByType.clear();
        this.disabledAttackName = null;
        this.damagePreventedNextTurn = false;
        this.damagePreventedIf60OrLessNextTurn = false;
        this.damageReducedBy20NextTurn = false;
        this.selfDisabledAttackName = null;
        this.selfDisabledAttackSetThisTurn = false;
        this.selfDisabledNextTurn = false;
        this.selfDisabledNextTurnSetThisTurn = false;
        this.retreatBlockedNextTurn = false;
        this.retreatBlockedNextTurnSetThisTurn = false;
        this.drawStepBlocked = false;
        this.excitingShakeActiveNextTurn = false;
        this.excitingShakeActiveNextTurnSetThisTurn = false;
        this.strongGustUsedLastTurn = false;
        this.strongGustUsedLastTurnSetThisTurn = false;
        this.poisonDamageCounters = 1;
    }

    /**
     * Returns whether the given status effect is currently active.
     *
     * @param type the type to check
     * @return {@code true} if active
     */
    public boolean has(final StatusEffectType type) {
        return effectsByType.containsKey(type);
    }

    /**
     * Returns an unmodifiable snapshot of the currently active effect types.
     *
     * @return immutable set of active StatusEffectType values
     */
    public Set<StatusEffectType> activeEffects() {
        return Set.copyOf(effectsByType.keySet());
    }

    /**
     * Returns whether the Pokémon is allowed to attack this turn.
     *
     * @return {@code true} if no active effect blocks attacks
     */
    public boolean canAttack() {
        return effectsByType.values().stream().noneMatch(StatusEffect::blocksAttack);
    }

    /**
     * Returns whether the Pokémon is allowed to retreat this turn.
     *
     * @return {@code true} if no active effect blocks retreating
     */
    public boolean canRetreat() {
        if (retreatBlockedNextTurn) {
            return false;
        }
        return effectsByType.values().stream().noneMatch(StatusEffect::blocksRetreat);
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
        if (has(StatusEffectType.PRECISION_BAJA)) {
            boolean headsResult = coinFlipper.flip();
            if (!headsResult) {
                return new AttackModifierResult.SmokescreenFailed();
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
        processBetweenTurns(pokemon, true);
    }

    public void processBetweenTurns(final ActivePokemonState pokemon, final boolean isOwnerTurnEnding) {
        List<StatusEffectType> toRemove = new ArrayList<>();
        for (StatusEffectType type : BETWEEN_TURNS_ORDER) {
            StatusEffect effect = effectsByType.get(type);
            if (effect != null) {
                if ((type == StatusEffectType.PARALIZADO || type == StatusEffectType.PRECISION_BAJA) && !isOwnerTurnEnding) {
                    continue;
                }
                boolean shouldRemove = effect.processBetweenTurns(pokemon, coinFlipper);
                if (shouldRemove) {
                    toRemove.add(type);
                }
            }
        }
        toRemove.forEach(effectsByType::remove);
    }


    private StatusEffect buildEffect(final StatusEffectType type) {
        return switch (type) {
            case DORMIDO -> new AsleepEffect();
            case QUEMADO -> new BurnedEffect();
            case CONFUNDIDO -> new ConfusedEffect();
            case PARALIZADO -> new ParalyzedEffect();
            case ENVENENADO -> new PoisonedEffect(poisonDamageCounters);
            case PRECISION_BAJA -> new PrecisionBajaEffect();
        };
    }

    public String getDisabledAttackName() {
        return disabledAttackName;
    }

    public void setDisabledAttackName(final String disabledAttackName) {
        this.disabledAttackName = disabledAttackName;
    }

    public boolean isDamagePreventedNextTurn() {
        return damagePreventedNextTurn;
    }

    public void setDamagePreventedNextTurn(final boolean damagePreventedNextTurn) {
        this.damagePreventedNextTurn = damagePreventedNextTurn;
    }

    public boolean isDamagePreventedIf60OrLessNextTurn() {
        return damagePreventedIf60OrLessNextTurn;
    }

    public void setDamagePreventedIf60OrLessNextTurn(final boolean damagePreventedIf60OrLessNextTurn) {
        this.damagePreventedIf60OrLessNextTurn = damagePreventedIf60OrLessNextTurn;
    }

    public boolean isDamageReducedBy20NextTurn() {
        return damageReducedBy20NextTurn;
    }

    public void setDamageReducedBy20NextTurn(final boolean damageReducedBy20NextTurn) {
        this.damageReducedBy20NextTurn = damageReducedBy20NextTurn;
    }

    public String getSelfDisabledAttackName() {
        return selfDisabledAttackName;
    }

    public void setSelfDisabledAttackName(final String selfDisabledAttackName) {
        this.selfDisabledAttackName = selfDisabledAttackName;
    }

    public boolean isSelfDisabledAttackSetThisTurn() {
        return selfDisabledAttackSetThisTurn;
    }

    public void setSelfDisabledAttackSetThisTurn(final boolean selfDisabledAttackSetThisTurn) {
        this.selfDisabledAttackSetThisTurn = selfDisabledAttackSetThisTurn;
    }

    public boolean isSelfDisabledNextTurn() {
        return selfDisabledNextTurn;
    }

    public void setSelfDisabledNextTurn(final boolean selfDisabledNextTurn) {
        this.selfDisabledNextTurn = selfDisabledNextTurn;
    }

    public boolean isSelfDisabledNextTurnSetThisTurn() {
        return selfDisabledNextTurnSetThisTurn;
    }

    public void setSelfDisabledNextTurnSetThisTurn(final boolean selfDisabledNextTurnSetThisTurn) {
        this.selfDisabledNextTurnSetThisTurn = selfDisabledNextTurnSetThisTurn;
    }

    public boolean isRetreatBlockedNextTurn() {
        return retreatBlockedNextTurn;
    }

    public void setRetreatBlockedNextTurn(final boolean retreatBlockedNextTurn) {
        this.retreatBlockedNextTurn = retreatBlockedNextTurn;
    }

    public boolean isRetreatBlockedNextTurnSetThisTurn() {
        return retreatBlockedNextTurnSetThisTurn;
    }

    public void setRetreatBlockedNextTurnSetThisTurn(final boolean retreatBlockedNextTurnSetThisTurn) {
        this.retreatBlockedNextTurnSetThisTurn = retreatBlockedNextTurnSetThisTurn;
    }

    public boolean isDrawStepBlocked() {
        return drawStepBlocked;
    }

    public void setDrawStepBlocked(final boolean drawStepBlocked) {
        this.drawStepBlocked = drawStepBlocked;
    }

    public boolean isExcitingShakeActiveNextTurn() {
        return excitingShakeActiveNextTurn;
    }

    public void setExcitingShakeActiveNextTurn(final boolean excitingShakeActiveNextTurn) {
        this.excitingShakeActiveNextTurn = excitingShakeActiveNextTurn;
    }

    public boolean isExcitingShakeActiveNextTurnSetThisTurn() {
        return excitingShakeActiveNextTurnSetThisTurn;
    }

    public void setExcitingShakeActiveNextTurnSetThisTurn(final boolean excitingShakeActiveNextTurnSetThisTurn) {
        this.excitingShakeActiveNextTurnSetThisTurn = excitingShakeActiveNextTurnSetThisTurn;
    }

    public boolean isStrongGustUsedLastTurn() {
        return strongGustUsedLastTurn;
    }

    public void setStrongGustUsedLastTurn(final boolean strongGustUsedLastTurn) {
        this.strongGustUsedLastTurn = strongGustUsedLastTurn;
    }

    public boolean isStrongGustUsedLastTurnSetThisTurn() {
        return strongGustUsedLastTurnSetThisTurn;
    }

    public void setStrongGustUsedLastTurnSetThisTurn(final boolean strongGustUsedLastTurnSetThisTurn) {
        this.strongGustUsedLastTurnSetThisTurn = strongGustUsedLastTurnSetThisTurn;
    }

    public int getPoisonDamageCounters() {
        return poisonDamageCounters;
    }

    public void setPoisonDamageCounters(final int poisonDamageCounters) {
        this.poisonDamageCounters = poisonDamageCounters;
    }
}
