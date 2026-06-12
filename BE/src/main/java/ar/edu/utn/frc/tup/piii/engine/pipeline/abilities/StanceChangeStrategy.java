package ar.edu.utn.frc.tup.piii.engine.pipeline.abilities;

import ar.edu.utn.frc.tup.piii.engine.model.AbilityEffect;
import ar.edu.utn.frc.tup.piii.engine.model.BattlePokemonState;
import ar.edu.utn.frc.tup.piii.engine.model.Card;
import ar.edu.utn.frc.tup.piii.engine.model.InPlayPokemon;
import ar.edu.utn.frc.tup.piii.engine.model.PokemonCard;
import ar.edu.utn.frc.tup.piii.engine.model.UseAbilityAction;
import ar.edu.utn.frc.tup.piii.engine.session.MatchSession;
import ar.edu.utn.frc.tup.piii.engine.session.PlayerRuntime;

/**
 * Strategy for the Stance Change ability, allowing swapping the active Pokémon in play with another Pokémon card from hand that shares the same name.
 */
public final class StanceChangeStrategy implements AbilityEffect {

    @Override
    public void apply(MatchSession session, UseAbilityAction action) {
        final int playerIndex = session.getActivePlayerIndex();
        final PlayerRuntime runtime = session.getPlayerRuntime(playerIndex);
        
        final String sourceName = action.source().getName();
        final String sourceCardId = action.source().getCardId();
        
        Card aegisHand = runtime.getHand().getCards().stream()
                .filter(c -> c.getName().equalsIgnoreCase(sourceName) && !c.getCardId().equals(sourceCardId))
                .findFirst().orElse(null);
        
        if (aegisHand instanceof PokemonCard newAegis) {
            runtime.getHand().removeCard(newAegis.getCardId());
            
            final BattlePokemonState source = action.source();
            final Card oldBaseCard = source.getBaseCard();
            
            if (source instanceof InPlayPokemon inPlay) {
                inPlay.swapCard(newAegis);
            }
            
            runtime.getHand().addCard(oldBaseCard);
        }
    }
}
