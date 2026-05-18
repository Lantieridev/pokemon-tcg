package ar.edu.utn.frc.tup.piii.engine.model;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for TrainerType enum. FR-003.
 */
class TrainerTypeTest {

    @Test
    void shouldHaveExactlyThreeConstantsWhenValuesIsCalled() {
        assertEquals(3, TrainerType.values().length);
    }

    @Test
    void shouldContainItemSupporterAndStadiumWhenValuesIsCalled() {
        Set<String> names = Arrays.stream(TrainerType.values())
                .map(Enum::name)
                .collect(Collectors.toSet());
        assertTrue(names.contains("ITEM"));
        assertTrue(names.contains("SUPPORTER"));
        assertTrue(names.contains("STADIUM"));
    }
}
