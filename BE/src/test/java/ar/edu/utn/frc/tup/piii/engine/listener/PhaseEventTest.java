package ar.edu.utn.frc.tup.piii.engine.listener;

import ar.edu.utn.frc.tup.piii.engine.model.DrawPhase;
import ar.edu.utn.frc.tup.piii.engine.model.TurnPhase;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

/**
 * Tests for the PhaseEvent sealed hierarchy. FR-012.
 */
class PhaseEventTest {

    @Test
    void shouldHaveCorrectPlayerIndexAndPhaseInPhaseEnteredEvent() {
        PhaseEvent event = new PhaseEvent.PhaseEntered(0, new DrawPhase());
        assertEquals(0, event.playerIndex());
        assertInstanceOf(DrawPhase.class, event.phase());
    }

    @Test
    void shouldHaveCorrectPlayerIndexAndPhaseInPhaseExitedEvent() {
        PhaseEvent event = new PhaseEvent.PhaseExited(1, new DrawPhase());
        assertEquals(1, event.playerIndex());
        assertInstanceOf(DrawPhase.class, event.phase());
    }

    @Test
    void shouldHaveCorrectPlayerIndexAndPhaseInTurnStartedEvent() {
        PhaseEvent event = new PhaseEvent.TurnStarted(0, new DrawPhase());
        assertEquals(0, event.playerIndex());
        assertInstanceOf(TurnPhase.class, event.phase());
    }

    @Test
    void shouldHaveCorrectPlayerIndexAndPhaseInTurnEndedEvent() {
        PhaseEvent event = new PhaseEvent.TurnEnded(1, new DrawPhase());
        assertEquals(1, event.playerIndex());
        assertInstanceOf(TurnPhase.class, event.phase());
    }
}
