package ar.edu.utn.frc.tup.piii.engine.pipeline;

import ar.edu.utn.frc.tup.piii.engine.model.PokemonToolEffectId;
import ar.edu.utn.frc.tup.piii.engine.model.TrainerCard;

/**
 * Step 2.5 — applies passive Pokémon Tool modifiers to the attack context BEFORE
 * damage calculation. Runs after {@link PreDamageEffectsStep} and before
 * {@link DamageCalculationStep} so that tool bonuses are folded in the correct order
 * mandated by the XY1 rulebook (§3):
 *
 * <ul>
 *   <li><b>Muscle Band (xy1-121)</b>: +20 damage before Weakness/Resistance
 *       → added as an <em>attacker</em> modifier.</li>
 *   <li><b>Hard Charm (xy1-119)</b>: −20 damage after Weakness/Resistance (minimum 0)
 *       → added as a <em>defender</em> modifier.</li>
 * </ul>
 *
 * <p>The step is a no-op when neither Pokémon carries a tool with a known effect.</p>
 */
public final class PokemonToolStep implements AttackPipelineStep {

    private final TrainerEffectResolver resolver;

    public PokemonToolStep(final TrainerEffectResolver resolver) {
        this.resolver = resolver;
    }

    @Override
    public void process(final AttackContext ctx, final Runnable next) {
        applyAttackerTool(ctx);
        applyDefenderTool(ctx);
        next.run();
    }

    private void applyAttackerTool(final AttackContext ctx) {
        ctx.getAttacker().getAttachedTool().ifPresent(tool -> {
            var effect = resolver.resolveTool(tool.getToolEffectId());
            if (effect != null) {
                effect.apply(ctx, true);
            }
        });
    }

    private void applyDefenderTool(final AttackContext ctx) {
        ctx.getDefender().getAttachedTool().ifPresent(tool -> {
            var effect = resolver.resolveTool(tool.getToolEffectId());
            if (effect != null) {
                effect.apply(ctx, false);
            }
        });
    }
}
