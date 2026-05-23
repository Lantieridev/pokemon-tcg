package ar.edu.utn.frc.tup.piii.services;

import ar.edu.utn.frc.tup.piii.services.impl.ProfanityFilterServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ProfanityFilterServiceTest {

    private ProfanityFilterService profanityFilterService;

    @BeforeEach
    void setUp() {
        profanityFilterService = new ProfanityFilterServiceImpl();
    }

    @Test
    void shouldFilterProfanityCaseInsensitively() {
        final String rawMessage = "You are such a loser and a noob!";
        final String expected = "You are such a ***** and a ****!";
        assertEquals(expected, profanityFilterService.filter(rawMessage));
    }

    @Test
    void shouldFilterSpanishProfanity() {
        final String rawMessage = "Que mierda de juego, eres un tonto idiota";
        final String expected = "Que ****** de juego, eres un ***** ******";
        assertEquals(expected, profanityFilterService.filter(rawMessage));
    }

    @Test
    void shouldNotFilterWhenWordIsPartOfAnotherWord() {
        final String rawMessage = "This is not nooby, nor tontito";
        assertEquals(rawMessage, profanityFilterService.filter(rawMessage));
    }

    @Test
    void shouldReturnNullWhenMessageIsNull() {
        assertNull(profanityFilterService.filter(null));
    }

    @Test
    void shouldHandleMixedCasingOfProfanity() {
        final String rawMessage = "ChEaT and EsTuPiDo";
        final String expected = "***** and ********";
        assertEquals(expected, profanityFilterService.filter(rawMessage));
    }
}
