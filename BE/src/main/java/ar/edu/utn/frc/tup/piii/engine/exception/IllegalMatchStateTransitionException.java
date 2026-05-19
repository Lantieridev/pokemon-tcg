package ar.edu.utn.frc.tup.piii.engine.exception;

/**
 * Thrown when a match-session state-machine transition is attempted
 * from a state that does not permit it (e.g. start() on ACTIVE, finish() on WAITING).
 */
public class IllegalMatchStateTransitionException extends RuntimeException {

    /**
     * Constructs an IllegalMatchStateTransitionException with the specified detail message.
     *
     * @param message the detail message
     */
    public IllegalMatchStateTransitionException(final String message) {
        super(message);
    }
}
