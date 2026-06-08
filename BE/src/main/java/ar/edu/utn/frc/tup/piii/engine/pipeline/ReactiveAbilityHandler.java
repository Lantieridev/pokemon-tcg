package ar.edu.utn.frc.tup.piii.engine.pipeline;

import ar.edu.utn.frc.tup.piii.engine.model.AbilityEffectId;
import ar.edu.utn.frc.tup.piii.engine.model.BattlePokemonState;

/**
 * Handles reactive or event-triggered abilities, such as Spiky Shield
 * and Destiny Burst, during damage application and knockout resolution.
 */
public final class ReactiveAbilityHandler {

    private ReactiveAbilityHandler() {}

    /**
     * Called after damage is successfully applied to the defender.
     * E.g., Chesnaught's Spiky Shield places 3 damage counters on the attacker.
     *
     * @param ctx          the attack context
     * @param damageDealt  the amount of damage dealt (before scaling to counters)
     */
    public static void onDamageDealt(final AttackContext ctx, final int damageDealt) {
        if (damageDealt <= 0) {
            return;
        }
        final BattlePokemonState defender = ctx.getDefender();
        final BattlePokemonState attacker = ctx.getAttacker();
        if (defender != null && attacker != null && hasAbility(defender, AbilityEffectId.SPIKY_SHIELD)) {
            // Put 3 damage counters (30 damage) on the attacker
            attacker.addDamageCounters(3);
        }
    }

    /**
     * Called when a Pokémon is fainted/knocked out.
     * E.g., Voltorb's Destiny Burst flips a coin; on heads, places 5 damage counters on the attacker.
     *
     * @param ctx      the attack context
     * @param deceased the Pokémon that was knocked out
     */
    public static void onKnockout(final AttackContext ctx, final BattlePokemonState deceased) {
        final BattlePokemonState attacker = ctx.getAttacker();
        if (deceased != null && attacker != null && hasAbility(deceased, AbilityEffectId.DESTINY_BURST)) {
            // Flip a coin. If heads, put 5 damage counters (50 damage) on the attacker.
            if (ctx.getCoinFlipper() != null && ctx.getCoinFlipper().flip()) {
                attacker.addDamageCounters(5);
            }
        }
    }

    private static boolean hasAbility(final BattlePokemonState pokemon, final AbilityEffectId abilityId) {
        return pokemon != null && pokemon.getAbilities().stream().anyMatch(a -> a.effectId() == abilityId);
    }
}
