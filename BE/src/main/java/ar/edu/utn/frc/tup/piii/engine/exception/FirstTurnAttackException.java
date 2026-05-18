package ar.edu.utn.frc.tup.piii.engine.exception;

/**
 * Thrown when the first player attempts to attack on their very first turn, which is forbidden
 * by the Pokémon TCG rules. Extends InvalidTurnPhaseException because it is a phase-rule
 * violation. FR-013.
 */
public class FirstTurnAttackException extends InvalidTurnPhaseException {

    /**
     * Constructs a FirstTurnAttackException with the specified detail message.
     *
     * @param message the detail message
     */
    public FirstTurnAttackException(final String message) {
        super(message);
    }

    /**
     * Constructs a FirstTurnAttackException with the specified detail message and cause.
     *
     * @param message the detail message
     * @param cause   the cause
     */
    public FirstTurnAttackException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
