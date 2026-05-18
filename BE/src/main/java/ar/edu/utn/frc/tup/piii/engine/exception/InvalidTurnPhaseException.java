package ar.edu.utn.frc.tup.piii.engine.exception;

/**
 * Thrown when an action is attempted in a turn phase where it is not permitted. FR-013.
 */
public class InvalidTurnPhaseException extends RuntimeException {

    /**
     * Constructs an InvalidTurnPhaseException with the specified detail message.
     *
     * @param message the detail message
     */
    public InvalidTurnPhaseException(final String message) {
        super(message);
    }

    /**
     * Constructs an InvalidTurnPhaseException with the specified detail message and cause.
     *
     * @param message the detail message
     * @param cause   the cause
     */
    public InvalidTurnPhaseException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
