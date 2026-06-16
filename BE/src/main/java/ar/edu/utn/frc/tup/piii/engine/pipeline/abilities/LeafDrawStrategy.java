package ar.edu.utn.frc.tup.piii.engine.pipeline.abilities;

import ar.edu.utn.frc.tup.piii.engine.model.AbilityEffect;
import ar.edu.utn.frc.tup.piii.engine.model.Card;
import ar.edu.utn.frc.tup.piii.engine.model.EnergyCard;
import ar.edu.utn.frc.tup.piii.engine.model.PokemonType;
import ar.edu.utn.frc.tup.piii.engine.model.UseAbilityAction;
import ar.edu.utn.frc.tup.piii.engine.session.MatchSession;
import ar.edu.utn.frc.tup.piii.engine.session.PlayerRuntime;
import java.util.List;

/**
 * Strategy for the Leaf Draw ability, discarding a Grass energy from hand to draw 3 cards.
 */
public final class LeafDrawStrategy implements AbilityEffect {

    @Override
    public void apply(MatchSession session, UseAbilityAction action) {
        final int playerIndex = session.getActivePlayerIndex();
        final PlayerRuntime runtime = session.getPlayerRuntime(playerIndex);
        
        final Card energyCard = runtime.getHand().getCards().stream()
                .filter(c -> c instanceof EnergyCard ec && ec.getEnergyType() == PokemonType.GRASS)
                .findFirst().orElse(null);
        
        if (energyCard != null) {
            runtime.getHand().removeCard(energyCard.getCardId());
            runtime.getDiscardPile().add(energyCard);
            
            final List<Card> drawn = runtime.getDeck().drawMultiple(3);
            for (Card card : drawn) {
                runtime.getHand().addCard(card);
            }
        }
    }
}
