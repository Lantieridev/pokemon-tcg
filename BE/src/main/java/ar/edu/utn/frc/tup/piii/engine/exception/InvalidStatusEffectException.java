package ar.edu.utn.frc.tup.piii.engine.exception;

/**
 * Thrown when an invalid status effect is supplied to the engine (e.g. null).
 * FR-019.
 */
public class InvalidStatusEffectException extends RuntimeException {

    /**
     * Constructs an InvalidStatusEffectException with the specified detail message.
     *
     * @param message the detail message
     */
    public InvalidStatusEffectException(final String message) {
        super(message);
    }

    /**
     * Constructs an InvalidStatusEffectException with the specified detail message and cause.
     *
     * @param message the detail message
     * @param cause   the cause
     */
    public InvalidStatusEffectException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
