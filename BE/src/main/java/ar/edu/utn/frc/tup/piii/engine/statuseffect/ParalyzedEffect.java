package ar.edu.utn.frc.tup.piii.engine.statuseffect;

import ar.edu.utn.frc.tup.piii.engine.model.ActivePokemonState;
import ar.edu.utn.frc.tup.piii.engine.model.CoinFlipper;
import ar.edu.utn.frc.tup.piii.engine.model.StatusEffectType;

/**
 * Strategy for the PARALIZADO (paralyzed) status condition.
 * Blocks attack and retreat. Always removed after the between-turns phase
 * without flipping a coin or adding damage counters. FR-009.
 */
public class ParalyzedEffect implements StatusEffect {

    @Override
    public StatusEffectType getType() {
        return StatusEffectType.PARALIZADO;
    }

    @Override
    public boolean isRotationSlot() {
        return true;
    }

    @Override
    public boolean blocksAttack() {
        return true;
    }

    @Override
    public boolean blocksRetreat() {
        return true;
    }

    @Override
    public boolean processBetweenTurns(final ActivePokemonState state, final CoinFlipper flipper) {
        return true;
    }
}
