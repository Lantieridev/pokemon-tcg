package ar.edu.utn.frc.tup.piii.engine.exception;

/**
 * Thrown when a Pokémon attempts to attack while under the PARALIZADO status condition.
 * FR-019.
 */
public class PokemonParalyzedException extends RuntimeException {

    /**
     * Constructs a PokemonParalyzedException with the specified detail message.
     *
     * @param message the detail message
     */
    public PokemonParalyzedException(final String message) {
        super(message);
    }

    /**
     * Constructs a PokemonParalyzedException with the specified detail message and cause.
     *
     * @param message the detail message
     * @param cause   the cause
     */
    public PokemonParalyzedException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
