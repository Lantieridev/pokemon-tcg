package ar.edu.utn.frc.tup.piii.engine.pipeline;

import ar.edu.utn.frc.tup.piii.engine.model.AttackModifierResult;
import ar.edu.utn.frc.tup.piii.engine.model.BattlePokemonState;
import ar.edu.utn.frc.tup.piii.engine.model.EnergyCard;
import ar.edu.utn.frc.tup.piii.engine.model.PokemonType;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Step 1 — validates that the attacker has sufficient energy and is allowed to attack
 * given its current status conditions.
 *
 * <p>Energy rule: COLORLESS requirements may be satisfied by any attached energy type,
 * after specific-type requirements have been satisfied first.</p>
 *
 * <p>Halts the chain (does not call {@code next}) when blocked. Sets
 * {@link AttackContext#setAttackBlocked(boolean)} to {@code true} for the caller's inspection.
 * Exceptions from {@code StatusEffectManager.onAttackAttempt()} propagate upward unchanged.</p>
 */
public final class ValidationStep implements AttackPipelineStep {

    @Override
    public void process(final AttackContext ctx, final Runnable next) {
        if (!hasRequiredEnergy(ctx.getAttack().requiredEnergies(), ctx.getAttacker())) {
            ctx.setAttackBlocked(true);
            return;
        }
        final AttackModifierResult result = ctx.getAttackerStatusManager().onAttackAttempt(ctx.getAttacker());
        if (result instanceof AttackModifierResult.ConfusionFailed || result instanceof AttackModifierResult.SmokescreenFailed) {
            ctx.setAttackBlocked(true);
            return;
        }
        next.run();
    }

    private boolean hasRequiredEnergy(final List<PokemonType> required, final BattlePokemonState attacker) {
        final List<EnergyCard> energyCards = attacker.getAttachedEnergyCards();
        final List<PokemonType> pool = new java.util.ArrayList<>(attacker.getAttachedEnergies());
        final List<Boolean> wildcard = new java.util.ArrayList<>();
        
        for (final EnergyCard ec : energyCards) {
            for (int i = 0; i < ec.getEnergyCount(); i++) {
                wildcard.add(ec.isProvidesAllTypes());
            }
        }

        int colorlessRequired = 0;
        for (final PokemonType req : required) {
            if (req == PokemonType.COLORLESS) {
                colorlessRequired++;
                continue;
            }
            boolean satisfied = false;
            for (int i = 0; i < pool.size(); i++) {
                if (!wildcard.get(i) && pool.get(i) == req) {
                    pool.remove(i);
                    wildcard.remove(i);
                    satisfied = true;
                    break;
                }
            }
            if (!satisfied) {
                for (int i = 0; i < pool.size(); i++) {
                    if (wildcard.get(i)) {
                        pool.remove(i);
                        wildcard.remove(i);
                        satisfied = true;
                        break;
                    }
                }
            }
            if (!satisfied) {
                return false;
            }
        }
        return pool.size() >= colorlessRequired;
    }
}
