package ar.edu.utn.frc.tup.piii.engine.exception;

/**
 * Thrown by MatchService when a player submits an action that fails rule validation.
 */
public class InvalidActionException extends RuntimeException {

    /**
     * Constructs an InvalidActionException with the given reason.
     *
     * @param reason the validation failure reason (never null)
     */
    public InvalidActionException(final String reason) {
        super(reason);
    }
}
