package ar.edu.utn.frc.tup.piii.engine.listener;

import ar.edu.utn.frc.tup.piii.engine.model.Card;
import java.util.List;

/**
 * Provides access to a player's discard pile cards.
 * Used for Trainer validations (e.g. Max Revive).
 */
public interface DiscardPileStateProvider {

    /**
     * Returns an unmodifiable list of cards in the given player's discard pile.
     *
     * @param playerIndex zero-based player index
     * @return discard pile cards (never null)
     */
    List<Card> getDiscardPile(int playerIndex);
}
