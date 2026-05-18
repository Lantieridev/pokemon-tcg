package ar.edu.utn.frc.tup.piii.engine.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for MainPhase counters and rules. FR-001, FR-010.
 */
class MainPhaseTest {

    @Test
    void shouldReturnCorrectNameForMainPhase() {
        assertEquals("MAIN", new MainPhase().name());
    }

    @Test
    void shouldAllowFirstEnergyAttachmentInTurn() {
        MainPhase phase = new MainPhase();
        phase.recordEnergyAttached();
        assertEquals(1, phase.getEnergyAttached());
    }

    @Test
    void shouldThrowIllegalStateExceptionWhenEnergyAttachedTwiceInSameTurn() {
        MainPhase phase = new MainPhase();
        phase.recordEnergyAttached();
        assertThrows(IllegalStateException.class, phase::recordEnergyAttached);
    }

    @Test
    void shouldAllowFirstSupporterPlayInTurn() {
        MainPhase phase = new MainPhase();
        phase.recordSupporterPlayed();
        assertTrue(phase.isSupporterPlayed());
    }

    @Test
    void shouldThrowIllegalStateExceptionWhenSupporterPlayedTwiceInSameTurn() {
        MainPhase phase = new MainPhase();
        phase.recordSupporterPlayed();
        assertThrows(IllegalStateException.class, phase::recordSupporterPlayed);
    }

    @Test
    void shouldAllowFirstStadiumPlayInTurn() {
        MainPhase phase = new MainPhase();
        phase.recordStadiumPlayed();
        assertTrue(phase.isStadiumPlayed());
    }

    @Test
    void shouldThrowIllegalStateExceptionWhenStadiumPlayedTwiceInSameTurn() {
        MainPhase phase = new MainPhase();
        phase.recordStadiumPlayed();
        assertThrows(IllegalStateException.class, phase::recordStadiumPlayed);
    }

    @Test
    void shouldAllowFirstRetreatInTurn() {
        MainPhase phase = new MainPhase();
        phase.recordRetreatUsed();
        assertTrue(phase.isRetreatUsed());
    }

    @Test
    void shouldThrowIllegalStateExceptionWhenRetreatUsedTwiceInSameTurn() {
        MainPhase phase = new MainPhase();
        phase.recordRetreatUsed();
        assertThrows(IllegalStateException.class, phase::recordRetreatUsed);
    }

    @Test
    void shouldHaveAllCountersResetOnFreshInstance() {
        MainPhase phase = new MainPhase();
        assertEquals(0, phase.getEnergyAttached());
        assertFalse(phase.isSupporterPlayed());
        assertFalse(phase.isStadiumPlayed());
        assertFalse(phase.isRetreatUsed());
    }
}
