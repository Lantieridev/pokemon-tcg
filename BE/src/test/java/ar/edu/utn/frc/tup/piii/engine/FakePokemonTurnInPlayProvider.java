package ar.edu.utn.frc.tup.piii.engine;

import ar.edu.utn.frc.tup.piii.engine.listener.PokemonTurnInPlayProvider;
import ar.edu.utn.frc.tup.piii.engine.model.BattlePokemonState;

import java.util.HashMap;
import java.util.Map;

/**
 * Test double for PokemonTurnInPlayProvider backed by a HashMap.
 */
public final class FakePokemonTurnInPlayProvider implements PokemonTurnInPlayProvider {

    private final Map<BattlePokemonState, Integer> turns = new HashMap<>();

    public void set(final BattlePokemonState pokemon, final int turnsInPlay) {
        turns.put(pokemon, turnsInPlay);
    }

    @Override
    public int getTurnsInPlay(final BattlePokemonState pokemon) {
        return turns.getOrDefault(pokemon, 0);
    }
}
