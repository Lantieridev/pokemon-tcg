package ar.edu.utn.frc.tup.piii.engine.exception;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * Tests for the engine exception hierarchy. FR-019.
 */
class ExceptionTest {

    @Test
    void shouldConstructPokemonAsleepExceptionWithMessage() {
        PokemonAsleepException ex = new PokemonAsleepException("asleep");
        assertEquals("asleep", ex.getMessage());
    }

    @Test
    void shouldConstructPokemonParalyzedExceptionWithMessage() {
        PokemonParalyzedException ex = new PokemonParalyzedException("paralyzed");
        assertEquals("paralyzed", ex.getMessage());
    }

    @Test
    void shouldConstructInvalidStatusEffectExceptionWithMessageAndCause() {
        Throwable cause = new IllegalArgumentException("root");
        InvalidStatusEffectException ex =
                new InvalidStatusEffectException("invalid effect", cause);
        assertEquals("invalid effect", ex.getMessage());
        assertSame(cause, ex.getCause());
    }
}
