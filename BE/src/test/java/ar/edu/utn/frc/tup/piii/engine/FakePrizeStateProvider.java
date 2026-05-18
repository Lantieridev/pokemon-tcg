package ar.edu.utn.frc.tup.piii.engine;

import ar.edu.utn.frc.tup.piii.engine.listener.PrizeStateProvider;

import java.util.HashMap;
import java.util.Map;

/**
 * Test double for PrizeStateProvider backed by a HashMap.
 */
final class FakePrizeStateProvider implements PrizeStateProvider {

    private static final int DEFAULT_PRIZES = 6;

    private final Map<Integer, Integer> prizes = new HashMap<>();

    public void set(final int playerIndex, final int remaining) {
        prizes.put(playerIndex, remaining);
    }

    @Override
    public int getRemainingPrizes(final int playerIndex) {
        return prizes.getOrDefault(playerIndex, DEFAULT_PRIZES);
    }
}
