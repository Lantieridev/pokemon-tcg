package ar.edu.utn.frc.tup.piii.engine.pipeline.abilities;

import ar.edu.utn.frc.tup.piii.engine.model.AbilityEffect;
import ar.edu.utn.frc.tup.piii.engine.model.BattlePokemonState;
import ar.edu.utn.frc.tup.piii.engine.model.Card;
import ar.edu.utn.frc.tup.piii.engine.model.EnergyCard;
import ar.edu.utn.frc.tup.piii.engine.model.PokemonCard;
import ar.edu.utn.frc.tup.piii.engine.model.TrainerCard;
import ar.edu.utn.frc.tup.piii.engine.model.UseAbilityAction;
import ar.edu.utn.frc.tup.piii.engine.session.MatchSession;
import ar.edu.utn.frc.tup.piii.engine.session.PlayerRuntime;
import java.util.List;
import java.util.Optional;

/**
 * Strategy for the Big Jump ability (Lopunny).
 * Returns Lopunny and all attached cards (energies, tools, and underlyings) to the player's hand,
 * and removes it from its current board slot (Active or Bench).
 */
public final class BigJumpStrategy implements AbilityEffect {

    @Override
    public void apply(MatchSession session, UseAbilityAction action) {
        final int playerIndex = session.getActivePlayerIndex();
        final PlayerRuntime runtime = session.getPlayerRuntime(playerIndex);
        final BattlePokemonState source = action.source();

        if (source == null) {
            return;
        }

        // Return everything to the hand
        final Card baseCard = source.getBaseCard();
        if (baseCard != null) {
            runtime.getHand().addCard(baseCard);
        }

        final List<PokemonCard> underlying = source.getUnderlyingCards();
        if (underlying != null) {
            underlying.forEach(runtime.getHand()::addCard);
        }

        final List<EnergyCard> attachedEnergies = source.getAttachedEnergyCards();
        if (attachedEnergies != null) {
            attachedEnergies.forEach(runtime.getHand()::addCard);
        }

        final Optional<TrainerCard> attachedTool = source.getAttachedTool();
        attachedTool.ifPresent(runtime.getHand()::addCard);

        if (runtime.getActivePokemon() == source) {
            runtime.clearActivePokemon();
            if (!runtime.getBench().isEmpty()) {
                session.setAwaitingPromotion(playerIndex);
                if (session.getTurnManager() != null) {
                    session.getTurnManager().interruptMainPhase();
                }
            }
        } else {
            final int index = runtime.getBench().getAll().indexOf(source);
            if (index != -1) {
                runtime.getBench().remove(index);
            }
        }
        runtime.removePokemonFromPlay(source);
    }
}
