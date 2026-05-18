package ar.edu.utn.frc.tup.piii.engine.model;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for PokemonType enum. FR-001.
 */
class PokemonTypeTest {

    private static final int EXPECTED_TYPE_COUNT = 11;

    @Test
    void shouldExposeExactlyElevenValuesWhenEnumIsInspected() {
        assertEquals(EXPECTED_TYPE_COUNT, PokemonType.values().length);
    }

    @Test
    void shouldContainAllXy1TypeNamesWhenEnumIsInspected() {
        Set<String> names = Arrays.stream(PokemonType.values())
                .map(Enum::name)
                .collect(Collectors.toSet());

        assertTrue(names.contains("FIRE"));
        assertTrue(names.contains("WATER"));
        assertTrue(names.contains("GRASS"));
        assertTrue(names.contains("LIGHTNING"));
        assertTrue(names.contains("PSYCHIC"));
        assertTrue(names.contains("FIGHTING"));
        assertTrue(names.contains("DARKNESS"));
        assertTrue(names.contains("METAL"));
        assertTrue(names.contains("FAIRY"));
        assertTrue(names.contains("DRAGON"));
        assertTrue(names.contains("COLORLESS"));
    }
}
