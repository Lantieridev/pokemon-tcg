package ar.edu.utn.frc.tup.piii.engine;

import ar.edu.utn.frc.tup.piii.engine.listener.DeckStateProvider;

import java.util.HashMap;
import java.util.Map;

/**
 * Test double for DeckStateProvider backed by a HashMap.
 */
public final class FakeDeckStateProvider implements DeckStateProvider {

    private static final int DEFAULT_DECK_SIZE = 60;

    private final Map<Integer, Integer> sizes = new HashMap<>();

    public void set(final int playerIndex, final int deckSize) {
        sizes.put(playerIndex, deckSize);
    }

    @Override
    public int getDeckSize(final int playerIndex) {
        return sizes.getOrDefault(playerIndex, DEFAULT_DECK_SIZE);
    }
}
