package ar.edu.utn.frc.tup.piii.services.deck;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DeckBuilderValidatorTest {

    private DeckBuilderValidator validator;

    @BeforeEach
    void setUp() {
        validator = new DeckBuilderValidator();
    }

    // --- helpers ---

    private static DeckEntry pokemon(final String name, final String subtype, final int qty) {
        return new DeckEntry("p-" + name, name, "Pokémon", subtype, null, qty);
    }

    private static DeckEntry basicEnergy(final String name, final int qty) {
        return new DeckEntry("e-" + name, name, "Energy", "Basic Energy", null, qty);
    }

    private static DeckEntry specialEnergy(final String name, final int qty) {
        return new DeckEntry("se-" + name, name, "Energy", "Special Energy", null, qty);
    }

    private static DeckEntry trainer(final String name, final int qty) {
        return new DeckEntry("t-" + name, name, "Trainer", "Item", null, qty);
    }

    private static DeckEntry aceSpec(final String name, final int qty) {
        return new DeckEntry("as-" + name, name, "Trainer", "Item",
                List.of("ACE SPEC rule: You can't have more than 1 ACE SPEC card in your deck."), qty);
    }

    private static List<DeckEntry> validDeck() {
        final List<DeckEntry> entries = new ArrayList<>();
        entries.add(pokemon("Bulbasaur", "Basic", 4));
        entries.add(pokemon("Ivysaur", "Stage 1", 3));
        entries.add(pokemon("Venusaur", "Stage 2", 2));
        entries.add(trainer("Professor's Letter", 4));
        entries.add(trainer("Potion", 4));
        entries.add(trainer("Revive", 4));
        entries.add(trainer("Switch", 4));
        entries.add(trainer("Ultra Ball", 4));
        entries.add(trainer("Rare Candy", 4));
        entries.add(basicEnergy("Grass Energy", 4));
        entries.add(basicEnergy("Fire Energy", 4));
        entries.add(basicEnergy("Water Energy", 4));
        entries.add(basicEnergy("Lightning Energy", 4));
        entries.add(basicEnergy("Psychic Energy", 4));
        // total = 4+3+2+4+4+4+4+4+4+4+4+4+4+4 = 57... let me fix
        entries.add(basicEnergy("Fighting Energy", 3));
        entries.add(basicEnergy("Darkness Energy", 4));
        // total = 4+3+2+4+4+4+4+4+4+4+4+4+4+4+3+4 = 60
        return entries;
    }

    // === RULE 1: Total = 60 ===

    @Test
    void shouldPassWhenDeckHasExactly60Cards() {
        assertDoesNotThrow(() -> validator.validate(validDeck(), ar.edu.utn.frc.tup.piii.engine.model.DeckStatus.VALID));
    }

    @Test
    void shouldRejectDeckWithFewerThan60Cards() {
        final List<DeckEntry> deck = new ArrayList<>(validDeck());
        // remove last entry and add one with qty=1 fewer
        deck.remove(deck.size() - 1);
        deck.add(basicEnergy("Fighting Energy", 2)); // -1
        final InvalidDeckException ex = assertThrows(
                InvalidDeckException.class, () -> validator.validate(deck, ar.edu.utn.frc.tup.piii.engine.model.DeckStatus.VALID));
        assertTrue(ex.getMessage().contains("60"), "Error must mention 60");
    }

    @Test
    void shouldRejectDeckWithMoreThan60Cards() {
        final List<DeckEntry> deck = new ArrayList<>(validDeck());
        deck.remove(deck.size() - 1);
        deck.add(basicEnergy("Fighting Energy", 5)); // +2
        final InvalidDeckException ex = assertThrows(
                InvalidDeckException.class, () -> validator.validate(deck, ar.edu.utn.frc.tup.piii.engine.model.DeckStatus.VALID));
        assertTrue(ex.getMessage().contains("60"), "Error must mention 60");
    }

    @Test
    void shouldRejectEmptyDeck() {
        final InvalidDeckException ex = assertThrows(
                InvalidDeckException.class, () -> validator.validate(Collections.emptyList(), ar.edu.utn.frc.tup.piii.engine.model.DeckStatus.VALID));
        assertTrue(ex.getMessage().contains("60"), "Error must mention 60");
    }

    // === RULE 2: At least 1 Basic Pokémon ===

    @Test
    void shouldRejectDeckWithNoBasicPokemon() {
        final List<DeckEntry> deck = new ArrayList<>();
        deck.add(pokemon("Ivysaur", "Stage 1", 4));
        deck.add(basicEnergy("Grass Energy", 56));
        final InvalidDeckException ex = assertThrows(
                InvalidDeckException.class, () -> validator.validate(deck, ar.edu.utn.frc.tup.piii.engine.model.DeckStatus.VALID));
        assertTrue(ex.getMessage().toLowerCase().contains("basic"),
                "Error must mention 'basic'");
    }

    @Test
    void shouldPassWhenDeckHasOneBasicPokemon() {
        // validDeck() has Bulbasaur (Basic) so this is covered by shouldPassWhenDeckHasExactly60Cards
        // testing a deck with just 1 basic pokemon slot
        final List<DeckEntry> deck = new ArrayList<>();
        deck.add(pokemon("Bulbasaur", "Basic", 1));
        deck.add(trainer("Potion", 4));
        deck.add(trainer("Professor's Letter", 4));
        deck.add(trainer("Revive", 4));
        deck.add(trainer("Switch", 4));
        deck.add(trainer("Ultra Ball", 4));
        deck.add(trainer("Rare Candy", 4));
        deck.add(trainer("Super Potion", 4));
        deck.add(basicEnergy("Grass Energy", 4));
        deck.add(basicEnergy("Fire Energy", 4));
        deck.add(basicEnergy("Water Energy", 4));
        deck.add(basicEnergy("Lightning Energy", 4));
        deck.add(basicEnergy("Psychic Energy", 4));
        deck.add(basicEnergy("Fighting Energy", 4));
        deck.add(basicEnergy("Darkness Energy", 4));
        deck.add(basicEnergy("Fairy Energy", 3));
        // 1+4+4+4+4+4+4+4+4+4+4+4+4+4+4+3 = 60
        assertDoesNotThrow(() -> validator.validate(deck, ar.edu.utn.frc.tup.piii.engine.model.DeckStatus.VALID));
    }

    @Test
    void shouldRejectDeckWithOnlyTrainersAndEnergy() {
        final List<DeckEntry> deck = new ArrayList<>();
        deck.add(trainer("VS Seeker", 4));
        deck.add(trainer("Sycamore", 4));
        deck.add(trainer("N", 4));
        deck.add(trainer("Lysandre", 4));
        deck.add(trainer("Skyla", 4));
        deck.add(trainer("Shauna", 4));
        deck.add(trainer("Tierno", 4));
        deck.add(trainer("Hau", 4));
        deck.add(basicEnergy("Grass Energy", 4));
        deck.add(basicEnergy("Fire Energy", 4));
        deck.add(basicEnergy("Water Energy", 4));
        deck.add(basicEnergy("Lightning Energy", 4));
        deck.add(basicEnergy("Psychic Energy", 4));
        deck.add(basicEnergy("Fighting Energy", 4));
        deck.add(basicEnergy("Darkness Energy", 4));
        // 15 * 4 = 60
        final InvalidDeckException ex = assertThrows(
                InvalidDeckException.class, () -> validator.validate(deck, ar.edu.utn.frc.tup.piii.engine.model.DeckStatus.VALID));
        assertTrue(ex.getMessage().toLowerCase().contains("basic"));
    }

    // === RULE 3: Max 4 copies per card name (except Basic Energy) ===

    @Test
    void shouldRejectMoreThan4CopiesOfSameNonEnergyCard() {
        final List<DeckEntry> deck = new ArrayList<>(validDeck());
        // replace last entry with an over-limit trainer
        deck.remove(deck.size() - 1);
        deck.add(trainer("Potion", 4)); // "Potion" already has 4 → total 8
        // total still 60? let me recalculate... validDeck has 60 cards total.
        // removing last (Fighting Energy qty=3) and adding Potion qty=4 => 61 cards
        // so also fails for size. Let's build a dedicated deck.
        final List<DeckEntry> over4 = new ArrayList<>();
        over4.add(pokemon("Bulbasaur", "Basic", 4));
        over4.add(new DeckEntry("t-Potion-a", "Potion", "Trainer", "Item", null, 4));
        over4.add(new DeckEntry("t-Potion-b", "Potion", "Trainer", "Item", null, 2)); // same name, 6 total
        over4.add(basicEnergy("Grass Energy", 50));
        // total = 4+4+2+50 = 60
        final InvalidDeckException ex = assertThrows(
                InvalidDeckException.class, () -> validator.validate(over4, ar.edu.utn.frc.tup.piii.engine.model.DeckStatus.VALID));
        assertTrue(ex.getMessage().contains("4") || ex.getMessage().toLowerCase().contains("copies"),
                "Error must mention copy limit");
    }

    @Test
    void shouldAllowMoreThan4CopiesOfBasicEnergy() {
        final List<DeckEntry> deck = new ArrayList<>();
        deck.add(pokemon("Bulbasaur", "Basic", 4));
        deck.add(basicEnergy("Grass Energy", 56)); // 60 total, more than 4 basic energy
        assertDoesNotThrow(() -> validator.validate(deck, ar.edu.utn.frc.tup.piii.engine.model.DeckStatus.VALID));
    }

    @Test
    void shouldRejectMoreThan4CopiesOfSpecialEnergy() {
        final List<DeckEntry> deck = new ArrayList<>();
        deck.add(pokemon("Bulbasaur", "Basic", 4));
        deck.add(specialEnergy("Double Colorless Energy", 5));
        deck.add(basicEnergy("Grass Energy", 51));
        // total = 4+5+51 = 60
        final InvalidDeckException ex = assertThrows(
                InvalidDeckException.class, () -> validator.validate(deck, ar.edu.utn.frc.tup.piii.engine.model.DeckStatus.VALID));
        assertTrue(ex.getMessage().contains("4") || ex.getMessage().toLowerCase().contains("copies"));
    }

    @Test
    void shouldCountCopiesAcrossMultipleEntriesWithSameName() {
        final List<DeckEntry> deck = new ArrayList<>();
        deck.add(pokemon("Bulbasaur", "Basic", 4));
        // 3 separate entries for "Switch" — same name — 2+2+1 = 5 > 4
        deck.add(new DeckEntry("sw-1", "Switch", "Trainer", "Item", null, 2));
        deck.add(new DeckEntry("sw-2", "Switch", "Trainer", "Item", null, 2));
        deck.add(new DeckEntry("sw-3", "Switch", "Trainer", "Item", null, 1));
        deck.add(basicEnergy("Grass Energy", 51));
        // total = 4+2+2+1+51 = 60
        final InvalidDeckException ex = assertThrows(
                InvalidDeckException.class, () -> validator.validate(deck, ar.edu.utn.frc.tup.piii.engine.model.DeckStatus.VALID));
        assertTrue(ex.getMessage().contains("4") || ex.getMessage().toLowerCase().contains("copies"));
    }

    @Test
    void shouldAllowExactly4CopiesOfSameCard() {
        final List<DeckEntry> deck = new ArrayList<>();
        deck.add(pokemon("Bulbasaur", "Basic", 4));
        deck.add(new DeckEntry("sw-1", "Switch", "Trainer", "Item", null, 2));
        deck.add(new DeckEntry("sw-2", "Switch", "Trainer", "Item", null, 2)); // exactly 4
        deck.add(basicEnergy("Grass Energy", 52));
        // total = 4+2+2+52 = 60
        assertDoesNotThrow(() -> validator.validate(deck, ar.edu.utn.frc.tup.piii.engine.model.DeckStatus.VALID));
    }

    // === RULE 4: Max 1 ACE SPEC total ===

    @Test
    void shouldRejectMoreThan1AceSpecInDeck() {
        final List<DeckEntry> deck = new ArrayList<>();
        deck.add(pokemon("Bulbasaur", "Basic", 4));
        deck.add(aceSpec("Computer Search", 1));
        deck.add(aceSpec("Dowsing Machine", 1)); // 2 ACE SPEC → invalid
        deck.add(basicEnergy("Grass Energy", 54));
        // total = 4+1+1+54 = 60
        final InvalidDeckException ex = assertThrows(
                InvalidDeckException.class, () -> validator.validate(deck, ar.edu.utn.frc.tup.piii.engine.model.DeckStatus.VALID));
        assertTrue(ex.getMessage().toLowerCase().contains("ace spec")
                || ex.getMessage().toLowerCase().contains("ace"),
                "Error must mention ACE SPEC");
    }

    @Test
    void shouldAllowExactly1AceSpec() {
        final List<DeckEntry> deck = new ArrayList<>();
        deck.add(pokemon("Bulbasaur", "Basic", 4));
        deck.add(aceSpec("Computer Search", 1));
        deck.add(trainer("Potion", 4));
        deck.add(trainer("Switch", 4));
        deck.add(trainer("Ultra Ball", 4));
        deck.add(trainer("Revive", 4));
        deck.add(trainer("Professor's Letter", 4));
        deck.add(trainer("Rare Candy", 4));
        deck.add(basicEnergy("Grass Energy", 4));
        deck.add(basicEnergy("Fire Energy", 4));
        deck.add(basicEnergy("Water Energy", 4));
        deck.add(basicEnergy("Lightning Energy", 4));
        deck.add(basicEnergy("Psychic Energy", 4));
        deck.add(basicEnergy("Fighting Energy", 4));
        deck.add(basicEnergy("Darkness Energy", 4));
        deck.add(basicEnergy("Fairy Energy", 3));
        // 4+1+4+4+4+4+4+4+4+4+4+4+4+4+4+3 = 60
        assertDoesNotThrow(() -> validator.validate(deck, ar.edu.utn.frc.tup.piii.engine.model.DeckStatus.VALID));
    }

    @Test
    void shouldRejectAceSpecWithQuantityGreaterThan1() {
        final List<DeckEntry> deck = new ArrayList<>();
        deck.add(pokemon("Bulbasaur", "Basic", 4));
        deck.add(aceSpec("Computer Search", 2)); // 2 copies of 1 ACE SPEC card
        deck.add(basicEnergy("Grass Energy", 54));
        // total = 4+2+54 = 60
        final InvalidDeckException ex = assertThrows(
                InvalidDeckException.class, () -> validator.validate(deck, ar.edu.utn.frc.tup.piii.engine.model.DeckStatus.VALID));
        assertTrue(ex.getMessage().toLowerCase().contains("ace spec")
                || ex.getMessage().toLowerCase().contains("ace"));
    }

    @Test
    void shouldPassDeckWithNoAceSpec() {
        final List<DeckEntry> deck = new ArrayList<>();
        deck.add(pokemon("Bulbasaur", "Basic", 4));
        deck.add(trainer("Potion", 4));
        deck.add(trainer("Switch", 4));
        deck.add(basicEnergy("Grass Energy", 48));
        // total = 4+4+4+48 = 60
        assertDoesNotThrow(() -> validator.validate(deck, ar.edu.utn.frc.tup.piii.engine.model.DeckStatus.VALID));
    }

    @Test
    void shouldAllowBypassingMaxCopiesLimitForAdmin2AndBenka() {
        final List<DeckEntry> deck = new ArrayList<>();
        deck.add(pokemon("Bulbasaur", "Basic", 4));
        deck.add(trainer("Potion", 10)); // 10 copies of Potion (> 4)
        deck.add(basicEnergy("Grass Energy", 46));
        // total = 4+10+46 = 60
        assertDoesNotThrow(() -> validator.validate(deck, ar.edu.utn.frc.tup.piii.engine.model.DeckStatus.VALID, "admin2"));
        assertDoesNotThrow(() -> validator.validate(deck, ar.edu.utn.frc.tup.piii.engine.model.DeckStatus.VALID, "benka"));
    }

    @Test
    void shouldRejectMoreThan4CopiesForOtherUsers() {
        final List<DeckEntry> deck = new ArrayList<>();
        deck.add(pokemon("Bulbasaur", "Basic", 4));
        deck.add(trainer("Potion", 10)); // 10 copies of Potion (> 4)
        deck.add(basicEnergy("Grass Energy", 46));
        // total = 4+10+46 = 60
        assertThrows(InvalidDeckException.class, () -> validator.validate(deck, ar.edu.utn.frc.tup.piii.engine.model.DeckStatus.VALID, "other-user"));
    }

    @Test
    void shouldAllowBypassingMaxCopiesLimitForSpecialDeckNames() {
        final List<DeckEntry> deck = new ArrayList<>();
        deck.add(pokemon("Bulbasaur", "Basic", 4));
        deck.add(trainer("Potion", 10));
        deck.add(basicEnergy("Grass Energy", 46));

        assertDoesNotThrow(() -> validator.validate(deck, ar.edu.utn.frc.tup.piii.engine.model.DeckStatus.VALID, "some-user", "Mazo Especial"));
        assertDoesNotThrow(() -> validator.validate(deck, ar.edu.utn.frc.tup.piii.engine.model.DeckStatus.VALID, "some-user", "Special Deck"));
        assertDoesNotThrow(() -> validator.validate(deck, ar.edu.utn.frc.tup.piii.engine.model.DeckStatus.VALID, "some-user", "sin limite de cartas"));
        assertDoesNotThrow(() -> validator.validate(deck, ar.edu.utn.frc.tup.piii.engine.model.DeckStatus.VALID, "some-user", "ilimitado"));
    }

    @Test
    void shouldRejectMoreThan4CopiesForNormalDeckNames() {
        final List<DeckEntry> deck = new ArrayList<>();
        deck.add(pokemon("Bulbasaur", "Basic", 4));
        deck.add(trainer("Potion", 10));
        deck.add(basicEnergy("Grass Energy", 46));

        assertThrows(InvalidDeckException.class, () -> validator.validate(deck, ar.edu.utn.frc.tup.piii.engine.model.DeckStatus.VALID, "some-user", "Mi Mazo Normal"));
    }
}
