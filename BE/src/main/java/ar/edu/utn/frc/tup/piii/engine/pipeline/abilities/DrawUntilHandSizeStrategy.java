package ar.edu.utn.frc.tup.piii.engine.pipeline.abilities;

import ar.edu.utn.frc.tup.piii.engine.model.AbilityEffect;
import ar.edu.utn.frc.tup.piii.engine.model.Card;
import ar.edu.utn.frc.tup.piii.engine.model.UseAbilityAction;
import ar.edu.utn.frc.tup.piii.engine.session.MatchSession;
import ar.edu.utn.frc.tup.piii.engine.session.PlayerRuntime;

import java.util.List;

/**
 * Strategy to draw cards from the deck until the player's hand reaches a target size.
 */
public final class DrawUntilHandSizeStrategy implements AbilityEffect {

    private final int targetHandSize;

    public DrawUntilHandSizeStrategy(int targetHandSize) {
        this.targetHandSize = targetHandSize;
    }

    @Override
    public void apply(MatchSession session, UseAbilityAction action) {
        final int playerIndex = session.getActivePlayerIndex();
        final PlayerRuntime runtime = session.getPlayerRuntime(playerIndex);
        final int currentHandSize = runtime.getHand().size();
        if (currentHandSize < targetHandSize) {
            final int cardsToDraw = targetHandSize - currentHandSize;
            List<Card> drawn = runtime.getDeck().drawMultiple(cardsToDraw);
            for (Card card : drawn) {
                runtime.getHand().addCard(card);
            }
        }
    }
}
