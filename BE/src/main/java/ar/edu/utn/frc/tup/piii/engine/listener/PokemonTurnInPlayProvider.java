package ar.edu.utn.frc.tup.piii.engine.listener;

import ar.edu.utn.frc.tup.piii.engine.model.BattlePokemonState;

/**
 * Provides the number of full turns a Pokémon has been in play.
 * A return value of {@code 0} means the Pokémon entered play this turn.
 * A return value of {@code 1} or more means it survived at least one end-of-turn boundary.
 * FR-007.
 */
public interface PokemonTurnInPlayProvider {

    /**
     * Returns the number of turns the given Pokémon has been in play.
     *
     * @param pokemon the Pokémon to query (never null)
     * @return turns in play (>= 0)
     */
    int getTurnsInPlay(BattlePokemonState pokemon);
}
