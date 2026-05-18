package ar.edu.utn.frc.tup.piii.engine.model;

/**
 * Immutable result of a damage calculation. Holds the final damage value (after all
 * modifiers, weakness, resistance, and floor) and the corresponding number of damage
 * counters to place. FR-006.
 *
 * <p>Invariant: {@code damageCountersToPlace == finalDamage / 10} (integer division).
 * This is enforced by {@link ar.edu.utn.frc.tup.piii.engine.manager.DamageCalculator}
 * and validated by the compact constructor below.</p>
 */
public record DamageResult(int finalDamage, int damageCountersToPlace) {

    private static final int DAMAGE_PER_COUNTER = 10;

    public DamageResult {
        if (finalDamage < 0) {
            throw new IllegalArgumentException("finalDamage must be >= 0");
        }
        if (damageCountersToPlace < 0) {
            throw new IllegalArgumentException("damageCountersToPlace must be >= 0");
        }
        if (damageCountersToPlace != finalDamage / DAMAGE_PER_COUNTER) {
            throw new IllegalArgumentException(
                    "damageCountersToPlace must equal finalDamage / 10; "
                    + "got finalDamage=" + finalDamage
                    + ", damageCountersToPlace=" + damageCountersToPlace);
        }
    }
}
