package ar.edu.utn.frc.tup.piii.engine.listener;

import ar.edu.utn.frc.tup.piii.engine.model.Card;

import java.util.Optional;

/**
 * Provides read-only access to a player's hand for rule validation purposes.
 */
public interface HandStateProvider {

    /**
     * Retrieves a card from a specific player's hand by its unique card ID.
     *
     * @param playerIndex the index of the player
     * @param cardId      the unique identifier of the card to find
     * @return an {@link Optional} containing the matching card, or
     *         {@link Optional#empty()} if the player doesn't have it
     *         (or the snapshot only carries IDs, not full cards).
     */
    /**
     * Returns the size of the player's hand.
     *
     * @param playerIndex zero-based player index
     * @return hand size
     */
    default int getHandSize(int playerIndex) {
        return 0;
    }

    Optional<Card> getCardInHand(int playerIndex, String cardId);

    default java.util.List<Card> getHandCards(int playerIndex) {
        return java.util.List.of();
    }
}
