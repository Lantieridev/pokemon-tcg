package ar.edu.utn.frc.tup.piii.engine.pipeline.abilities;

import ar.edu.utn.frc.tup.piii.engine.model.AbilityEffect;
import ar.edu.utn.frc.tup.piii.engine.model.UseAbilityAction;
import ar.edu.utn.frc.tup.piii.engine.session.MatchSession;
import ar.edu.utn.frc.tup.piii.engine.session.PlayerRuntime;

/**
 * Strategy for the Drive Off ability, forcing the opponent to switch their active Pokémon with a benched one.
 */
public final class DriveOffStrategy implements AbilityEffect {

    @Override
    public void apply(MatchSession session, UseAbilityAction action) {
        final int opponentIndex = 1 - session.getActivePlayerIndex();
        final PlayerRuntime opponent = session.getPlayerRuntime(opponentIndex);
        if (!opponent.getBench().isEmpty()) {
            session.setAwaitingPromotion(opponentIndex);
            session.getTurnManager().interruptMainPhase();
        }
    }
}
