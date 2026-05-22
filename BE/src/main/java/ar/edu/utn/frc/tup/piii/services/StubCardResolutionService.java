package ar.edu.utn.frc.tup.piii.services;

import ar.edu.utn.frc.tup.piii.engine.model.Attack;
import ar.edu.utn.frc.tup.piii.engine.model.Card;
import ar.edu.utn.frc.tup.piii.engine.model.EnergyCard;
import ar.edu.utn.frc.tup.piii.engine.model.PokemonCard;
import ar.edu.utn.frc.tup.piii.engine.model.PokemonType;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Stub implementation of {@link CardResolutionService} used before Module 4 provides
 * real database-backed resolution. Returns a minimal, valid 60-card deck: two Basics
 * (Charmander) and 58 Fire Energy cards, which satisfies all engine rules.
 *
 * <p>Activated automatically when no other {@link CardResolutionService} bean is present
 * (via {@link ConditionalOnMissingBean}).</p>
 */
@Service
@ConditionalOnMissingBean(value = CardResolutionService.class, ignored = StubCardResolutionService.class)
public final class StubCardResolutionService implements CardResolutionService {

    private static final int DECK_SIZE = 60;
    private static final int BASIC_POKEMON_COUNT = 2;
    private static final int HP = 50;
    private static final int ATTACK_DAMAGE = 10;
    private static final int RETREAT_COST = 1;

    @Override
    public List<Card> resolveCards(final Long deckId) {
        final List<Card> deck = new ArrayList<>();

        // Two Charmander (Basic) to satisfy the "at least one Basic" rule
        for (int i = 0; i < BASIC_POKEMON_COUNT; i++) {
            deck.add(buildCharmander("stub-charmander-" + i));
        }

        // Fill remainder with Basic Fire Energy
        for (int i = 0; i < DECK_SIZE - BASIC_POKEMON_COUNT; i++) {
            deck.add(new EnergyCard("stub-fire-energy-" + i, "Fire Energy", PokemonType.FIRE, true));
        }

        return deck;
    }

    private PokemonCard buildCharmander(final String cardId) {
        return new PokemonCard.Builder(cardId, "Charmander", HP, PokemonType.FIRE)
                .weaknessType(PokemonType.WATER)
                .retreatCost(RETREAT_COST)
                .attacks(List.of(
                        new Attack("Scratch", List.of(PokemonType.COLORLESS), ATTACK_DAMAGE, "")))
                .build();
    }
}
