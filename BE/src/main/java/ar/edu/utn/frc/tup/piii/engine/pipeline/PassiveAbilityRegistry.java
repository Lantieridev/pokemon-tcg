package ar.edu.utn.frc.tup.piii.engine.pipeline;

import ar.edu.utn.frc.tup.piii.engine.model.AbilityEffectId;
import ar.edu.utn.frc.tup.piii.engine.model.BattlePokemonState;
import ar.edu.utn.frc.tup.piii.engine.model.PokemonType;
import ar.edu.utn.frc.tup.piii.engine.model.StatusEffectType;
import ar.edu.utn.frc.tup.piii.engine.session.PlayerRuntime;

/**
 * Registry/Strategy class to apply passive abilities (like Sweet Veil and Fur Coat)
 * without hardcoding details in status effect or damage steps.
 */
public final class PassiveAbilityRegistry {

    private PassiveAbilityRegistry() {}

    /**
     * Applies any passive damage reduction or modification.
     * E.g., Fur Coat reduces damage from attacks by 20.
     *
     * @param baseDamage the calculated damage
     * @param ctx        the attack context
     * @return the modified damage (never negative)
     */
    public static int modifyIncomingDamage(final int baseDamage, final AttackContext ctx) {
        int damage = baseDamage;
        if (ctx.getDefender() != null && hasAbility(ctx.getDefender(), AbilityEffectId.FUR_COAT)) {
            damage = (int) (Math.ceil((damage / 2.0) / 10.0) * 10);
        }
        if (ctx.getDefender() != null && hasAbility(ctx.getDefender(), AbilityEffectId.INTIMIDATING_MANE)) {
            if (ctx.getAttacker() != null && ctx.getAttacker().getEvolutionStage() == ar.edu.utn.frc.tup.piii.engine.model.EvolutionStage.BASIC) {
                damage = 0;
            }
        }
        return damage;
    }

    /**
     * Checks if a special condition should be prevented by any passive abilities in play.
     * E.g., Sweet Veil prevents all Special Conditions if the target has Fairy Energy.
     *
     * @param type    the status effect type being applied
     * @param runtime the player runtime of the target
     * @return true if the effect is blocked/prevented
     */
    public static boolean preventStatusEffect(final StatusEffectType type, final PlayerRuntime runtime) {
        if (runtime == null) {
            return false;
        }
        if (runtime.hasAbility(AbilityEffectId.SWEET_VEIL)) {
            final BattlePokemonState active = runtime.getActivePokemon();
            if (active != null) {
                final boolean hasFairyEnergy = active.getAttachedEnergyCards().stream()
                        .anyMatch(ec -> ec.getEnergyType() == PokemonType.FAIRY || ec.isProvidesAllTypes());
                if (hasFairyEnergy) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean hasAbility(final BattlePokemonState pokemon, final AbilityEffectId abilityId) {
        return pokemon != null && pokemon.getAbilities().stream().anyMatch(a -> a.effectId() == abilityId);
    }
}
