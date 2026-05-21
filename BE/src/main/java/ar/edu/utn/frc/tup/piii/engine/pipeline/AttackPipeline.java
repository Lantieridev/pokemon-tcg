package ar.edu.utn.frc.tup.piii.engine.pipeline;

import java.util.List;
import java.util.Objects;

/**
 * Assembles and executes a Chain-of-Responsibility pipeline of {@link AttackPipelineStep}s.
 *
 * <p>Each step receives the shared {@link AttackContext} and a {@code next} runnable.
 * Calling {@code next.run()} passes control to the following step; omitting it halts
 * the chain (e.g., when the attack is blocked). RNF-04.</p>
 */
public final class AttackPipeline {

    private final List<AttackPipelineStep> steps;

    /**
     * @param steps ordered list of pipeline steps (never null; copied defensively)
     */
    public AttackPipeline(final List<AttackPipelineStep> steps) {
        this.steps = List.copyOf(Objects.requireNonNull(steps, "steps must not be null"));
    }

    /**
     * Runs all steps in order against the given context.
     *
     * @param ctx the shared mutable context (never null)
     */
    public void execute(final AttackContext ctx) {
        Objects.requireNonNull(ctx, "ctx must not be null");
        executeFrom(ctx, 0);
    }

    private void executeFrom(final AttackContext ctx, final int index) {
        if (index >= steps.size()) {
            return;
        }
        steps.get(index).process(ctx, () -> executeFrom(ctx, index + 1));
    }
}
