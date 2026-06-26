package ar.edu.utn.frc.tup.piii.engine.statuseffect;

import ar.edu.utn.frc.tup.piii.engine.model.ActivePokemonState;
import ar.edu.utn.frc.tup.piii.engine.model.CoinFlipper;
import ar.edu.utn.frc.tup.piii.engine.model.StatusEffectType;

/**
 * Strategy for the PRECISION_BAJA status condition (Smokescreen / Sand-Attack).
 * Does not block attack or retreat. When attacking, a coin is flipped; if tails, the attack fails.
 * Removed at the end of the affected player's turn (returns true in processBetweenTurns).
 */
public class PrecisionBajaEffect implements StatusEffect {

    @Override
    public StatusEffectType getType() {
        return StatusEffectType.PRECISION_BAJA;
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
        return true;
    }
}
