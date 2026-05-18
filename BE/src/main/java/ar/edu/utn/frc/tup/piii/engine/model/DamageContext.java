package ar.edu.utn.frc.tup.piii.engine.model;

import java.util.List;

/**
 * Immutable context object that bundles all inputs required for a single damage
 * calculation pass. Passed to {@code DamageCalculator.calculate()}. FR-005.
 *
 * <p>Both modifier lists are defensively copied via {@link List#copyOf} so that
 * external mutations do not affect in-flight calculations.</p>
 *
 * <p>Empty modifier lists ({@code List.of()}) are valid inputs; they produce a
 * no-op fold.</p>
 */
public record DamageContext(
        BattlePokemonState attacker,
        BattlePokemonState defender,
        Attack attack,
        List<DamageModifier> attackerModifiers,
        List<DamageModifier> defenderModifiers) {

    public DamageContext {
        if (attacker == null || defender == null || attack == null) {
            throw new IllegalArgumentException(
                    "attacker, defender, and attack must not be null");
        }
        if (attackerModifiers == null || defenderModifiers == null) {
            throw new IllegalArgumentException(
                    "modifier lists must not be null; use List.of() if empty");
        }
        attackerModifiers = List.copyOf(attackerModifiers);
        defenderModifiers = List.copyOf(defenderModifiers);
    }
}
