package ar.edu.utn.frc.tup.piii.engine.model;

/**
 * Immutable value type representing an attack action with a name and base damage value.
 * FR-003.
 *
 * <p>Rules enforced by the compact constructor:
 * <ul>
 *   <li>{@code name} must not be null or blank.</li>
 *   <li>{@code baseDamage} must be >= 0. Zero is legal (effect-only attacks).</li>
 * </ul>
 * </p>
 */
public record Attack(String name, int baseDamage) {

    private static final int MIN_BASE_DAMAGE = 0;

    public Attack {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Attack name must not be blank");
        }
        if (baseDamage < MIN_BASE_DAMAGE) {
            throw new IllegalArgumentException("baseDamage must be >= 0");
        }
    }
}
