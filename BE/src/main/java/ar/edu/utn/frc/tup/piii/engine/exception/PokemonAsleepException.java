package ar.edu.utn.frc.tup.piii.engine.exception;

/**
 * Thrown when a Pokémon attempts to attack while under the DORMIDO status condition.
 * FR-019.
 */
public class PokemonAsleepException extends RuntimeException {

    /**
     * Constructs a PokemonAsleepException with the specified detail message.
     *
     * @param message the detail message
     */
    public PokemonAsleepException(final String message) {
        super(message);
    }

    /**
     * Constructs a PokemonAsleepException with the specified detail message and cause.
     *
     * @param message the detail message
     * @param cause   the cause
     */
    public PokemonAsleepException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
