package ar.edu.utn.frc.tup.piii.engine.pipeline;

/**
 * Step 4 — places the damage counters computed by {@link DamageCalculationStep} onto
 * the defending Pokémon.
 */
public final class DamageApplicationStep implements AttackPipelineStep {

    @Override
    public void process(final AttackContext ctx, final Runnable next) {
        final int counters = ctx.getDamageResult().damageCountersToPlace();
        ctx.getDefender().addDamageCounters(counters);

        // Record damage in statistics trackers
        final int damageAmount = counters * 10;
        if (damageAmount > 0) {
            if (ctx.getAttackerStats() != null && ctx.getAttacker() != null) {
                ctx.getAttackerStats().addDamageDealt(ctx.getAttacker().getCardId(), damageAmount);
            }
            if (ctx.getDefenderStats() != null && ctx.getDefender() != null) {
                ctx.getDefenderStats().addDamageReceived(ctx.getDefender().getCardId(), damageAmount);
            }
        }

        next.run();
    }
}
