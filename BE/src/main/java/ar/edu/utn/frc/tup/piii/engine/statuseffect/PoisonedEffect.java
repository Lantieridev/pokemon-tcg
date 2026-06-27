package ar.edu.utn.frc.tup.piii.engine.statuseffect;

import ar.edu.utn.frc.tup.piii.engine.model.ActivePokemonState;
import ar.edu.utn.frc.tup.piii.engine.model.CoinFlipper;
import ar.edu.utn.frc.tup.piii.engine.model.StatusEffectType;

/**
 * Strategy for the ENVENENADO (poisoned) status condition.
 * Adds one damage counter between turns; persists indefinitely. FR-006.
 */
public class PoisonedEffect implements StatusEffect {

    private final int damageCounters;

    public PoisonedEffect() {
        this(1);
    }

    public PoisonedEffect(final int damageCounters) {
        this.damageCounters = damageCounters;
    }

    @Override
    public StatusEffectType getType() {
        return StatusEffectType.ENVENENADO;
    }

    @Override
    public boolean isRotationSlot() {
        return false;
    }

    @Override
    public boolean blocksAttack() {
        return false;
    }

    @Override
    public boolean blocksRetreat() {
        return false;
    }

    @Override
    public boolean processBetweenTurns(final ActivePokemonState state, final CoinFlipper flipper) {
        state.addDamageCounters(damageCounters);
        return false;
    }
}
