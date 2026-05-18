package ar.edu.utn.frc.tup.piii.engine.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests for ValidationResult sealed type. FR-005.
 */
class ValidationResultTest {

    @Test
    void shouldHaveNoFieldsWhenValidIsCreated() {
        ValidationResult result = new ValidationResult.Valid();

        assertInstanceOf(ValidationResult.class, result);
    }

    @Test
    void shouldReturnReasonWhenInvalidIsCreated() {
        ValidationResult result = new ValidationResult.Invalid("energy_already_attached");

        assertEquals("energy_already_attached", ((ValidationResult.Invalid) result).reason());
    }
}
