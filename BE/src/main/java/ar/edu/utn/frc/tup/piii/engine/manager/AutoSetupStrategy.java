package ar.edu.utn.frc.tup.piii.engine.manager;

import ar.edu.utn.frc.tup.piii.engine.model.Card;
import ar.edu.utn.frc.tup.piii.engine.model.EvolutionStage;
import ar.edu.utn.frc.tup.piii.engine.model.PokemonCard;
import ar.edu.utn.frc.tup.piii.engine.model.SetupStrategy;

import java.util.ArrayList;
import java.util.List;

/**
 * Auto-pilot {@link SetupStrategy} used by the server during match creation.
 * Picks the first Basic Pokémon as the Active and benches all remaining Basics
 * (up to the bench limit of 5). Always accepts bonus draws from opponent Mulligans.
 *
 * <p>Pure POJO — zero Spring imports.</p>
 */
public final class AutoSetupStrategy implements SetupStrategy {

    private static final int MAX_BENCH_SIZE = 5;

    @Override
    public String chooseActiveCardId(final List<Card> hand) {
        return hand.stream()
                .filter(this::isBasicPokemon)
                .findFirst()
                .map(Card::getCardId)
                .orElseThrow(() -> new IllegalStateException(
                        "No Basic Pokémon in hand — SetupManager should have prevented this"));
    }

    @Override
    public List<String> chooseBenchCardIds(final List<Card> hand) {
        final List<String> ids = new ArrayList<>();
        for (final Card card : hand) {
            if (ids.size() >= MAX_BENCH_SIZE) {
                break;
            }
            if (isBasicPokemon(card)) {
                ids.add(card.getCardId());
            }
        }
        return ids;
    }

    @Override
    public boolean acceptBonusDraws(final int opponentMulliganCount) {
        return true;
    }

    private boolean isBasicPokemon(final Card card) {
        return card instanceof PokemonCard pokemon
                && pokemon.getEvolutionStage() == EvolutionStage.BASIC;
    }
}
