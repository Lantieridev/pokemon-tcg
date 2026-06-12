package ar.edu.utn.frc.tup.piii.engine.model;

import ar.edu.utn.frc.tup.piii.engine.exception.DeckEmptyException;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class DeckTest {

    private static final int STANDARD_SIZE = 60;

    private List<Card> buildCards(final int count) {
        final List<Card> cards = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            final int idx = i;
            cards.add(new PokemonCard.Builder("id-" + idx, "Pokemon" + idx, 60, PokemonType.FIRE)
                    .evolutionStage(EvolutionStage.BASIC)
                    .build());
        }
        return cards;
    }

    @Test
    void shouldThrowWhenConstructedWithNull() {
        assertThrows(NullPointerException.class, () -> new Deck(null));
    }

    @Test
    void shouldThrowWhenConstructedWithEmptyList() {
        assertThrows(IllegalArgumentException.class, () -> new Deck(List.of()));
    }

    @Test
    void sizeShouldReflectCardCount() {
        final Deck deck = new Deck(buildCards(STANDARD_SIZE));
        assertEquals(STANDARD_SIZE, deck.size());
    }

    @Test
    void isEmptyShouldReturnFalseWhenCardsRemain() {
        assertFalse(new Deck(buildCards(1)).isEmpty());
    }

    @Test
    void isEmptyShouldReturnTrueAfterAllCardsDrawn() {
        final Deck deck = new Deck(buildCards(1));
        deck.draw();
        assertTrue(deck.isEmpty());
    }

    @Test
    void drawShouldReturnTopCard() {
        final List<Card> cards = buildCards(3);
        final Deck deck = new Deck(cards);
        final Card drawn = deck.draw();
        assertEquals("id-0", drawn.getCardId());
        assertEquals(2, deck.size());
    }

    @Test
    void drawShouldThrowDeckEmptyExceptionWhenNoCards() {
        final Deck deck = new Deck(buildCards(1));
        deck.draw();
        assertThrows(DeckEmptyException.class, deck::draw);
    }

    @Test
    void drawMultipleShouldReturnNCards() {
        final Deck deck = new Deck(buildCards(STANDARD_SIZE));
        final List<Card> drawn = deck.drawMultiple(7);
        assertEquals(7, drawn.size());
        assertEquals(53, deck.size());
    }

    @Test
    void drawMultipleShouldReturnAvailableCardsWhenInsufficient() {
        final Deck deck = new Deck(buildCards(3));
        final List<Card> drawn = deck.drawMultiple(4);
        assertEquals(3, drawn.size());
        assertTrue(deck.isEmpty());
    }

    @Test
    void drawMultipleWithZeroShouldReturnEmptyList() {
        final Deck deck = new Deck(buildCards(5));
        assertTrue(deck.drawMultiple(0).isEmpty());
        assertEquals(5, deck.size());
    }

    @Test
    void peekShouldReturnTopCardWithoutRemoving() {
        final List<Card> cards = buildCards(3);
        final Deck deck = new Deck(cards);
        final Card top = deck.peek();
        assertEquals("id-0", top.getCardId());
        assertEquals(3, deck.size());
    }

    @Test
    void peekShouldThrowWhenEmpty() {
        final Deck deck = new Deck(buildCards(1));
        deck.draw();
        assertThrows(DeckEmptyException.class, deck::peek);
    }

    @Test
    void shuffleShouldChangeCardOrder() {
        final List<Card> original = buildCards(STANDARD_SIZE);
        final Deck deck = new Deck(new ArrayList<>(original));
        deck.shuffle(new Random(0L));
        final Card newTop = deck.peek();
        // With seed 0 on 60 cards, it's astronomically unlikely to remain id-0
        assertNotEquals("id-0", newTop.getCardId());
    }

    @Test
    void shuffleShouldPreserveCardCount() {
        final Deck deck = new Deck(buildCards(STANDARD_SIZE));
        deck.shuffle(new Random(42L));
        assertEquals(STANDARD_SIZE, deck.size());
    }

    @Test
    void defaultShuffleShouldNotThrow() {
        final Deck deck = new Deck(buildCards(STANDARD_SIZE));
        assertDoesNotThrow(() -> deck.shuffle());
    }

    @Test
    void addCardsShouldAppendToEnd() {
        final Deck deck = new Deck(buildCards(3));
        final List<Card> extras = buildCards(2);
        extras.get(0).getCardId(); // just reference
        deck.addCards(extras);
        assertEquals(5, deck.size());
        // first 3 remain at top
        assertEquals("id-0", deck.draw().getCardId());
        assertEquals("id-1", deck.draw().getCardId());
        assertEquals("id-2", deck.draw().getCardId());
    }

    @Test
    void addCardsShouldThrowWhenNull() {
        assertThrows(NullPointerException.class, () -> new Deck(buildCards(3)).addCards(null));
    }

    @Test
    void drawMultipleShouldReturnCardsInDrawOrder() {
        final List<Card> cards = buildCards(5);
        final Deck deck = new Deck(cards);
        final List<Card> drawn = deck.drawMultiple(3);
        assertEquals("id-0", drawn.get(0).getCardId());
        assertEquals("id-1", drawn.get(1).getCardId());
        assertEquals("id-2", drawn.get(2).getCardId());
    }
}
