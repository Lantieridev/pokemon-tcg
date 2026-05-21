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
        next.run();
    }
}
