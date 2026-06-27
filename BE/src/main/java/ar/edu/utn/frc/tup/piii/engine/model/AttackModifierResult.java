package ar.edu.utn.frc.tup.piii.engine.model;

/**
 * Sealed result type returned by StatusEffectManager.onAttackAttempt().
 * Pattern-matching on this type is exhaustive without a default branch. FR-004.
 */
public sealed interface AttackModifierResult permits AttackModifierResult.Proceed,
        AttackModifierResult.ConfusionFailed, AttackModifierResult.SmokescreenFailed {

    /**
     * The attack proceeds normally — no blocking status applied.
     */
    record Proceed() implements AttackModifierResult {
    }

    /**
     * The Pokémon hurt itself in confusion. The caller must apply selfDamageCounters
     * to the attacker before resolving the turn.
     *
     * @param selfDamageCounters number of damage counters placed on the attacker
     */
    record ConfusionFailed(int selfDamageCounters) implements AttackModifierResult {
    }

    /**
     * The attack failed due to low precision (Smokescreen/Sand-Attack).
     */
    record SmokescreenFailed() implements AttackModifierResult {
    }
}
