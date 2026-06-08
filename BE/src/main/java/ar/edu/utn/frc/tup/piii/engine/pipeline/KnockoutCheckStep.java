package ar.edu.utn.frc.tup.piii.engine.pipeline;

import ar.edu.utn.frc.tup.piii.engine.model.BattlePokemonState;

/**
 * Step 6 — checks whether the defending Pokémon has been knocked out and, if so,
 * invokes the {@link ar.edu.utn.frc.tup.piii.engine.listener.KnockoutHandler} with the
 * correct prize count (2 for EX Pokémon, 1 for standard).
 */
public final class KnockoutCheckStep implements AttackPipelineStep {

    private static final int DAMAGE_PER_COUNTER = 10;
    private static final int STANDARD_PRIZES = 1;
    private static final int EX_PRIZES = 2;

    @Override
    public void process(final AttackContext ctx, final Runnable next) {
        final BattlePokemonState defender = ctx.getDefender();
        if (isKnockedOut(defender)) {
            final int prizes = defender.isEx() ? EX_PRIZES : STANDARD_PRIZES;
            ctx.getKnockoutHandler().onKnockout(defender, prizes);
            ReactiveAbilityHandler.onKnockout(ctx, defender);
        }
        next.run();
    }

    private boolean isKnockedOut(final BattlePokemonState state) {
        return state.getDamageCounters() * DAMAGE_PER_COUNTER >= state.getMaxHp();
    }
}
