package ar.edu.utn.frc.tup.piii.engine.pipeline.abilities;

import ar.edu.utn.frc.tup.piii.engine.model.AbilityEffect;
import ar.edu.utn.frc.tup.piii.engine.model.BattlePokemonState;
import ar.edu.utn.frc.tup.piii.engine.model.Card;
import ar.edu.utn.frc.tup.piii.engine.model.EnergyCard;
import ar.edu.utn.frc.tup.piii.engine.model.UseAbilityAction;
import ar.edu.utn.frc.tup.piii.engine.session.MatchSession;
import ar.edu.utn.frc.tup.piii.engine.session.PlayerRuntime;
import java.util.ArrayList;
import java.util.List;

/**
 * Strategy for the Energy Grace ability:
 * Knock out Milotic, then attach 3 basic Energy from discard to 1 non-EX Pokémon.
 */
public final class EnergyGraceStrategy implements AbilityEffect {

    @Override
    public void apply(MatchSession session, UseAbilityAction action) {
        final int playerIndex = session.getActivePlayerIndex();
        final PlayerRuntime runtime = session.getPlayerRuntime(playerIndex);
        
        final boolean wasActive = action.source().equals(runtime.getActivePokemon());
        
        // Knock out Milotic (1 prize card)
        session.getKnockoutHandler().onKnockout(action.source(), 1);
        
        if (wasActive && !runtime.getBench().isEmpty()) {
            session.setAwaitingPromotion(playerIndex);
            session.getTurnManager().interruptMainPhase();
        }
        
        // Find target Pokémon (must be non-EX)
        BattlePokemonState targetPokemon = null;
        if (action.targetIndex() == null || action.targetIndex() == -1) {
            targetPokemon = runtime.getActivePokemon();
        } else if (action.targetIndex() >= 0 && action.targetIndex() < runtime.getBench().getAll().size()) {
            targetPokemon = runtime.getBench().getAll().get(action.targetIndex());
        }
        
        if (targetPokemon != null && !targetPokemon.isEx()) {
            // Find up to 3 basic energies in discard
            final List<Card> basicEnergies = new ArrayList<>();
            for (Card card : runtime.getDiscardPile().getCards()) {
                if (card instanceof EnergyCard ec && ec.isBasic()) {
                    basicEnergies.add(card);
                    if (basicEnergies.size() == 3) {
                        break;
                    }
                }
            }
            
            // Attach them
            for (Card card : basicEnergies) {
                runtime.getDiscardPile().remove(card);
                targetPokemon.attachEnergy((EnergyCard) card);
            }
        }
    }
}
