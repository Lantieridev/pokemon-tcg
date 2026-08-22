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
        final List<Boolean> wildcard = buildWildcardFlags(energyCards);

        int colorlessRequired = 0;
        for (final PokemonType req : required) {
            if (req == PokemonType.COLORLESS) {
                colorlessRequired++;
                continue;
            }
            if (!consumeMatchingEnergy(pool, wildcard, req)) {
                return false;
            }
        }
        return pool.size() >= colorlessRequired;
    }

    private List<Boolean> buildWildcardFlags(final List<EnergyCard> energyCards) {
        final List<Boolean> wildcard = new java.util.ArrayList<>();
        for (final EnergyCard ec : energyCards) {
            for (int i = 0; i < ec.getEnergyCount(); i++) {
                wildcard.add(ec.isProvidesAllTypes());
            }
        }
        return wildcard;
    }

    /**
     * Tries to satisfy a colored energy requirement, preferring an exact-type match before
     * falling back to a wildcard (Colorless-providing) energy. Mutates {@code pool}/{@code wildcard}
     * by removing the consumed energy on success.
     */
    private boolean consumeMatchingEnergy(final List<PokemonType> pool, final List<Boolean> wildcard,
            final PokemonType req) {
        return removeFirstMatch(pool, wildcard, req, false) || removeFirstMatch(pool, wildcard, req, true);
    }

    private boolean removeFirstMatch(final List<PokemonType> pool, final List<Boolean> wildcard,
            final PokemonType req, final boolean matchWildcard) {
        for (int i = 0; i < pool.size(); i++) {
            final boolean isWildcard = wildcard.get(i);
            final boolean matches = matchWildcard ? isWildcard : (!isWildcard && pool.get(i) == req);
            if (matches) {
                pool.remove(i);
                wildcard.remove(i);
                return true;
            }
        }
        return false;
    }
}
