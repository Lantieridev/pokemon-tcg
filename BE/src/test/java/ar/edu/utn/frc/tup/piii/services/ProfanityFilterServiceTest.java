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

    @Test
    void shouldDetectProfanities() {
        final String message = "Eres un noob y un idiota";
        final java.util.List<String> expected = java.util.List.of("noob", "n00b", "idiota");
        final java.util.List<String> actual = profanityFilterService.getProfaneWords(message);
        assertEquals(expected.size(), actual.size());
        org.junit.jupiter.api.Assertions.assertTrue(actual.containsAll(expected));
    }

    @Test
    void shouldReturnEmptyListWhenNoProfanity() {
        final String message = "Hola amigo, que tal";
        final java.util.List<String> actual = profanityFilterService.getProfaneWords(message);
        org.junit.jupiter.api.Assertions.assertTrue(actual.isEmpty());
    }

    @Test
    void shouldReturnEmptyListWhenMessageIsNull() {
        org.junit.jupiter.api.Assertions.assertTrue(profanityFilterService.getProfaneWords(null).isEmpty());
    }
}
