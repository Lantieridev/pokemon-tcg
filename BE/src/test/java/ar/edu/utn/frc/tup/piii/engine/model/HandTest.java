package ar.edu.utn.frc.tup.piii.engine.model;

import ar.edu.utn.frc.tup.piii.engine.exception.CardNotInHandException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class HandTest {

    private PokemonCard basicPokemon(final String id) {
        return new PokemonCard.Builder(id, "Bulbasaur", 45, PokemonType.GRASS)
                .evolutionStage(EvolutionStage.BASIC)
                .build();
    }

    private PokemonCard stage1Pokemon(final String id) {
        return new PokemonCard.Builder(id, "Ivysaur", 80, PokemonType.GRASS)
                .evolutionStage(EvolutionStage.STAGE_1)
                .evolvesFrom("Bulbasaur")
                .build();
    }

    private Card trainerCard(final String id) {
        return new PokemonCard.Builder(id, "TrainerX", 0, PokemonType.COLORLESS)
                .evolutionStage(EvolutionStage.BASIC)
                .build();
    }

    @Test
    void shouldStartEmpty() {
        assertEquals(0, new Hand().size());
    }

    @Test
    void addCardShouldIncrementSize() {
        final Hand hand = new Hand();
        hand.addCard(basicPokemon("p1"));
        assertEquals(1, hand.size());
    }

    @Test
    void addCardsShouldAddAllCards() {
        final Hand hand = new Hand();
        hand.addCards(List.of(basicPokemon("p1"), basicPokemon("p2"), basicPokemon("p3")));
        assertEquals(3, hand.size());
    }

    @Test
    void removeCardShouldReturnAndRemoveByCardId() {
        final Hand hand = new Hand();
        final Card card = basicPokemon("p1");
        hand.addCard(card);
        final Card removed = hand.removeCard("p1");
        assertSame(card, removed);
        assertEquals(0, hand.size());
    }

    @Test
    void removeCardShouldThrowWhenCardNotPresent() {
        final Hand hand = new Hand();
        hand.addCard(basicPokemon("p1"));
        assertThrows(CardNotInHandException.class, () -> hand.removeCard("nonexistent"));
    }

    @Test
    void removeCardShouldThrowWhenHandIsEmpty() {
        assertThrows(CardNotInHandException.class, () -> new Hand().removeCard("p1"));
    }

    @Test
    void hasBasicPokemonShouldReturnTrueWhenBasicPresent() {
        final Hand hand = new Hand();
        hand.addCard(basicPokemon("p1"));
        assertTrue(hand.hasBasicPokemon());
    }

    @Test
    void hasBasicPokemonShouldReturnFalseWhenOnlyStage1() {
        final Hand hand = new Hand();
        hand.addCard(stage1Pokemon("p1"));
        assertFalse(hand.hasBasicPokemon());
    }

    @Test
    void hasBasicPokemonShouldReturnFalseWhenEmpty() {
        assertFalse(new Hand().hasBasicPokemon());
    }

    @Test
    void getCardsShouldReturnDefensiveCopy() {
        final Hand hand = new Hand();
        hand.addCard(basicPokemon("p1"));
        final List<Card> copy = hand.getCards();
        assertThrows(UnsupportedOperationException.class, () -> copy.add(basicPokemon("p2")));
        assertEquals(1, hand.size());
    }

    @Test
    void addCardShouldThrowWhenNull() {
        assertThrows(NullPointerException.class, () -> new Hand().addCard(null));
    }

    @Test
    void addCardsShouldThrowWhenNull() {
        assertThrows(NullPointerException.class, () -> new Hand().addCards(null));
    }

    @Test
    void removeAllShouldReturnAllCardsAndClearHand() {
        final Hand hand = new Hand();
        hand.addCards(List.of(basicPokemon("a"), basicPokemon("b"), stage1Pokemon("c")));
        final List<Card> removed = hand.removeAll();
        assertEquals(3, removed.size());
        assertEquals(0, hand.size());
    }

    @Test
    void removeAllOnEmptyHandShouldReturnEmptyList() {
        assertTrue(new Hand().removeAll().isEmpty());
    }

    @Test
    void shouldSupportMultipleCardsWithDifferentIds() {
        final Hand hand = new Hand();
        hand.addCards(List.of(basicPokemon("a"), basicPokemon("b"), stage1Pokemon("c")));
        hand.removeCard("b");
        assertEquals(2, hand.size());
        assertTrue(hand.hasBasicPokemon());
    }
}
