package ar.edu.utn.frc.tup.piii.engine.model;

import java.util.List;

/**
 * Immutable value type representing an attack action with a name, base damage value,
 * the list of energy types required to use it, and an optional effect-text descriptor. FR-003.
 *
 * <p>Rules enforced by the compact constructor:
 * <ul>
 *   <li>{@code name} must not be null or blank.</li>
 *   <li>{@code baseDamage} must be >= 0. Zero is legal (effect-only attacks).</li>
 *   <li>{@code requiredEnergies} must not be null. An empty list means the attack is free.</li>
 * </ul>
 * </p>
 */
public record Attack(String name, int baseDamage, List<PokemonType> requiredEnergies, String effectText) {

    private static final int MIN_BASE_DAMAGE = 0;

    public Attack {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Attack name must not be blank");
        }
        if (baseDamage < MIN_BASE_DAMAGE) {
            throw new IllegalArgumentException("baseDamage must be >= 0");
        }
        if (requiredEnergies == null) {
            throw new IllegalArgumentException("requiredEnergies must not be null");
        }
        requiredEnergies = List.copyOf(requiredEnergies);
        if (effectText == null) {
            effectText = "";
        }
    }

    public Attack(String name, int baseDamage, List<PokemonType> requiredEnergies) {
        this(name, baseDamage, requiredEnergies, "");
    }
}
