package ar.edu.utn.frc.tup.piii.engine.pipeline;

import ar.edu.utn.frc.tup.piii.engine.model.PokemonType;
import ar.edu.utn.frc.tup.piii.engine.model.TrainerCard;

/**
 * Pipeline step that applies active Stadium card effects to the attack context
 * before damage is calculated. Runs between {@link PokemonToolStep} and
 * {@link DamageCalculationStep}.
 *
 * <p>XY1 Stadiums handled:</p>
 * <ul>
 *   <li><b>Shadow Circle (xy1-126):</b> Darkness Pokémon have no Weakness for this attack.
 *       Sets {@link AttackContext#setWeaknessSuppressed(boolean)} when the defender is a
 *       {@link PokemonType#DARKNESS} type and Shadow Circle is in play.</li>
 * </ul>
 *
 * <p>Fairy Garden (xy1-117) affects retreat cost and is validated in
 * {@code RuleValidator.validateRetreat()} — not in the attack pipeline.</p>
 */
public final class StadiumEffectStep implements AttackPipelineStep {

    private final TrainerEffectResolver resolver;

    public StadiumEffectStep(final TrainerEffectResolver resolver) {
        this.resolver = resolver;
    }

    @Override
    public void process(final AttackContext ctx, final Runnable next) {
        final TrainerCard stadium = ctx.getActiveStadium();
        if (stadium != null) {
            var effect = resolver.resolveStadium(stadium.getCardId());
            if (effect != null) {
                effect.apply(ctx);
            }
        }
        next.run();
    }
}
