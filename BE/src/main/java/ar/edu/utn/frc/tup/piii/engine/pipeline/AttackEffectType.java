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
    COIN_FLIP_EXTRA_DAMAGE,
    /**
     * Applies damage to each of the opponent's Benched Pokémon.
     * Weakness and Resistance do not apply to Benched Pokémon (§3 rulebook).
     * Example: Xerneas-EX "Break Through" (xy1-97) — {@code "bench_damage:20"}.
     */
    BENCH_DAMAGE,
    /**
     * Moves energy from the attacker to one of its Benched Pokémon.
     * Example: Yveltal-EX "Y Cyclone" (xy1-79) — {@code "move_energy"}.
     * FR-TODO: requires full runtime access; not yet implemented.
     */
    MOVE_ENERGY,
    /**
     * Forces the opponent to switch their Active Pokémon with a Benched one.
     * Example: Blastoise-EX "Rapid Spin" (xy1-29) — {@code "force_switch"}.
     * FR-TODO: requires full runtime access; not yet implemented.
     */
    COIN_FLIP_POISON,
    COIN_FLIP_BURN,
    COIN_FLIP_PARALYSIS,
    COIN_FLIP_SLEEP,
    COIN_FLIP_CONFUSION,
    DISABLE_ATTACK,
    PREVENT_DAMAGE,
    COIN_FLIP_PREVENT_DAMAGE,
    FORCE_SWITCH,
    COIN_FLIP_SWITCH_SELF,
    HEAL_ANY,
    HEAL_BENCH,
    HEAL_ALL,
    DISCARD_OPPONENT_ENERGY,
    COIN_FLIP_DISCARD_OPPONENT_ENERGY,
    STOKE,
    COMBUSTION_BLAST,
    SCORCHING_FANG,
    BRIGHT_GARDEN,
    EAR_WE_GO,
    CLAIRVOYANT_EYE
}
