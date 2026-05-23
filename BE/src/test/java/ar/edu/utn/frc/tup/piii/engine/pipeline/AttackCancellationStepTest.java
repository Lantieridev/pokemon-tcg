package ar.edu.utn.frc.tup.piii.engine.pipeline;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AttackCancellationStepTest {

    private final AttackCancellationStep step = new AttackCancellationStep();

    @Mock
    private AttackContext ctx;

    @Mock
    private Runnable next;

    @Test
    void shouldCallNextWhenAttackIsNotBlocked() {
        when(ctx.isAttackBlocked()).thenReturn(false);

        step.process(ctx, next);

        verify(next).run();
    }

    @Test
    void shouldHaltPipelineWhenAttackIsBlocked() {
        when(ctx.isAttackBlocked()).thenReturn(true);

        step.process(ctx, next);

        verify(next, never()).run();
    }
}
