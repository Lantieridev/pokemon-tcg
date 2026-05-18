package ar.edu.utn.frc.tup.piii.engine;

import ar.edu.utn.frc.tup.piii.engine.listener.BenchStateProvider;

import java.util.HashMap;
import java.util.Map;

/**
 * Test double for BenchStateProvider backed by a HashMap.
 */
final class FakeBenchStateProvider implements BenchStateProvider {

    private static final int DEFAULT_BENCH_SIZE = 5;

    private final Map<Integer, Integer> sizes = new HashMap<>();

    public void set(final int playerIndex, final int benchSize) {
        sizes.put(playerIndex, benchSize);
    }

    @Override
    public int getBenchSize(final int playerIndex) {
        return sizes.getOrDefault(playerIndex, DEFAULT_BENCH_SIZE);
    }
}
