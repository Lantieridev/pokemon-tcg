package ar.edu.utn.frc.tup.piii.engine.pipeline;

/**
 * Step 3.5 — checks if the attack has been cancelled by any preceding effect.
 *
 * <p>If {@link AttackContext#isAttackBlocked()} is true, this step halts the chain
 * (omits calling {@code next.run()}). This enforces RF-05, The Attack Steps §3,
 * where certain abilities or effects can cancel an attack before damage is applied.</p>
 */
public final class AttackCancellationStep implements AttackPipelineStep {

    @Override
    public void process(final AttackContext ctx, final Runnable next) {
        if (ctx.isAttackBlocked()) {
            return;
        }
        next.run();
    }
}
