package ar.edu.utn.frc.tup.piii.engine.pipeline;

/**
 * A single step in the attack resolution pipeline. Each implementation encapsulates
 * one phase of the §3 rulebook sequence and must call {@code next.run()} to continue
 * down the chain, or omit it to halt processing early.
 */
@FunctionalInterface
public interface AttackPipelineStep {

    /**
     * Processes this step.
     *
     * @param ctx  the shared, mutable attack context (never null)
     * @param next invoke to pass control to the next step; omit to halt the chain
     */
    void process(AttackContext ctx, Runnable next);
}
