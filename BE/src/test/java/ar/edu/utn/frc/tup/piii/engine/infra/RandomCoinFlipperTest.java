package ar.edu.utn.frc.tup.piii.engine.infra;

import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests for RandomCoinFlipper. FR-002, FR-018.
 */
class RandomCoinFlipperTest {

    @Test
    void shouldReturnDeterministicSequenceWithSeededRandom() {
        // new Random(42L).nextBoolean() x5 = true, false, true, false, false
        RandomCoinFlipper flipper = new RandomCoinFlipper(new Random(42L));
        assertEquals(true, flipper.flip());
        assertEquals(false, flipper.flip());
        assertEquals(true, flipper.flip());
        assertEquals(false, flipper.flip());
        assertEquals(false, flipper.flip());
    }

    @Test
    void shouldReturnBooleanFromFlip() {
        RandomCoinFlipper flipper = new RandomCoinFlipper();
        // No exception, returns a boolean value
        assertDoesNotThrow(flipper::flip);
    }
}
