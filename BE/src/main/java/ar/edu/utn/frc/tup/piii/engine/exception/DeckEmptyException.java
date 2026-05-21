package ar.edu.utn.frc.tup.piii.engine.exception;

/**
 * Thrown when a draw is attempted on an empty deck.
 * Per XY1 rules §2.1: a player who cannot draw at the start of their turn loses the match.
 */
public class DeckEmptyException extends RuntimeException {

    public DeckEmptyException(final String message) {
        super(message);
    }
}
