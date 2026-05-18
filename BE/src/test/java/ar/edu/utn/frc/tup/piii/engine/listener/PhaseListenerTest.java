package ar.edu.utn.frc.tup.piii.engine.listener;

import ar.edu.utn.frc.tup.piii.engine.model.DrawPhase;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verify;

/**
 * Tests for the PhaseListener functional interface. FR-011.
 */
class PhaseListenerTest {

    @Test
    void shouldBeInvokableAsLambda() {
        PhaseListener listener = event -> { };
        assertNotNull(listener);
    }

    @Test
    void shouldReceiveEventPassedToOn() {
        PhaseListener mock = Mockito.mock(PhaseListener.class);
        PhaseEvent event = new PhaseEvent.PhaseEntered(0, new DrawPhase());
        mock.on(event);
        verify(mock).on(event);
    }
}
