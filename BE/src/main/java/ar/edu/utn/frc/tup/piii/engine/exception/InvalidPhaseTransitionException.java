package ar.edu.utn.frc.tup.piii.engine.exception;

/**
 * Thrown when a phase-transition method is called from a phase that does not permit it. FR-013.
 */
public class InvalidPhaseTransitionException extends RuntimeException {

    /**
     * Constructs an InvalidPhaseTransitionException with the specified detail message.
     *
     * @param message the detail message
     */
    public InvalidPhaseTransitionException(final String message) {
        super(message);
    }

    /**
     * Constructs an InvalidPhaseTransitionException with the specified detail message and cause.
     *
     * @param message the detail message
     * @param cause   the cause
     */
    public InvalidPhaseTransitionException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
