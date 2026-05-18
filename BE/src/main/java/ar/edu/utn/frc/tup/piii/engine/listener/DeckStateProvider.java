package ar.edu.utn.frc.tup.piii.engine.listener;

/**
 * Provides deck-size state for a player.
 * A return value of {@code 0} from {@link #getDeckSize} means the player cannot draw (deck-out).
 * FR-008.
 */
public interface DeckStateProvider {

    /**
     * Returns the number of cards remaining in the given player's deck.
     *
     * @param playerIndex zero-based player index
     * @return deck size (>= 0)
     */
    int getDeckSize(int playerIndex);
}
