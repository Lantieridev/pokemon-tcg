package ar.edu.utn.frc.tup.piii.engine.model;

/**
 * A single modifier applied to a running damage value during §3 calculation.
 * Implementations are caller-supplied lambdas built from card-text effects
 * (e.g., {@code dmg -> dmg + 30}). FR-004.
 */
@FunctionalInterface
public interface DamageModifier {

    /**
     * Applies this modifier to {@code currentDamage} and returns the new value.
     *
     * @param currentDamage the damage value before this modifier
     * @return the damage value after this modifier
     */
    int apply(int currentDamage);
}
