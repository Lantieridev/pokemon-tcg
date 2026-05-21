package ar.edu.utn.frc.tup.piii.engine.exception;

/**
 * Thrown when a card removal is attempted for a card ID not present in the player's hand.
 */
public class CardNotInHandException extends RuntimeException {

    public CardNotInHandException(final String cardId) {
        super("Card not found in hand: " + cardId);
    }
}
