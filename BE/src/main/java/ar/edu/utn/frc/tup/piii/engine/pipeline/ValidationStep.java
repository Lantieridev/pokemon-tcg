package ar.edu.utn.frc.tup.piii.engine.pipeline;

import ar.edu.utn.frc.tup.piii.engine.model.AttackModifierResult;
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
        if (!hasRequiredEnergy(ctx.getAttack().requiredEnergies(), ctx.getAttacker().getAttachedEnergies())) {
            ctx.setAttackBlocked(true);
            return;
        }
        final AttackModifierResult result = ctx.getAttackerStatusManager().onAttackAttempt(ctx.getAttacker());
        if (result instanceof AttackModifierResult.ConfusionFailed) {
            ctx.setAttackBlocked(true);
            return;
        }
        next.run();
    }

    private boolean hasRequiredEnergy(final List<PokemonType> required, final List<PokemonType> attached) {
        final Map<PokemonType, Long> reqByType = required.stream()
                .filter(t -> t != PokemonType.COLORLESS)
                .collect(Collectors.groupingBy(t -> t, Collectors.counting()));

        final Map<PokemonType, Long> attByType = attached.stream()
                .collect(Collectors.groupingBy(t -> t, Collectors.counting()));

        long usedForSpecific = 0L;
        for (final Map.Entry<PokemonType, Long> entry : reqByType.entrySet()) {
            final long available = attByType.getOrDefault(entry.getKey(), 0L);
            if (available < entry.getValue()) {
                return false;
            }
            usedForSpecific += entry.getValue();
        }

        final long colorlessNeeded = required.stream()
                .filter(t -> t == PokemonType.COLORLESS)
                .count();
        return (attached.size() - usedForSpecific) >= colorlessNeeded;
    }
}
