package ar.edu.utn.frc.tup.piii.engine.pipeline.abilities;

import ar.edu.utn.frc.tup.piii.engine.model.AbilityEffect;
import ar.edu.utn.frc.tup.piii.engine.model.PokemonCard;
import ar.edu.utn.frc.tup.piii.engine.model.UseAbilityAction;
import ar.edu.utn.frc.tup.piii.engine.session.MatchSession;
import ar.edu.utn.frc.tup.piii.engine.session.PlayerRuntime;

/**
 * Strategy for the Upside-Down Evolution ability, searching the deck for a Pokémon card that evolves from the active Pokémon and evolving it.
 */
public final class UpsideDownEvolutionStrategy implements AbilityEffect {

    @Override
    public void apply(MatchSession session, UseAbilityAction action) {
        final int playerIndex = session.getActivePlayerIndex();
        final PlayerRuntime runtime = session.getPlayerRuntime(playerIndex);
        
        final String evolvesFromTarget = action.source().getName();
        
        final PokemonCard evolutionCard = runtime.getDeck().getCards().stream()
                .filter(c -> c instanceof PokemonCard pc 
                        && pc.getEvolvesFrom() != null 
                        && pc.getEvolvesFrom().equalsIgnoreCase(evolvesFromTarget))
                .map(c -> (PokemonCard) c)
                .findFirst().orElse(null);
        
        if (evolutionCard != null) {
            runtime.getDeck().searchAndRemove(c -> c.getCardId().equals(evolutionCard.getCardId()), 1);
            action.source().evolveInto(evolutionCard);
            runtime.getDeck().shuffle();
        }
    }
}
