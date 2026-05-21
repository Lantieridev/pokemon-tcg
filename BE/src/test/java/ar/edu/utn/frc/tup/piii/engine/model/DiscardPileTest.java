package ar.edu.utn.frc.tup.piii.engine.model;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DiscardPileTest {

    private Card card(final String id) {
        return new PokemonCard.Builder(id, "Card" + id, 60, PokemonType.WATER)
                .evolutionStage(EvolutionStage.BASIC)
                .build();
    }

    @Test
    void shouldStartEmpty() {
        assertEquals(0, new DiscardPile().size());
    }

    @Test
    void addShouldIncrementSize() {
        final DiscardPile pile = new DiscardPile();
        pile.add(card("c1"));
        assertEquals(1, pile.size());
    }

    @Test
    void addAllShouldAddMultipleCards() {
        final DiscardPile pile = new DiscardPile();
        pile.addAll(List.of(card("c1"), card("c2"), card("c3")));
        assertEquals(3, pile.size());
    }

    @Test
    void addAllThenAddSingleShouldAccumulateCorrectly() {
        final DiscardPile pile = new DiscardPile();
        pile.addAll(List.of(card("c1"), card("c2")));
        pile.add(card("c3"));
        assertEquals(3, pile.size());
    }

    @Test
    void getCardsShouldReturnAllDiscardedInOrder() {
        final DiscardPile pile = new DiscardPile();
        pile.add(card("c1"));
        pile.add(card("c2"));
        final List<Card> cards = pile.getCards();
        assertEquals("c1", cards.get(0).getCardId());
        assertEquals("c2", cards.get(1).getCardId());
    }

    @Test
    void getCardsShouldReturnUnmodifiableView() {
        final DiscardPile pile = new DiscardPile();
        assertThrows(UnsupportedOperationException.class, () -> pile.getCards().add(card("x")));
    }

    @Test
    void addShouldThrowWhenNull() {
        assertThrows(NullPointerException.class, () -> new DiscardPile().add(null));
    }

    @Test
    void addAllShouldThrowWhenNull() {
        assertThrows(NullPointerException.class, () -> new DiscardPile().addAll(null));
    }
}
