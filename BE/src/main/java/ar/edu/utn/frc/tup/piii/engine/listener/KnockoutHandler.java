package ar.edu.utn.frc.tup.piii.engine.listener;

import ar.edu.utn.frc.tup.piii.engine.model.BattlePokemonState;

/**
 * Callback invoked by KnockoutManager when a Pokémon is detected as knocked out.
 * Declared as a functional interface so callers may supply lambda expressions. FR-014.
 */
@FunctionalInterface
public interface KnockoutHandler {

    /**
     * Called when a Pokémon's damage counters meet or exceed its maximum HP.
     *
     * @param knocked      the Pokémon that has been knocked out
     * @param prizesToTake number of prize cards the opponent should take (1 for normal, 2 for EX)
     */
    void onKnockout(BattlePokemonState knocked, int prizesToTake);
}
