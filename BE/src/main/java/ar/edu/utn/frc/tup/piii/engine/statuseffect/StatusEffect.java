package ar.edu.utn.frc.tup.piii.engine.statuseffect;

import ar.edu.utn.frc.tup.piii.engine.model.ActivePokemonState;
import ar.edu.utn.frc.tup.piii.engine.model.CoinFlipper;
import ar.edu.utn.frc.tup.piii.engine.model.StatusEffectType;

/**
 * Strategy interface for all status conditions. Each concrete implementation
 * encapsulates the rules for one condition (DORMIDO, QUEMADO, etc.). FR-005.
 */
public interface StatusEffect {

    /**
     * Returns the type of this status condition.
     *
     * @return the StatusEffectType
     */
    StatusEffectType getType();

    /**
     * Returns whether this effect occupies the rotation slot.
     * Only one rotation-slot effect can be active at a time.
     *
     * @return {@code true} if this effect is a rotation-slot condition
     */
    boolean isRotationSlot();

    /**
     * Returns whether this condition prevents the Pokémon from attacking.
     *
     * @return {@code true} if the attack is blocked
     */
    boolean blocksAttack();

    /**
     * Returns whether this condition prevents the Pokémon from retreating.
     *
     * @return {@code true} if retreating is blocked
     */
    boolean blocksRetreat();

    /**
     * Processes this effect at the between-turns phase.
     *
     * @param state   the active Pokémon whose counters may be mutated
     * @param flipper the coin flipper used for probabilistic outcomes
     * @return {@code true} if this effect should be removed after processing,
     *         {@code false} if it persists
     */
    boolean processBetweenTurns(ActivePokemonState state, CoinFlipper flipper);
}
