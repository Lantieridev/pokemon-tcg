package ar.edu.utn.frc.tup.piii.engine.listener;

import ar.edu.utn.frc.tup.piii.engine.model.Card;

/**
 * Provides read-only access to a player's hand for rule validation purposes.
 */
public interface HandStateProvider {

    /**
     * Retrieves a card from a specific player's hand by its unique card ID.
     *
     * @param playerIndex the index of the player
     * @param cardId      the unique identifier of the card to find
     * @return the Card if found, or null if the player doesn't have it
     */
    Card getCardInHand(int playerIndex, String cardId);
}
