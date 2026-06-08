package ar.edu.utn.frc.tup.piii.engine.pipeline;

import java.util.Objects;

/**
 * Step 5 — delegates secondary effects (status conditions, self-heal, energy discard, etc.)
 * to the {@link AttackEffectResolver} after damage counters have been placed.
 */
public final class PostDamageEffectsStep implements AttackPipelineStep {

    private final AttackEffectResolver resolver;

    /**
     * @param resolver the stateless effect resolver (never null)
     */
    public PostDamageEffectsStep(final AttackEffectResolver resolver) {
        this.resolver = Objects.requireNonNull(resolver, "resolver must not be null");
    }

    @Override
    public void process(final AttackContext ctx, final Runnable next) {
        resolver.apply(ctx);
        if (ctx.getDamageResult() != null) {
            ReactiveAbilityHandler.onDamageDealt(ctx, ctx.getDamageResult().finalDamage());
        }
        next.run();
    }
}
