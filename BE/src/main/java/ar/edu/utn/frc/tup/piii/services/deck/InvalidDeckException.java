package ar.edu.utn.frc.tup.piii.services.deck;

public final class InvalidDeckException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public InvalidDeckException(final String message) {
        super(message);
    }
}
