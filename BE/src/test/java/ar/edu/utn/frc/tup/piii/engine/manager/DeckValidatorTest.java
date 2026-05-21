package ar.edu.utn.frc.tup.piii.engine.manager;

import ar.edu.utn.frc.tup.piii.engine.model.Card;
import ar.edu.utn.frc.tup.piii.engine.model.EnergyCard;
import ar.edu.utn.frc.tup.piii.engine.model.EvolutionStage;
import ar.edu.utn.frc.tup.piii.engine.model.PokemonCard;
import ar.edu.utn.frc.tup.piii.engine.model.PokemonType;
import ar.edu.utn.frc.tup.piii.engine.model.TrainerCard;
import ar.edu.utn.frc.tup.piii.engine.model.TrainerType;
import ar.edu.utn.frc.tup.piii.engine.model.ValidationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertEquals;

class DeckValidatorTest {

    private DeckValidator validator;

    @BeforeEach
    void setUp() {
        validator = new DeckValidator();
    }

    @Test
    void shouldAllowEmptyDeck() {
        assertInstanceOf(ValidationResult.Valid.class, validator.validate(List.of()));
    }

    @Test
    void shouldAllowDeckWithNoAceSpec() {
        final List<Card> deck = List.of(
                pokemon("Charizard"),
                trainer("Cassius", TrainerType.SUPPORTER, false),
                energy("Fire Energy")
        );
        assertInstanceOf(ValidationResult.Valid.class, validator.validate(deck));
    }

    @Test
    void shouldAllowDeckWithExactlyOneAceSpec() {
        final List<Card> deck = List.of(
                pokemon("Pikachu"),
                trainer("Computer Search", TrainerType.ITEM, true),
                energy("Fire Energy")
        );
        assertInstanceOf(ValidationResult.Valid.class, validator.validate(deck));
    }

    @Test
    void shouldRejectDeckWithTwoAceSpecs() {
        final List<Card> deck = List.of(
                trainer("Computer Search", TrainerType.ITEM, true),
                trainer("Crystal Wall", TrainerType.ITEM, true)
        );
        final ValidationResult result = validator.validate(deck);
        final ValidationResult.Invalid invalid = assertInstanceOf(ValidationResult.Invalid.class, result);
        assertEquals("too_many_ace_spec", invalid.reason());
    }

    @Test
    void shouldIgnoreNonAceSpecTrainersWhenCounting() {
        final List<Card> deck = List.of(
                trainer("Evosoda",        TrainerType.ITEM,      false),
                trainer("Professor Sycamore", TrainerType.SUPPORTER, false),
                trainer("Fairy Garden",   TrainerType.STADIUM,   false),
                trainer("Computer Search", TrainerType.ITEM,     true)
        );
        assertInstanceOf(ValidationResult.Valid.class, validator.validate(deck));
    }

    // --- helpers ---

    private static Card pokemon(final String name) {
        return new PokemonCard.Builder("fake-id", name, 60, PokemonType.COLORLESS).build();
    }

    private static Card trainer(final String name, final TrainerType type, final boolean aceSpec) {
        return new TrainerCard.Builder("fake-id", name, type).aceSpec(aceSpec).build();
    }

    private static Card energy(final String name) {
        return new EnergyCard("fake-id", name, PokemonType.COLORLESS, true);
    }
}
