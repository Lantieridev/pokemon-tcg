package ar.edu.utn.frc.tup.piii.engine;

import ar.edu.utn.frc.tup.piii.engine.listener.BenchStateProvider;
import ar.edu.utn.frc.tup.piii.engine.model.BattlePokemonState;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Test double for BenchStateProvider backed by HashMaps.
 */
public final class FakeBenchStateProvider implements BenchStateProvider {

    private static final int DEFAULT_BENCH_SIZE = 5;

    private final Map<Integer, Integer> sizes = new HashMap<>();
    private final Map<Integer, List<BattlePokemonState>> benched = new HashMap<>();

    public void set(final int playerIndex, final int benchSize) {
        sizes.put(playerIndex, benchSize);
    }

    public void setBenched(final int playerIndex, final List<BattlePokemonState> pokemon) {
        benched.put(playerIndex, new ArrayList<>(pokemon));
    }

    @Override
    public int getBenchSize(final int playerIndex) {
        return sizes.getOrDefault(playerIndex, DEFAULT_BENCH_SIZE);
    }

    @Override
    public List<BattlePokemonState> getBenchedPokemon(final int playerIndex) {
        return benched.getOrDefault(playerIndex, List.of());
    }
}
