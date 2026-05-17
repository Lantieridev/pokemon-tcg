package ar.edu.utn.frc.tup.piii.engine.statuseffect;

import ar.edu.utn.frc.tup.piii.engine.model.ActivePokemonState;
import ar.edu.utn.frc.tup.piii.engine.model.CoinFlipper;
import ar.edu.utn.frc.tup.piii.engine.model.StatusEffectType;

/**
 * Strategy for the CONFUNDIDO (confused) status condition.
 * Does not block attack or retreat. Confusion is resolved at attack time by
 * StatusEffectManager.onAttackAttempt(), not during the between-turns phase. FR-010.
 */
public class ConfusedEffect implements StatusEffect {

    @Override
    public StatusEffectType getType() {
        return StatusEffectType.CONFUNDIDO;
    }

    @Override
    public boolean isRotationSlot() {
        return true;
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
        return false;
    }
}
