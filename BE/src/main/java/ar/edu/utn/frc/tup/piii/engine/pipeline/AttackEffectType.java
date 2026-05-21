package ar.edu.utn.frc.tup.piii.engine.pipeline;

/**
 * Enumerates every secondary effect an attack may produce after damage resolution (§3 rulebook).
 * Used by {@link AttackEffectResolver} to dispatch the correct handler without switch/instanceof.
 */
public enum AttackEffectType {
    NONE,
    APPLY_POISON,
    APPLY_BURN,
    APPLY_PARALYSIS,
    APPLY_SLEEP,
    APPLY_CONFUSION,
    HEAL_SELF,
    SELF_DAMAGE,
    DISCARD_ENERGY,
    COIN_FLIP_EXTRA_DAMAGE
}
