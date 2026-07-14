package ar.edu.utn.frc.tup.piii.services.deck;

import ar.edu.utn.frc.tup.piii.dtos.deck.DeckCardRequestDTO;
import ar.edu.utn.frc.tup.piii.dtos.deck.DeckCardResponseDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class DeckAssistantServiceTest {

    private DeckAssistantService service;

    @BeforeEach
    void setUp() {
        DeckTemplateService templateService = mock(DeckTemplateService.class);
        service = new DeckAssistantService(templateService, null, null);
    }

    @Test
    void autocomplete_buildsACompleteDeckWithRealPokemonWhenMissingMoreThan20Cards() {
        // Regression test: autocomplete() used to fill a near-empty deck with
        // ONLY trainers + a single energy type (e.g. 4 Sycamore + 4 Shauna +
        // 2 Professor's Letter + 46 Fire Energy = 60 cards, zero Pokemon),
        // producing a deck that can never legally start a match (every real
        // game requires a Basic Pokemon). Fixed to delegate to the same
        // known-good evolution-line generator the deck wizard's fallback
        // uses, so the result always contains real Pokemon.
        //
        // generateFallbackWizardDeck() shuffles its candidate evolution lines, so this
        // uses the package-private seeded-Random constructor to assert the EXACT deck
        // composition rather than just aggregate properties (total count, "some Pokemon
        // exists") that a broken evolution-line generator could still satisfy by luck.
        final DeckAssistantService seededService = new DeckAssistantService(
                mock(DeckTemplateService.class), null, null, new java.util.Random(42));
        List<DeckCardRequestDTO> currentCards = List.of(
                new DeckCardRequestDTO("xy1-14", 4) // Charmander (signals the fire theme)
        );

        List<DeckCardRequestDTO> result = seededService.autocomplete(currentCards);

        int totalCards = result.stream().mapToInt(DeckCardRequestDTO::quantity).sum();
        assertEquals(60, totalCards, "The total cards should be exactly 60");

        assertEquals(
                List.of(
                        new DeckCardRequestDTO("xy1-27", 4), // Fletchinder (basic)
                        new DeckCardRequestDTO("xy1-28", 3), // Fletchinder (stage 1)
                        new DeckCardRequestDTO("xy1-20", 4), // Slugma (basic)
                        new DeckCardRequestDTO("xy1-21", 3), // Slugma (stage 1)
                        new DeckCardRequestDTO("xy1-123", 4), // Professor's Letter
                        new DeckCardRequestDTO("xy1-127", 4), // Shauna
                        new DeckCardRequestDTO("xy1-128", 4), // Super Potion
                        new DeckCardRequestDTO("xy1-125", 4), // Roller Skates
                        new DeckCardRequestDTO("xy1-121", 3), // Muscle Band
                        new DeckCardRequestDTO("xy1-124", 3), // Red Card
                        new DeckCardRequestDTO("xy1-133", 24) // Fire Energy
                ),
                result,
                "Deck composition for seed 42 must match exactly - a change here means "
                        + "the evolution-line generator or trainer/energy fill logic changed");
    }

    @Test
    void autocomplete_returnsCurrentCardsUnchangedWhenAlreadyAt60() {
        List<DeckCardRequestDTO> full = List.of(new DeckCardRequestDTO("xy1-132", 60));
        assertEquals(full, service.autocomplete(full));
    }

    @Test
    void autocomplete_fillsWithEnergyOnlyWhenExactlyAtThreshold() {
        // FROM_SCRATCH_THRESHOLD = 20: the full-rebuild path only triggers for
        // `missing > 20`, so missing == 20 (the boundary itself) must still take the
        // energy-only top-off path, not the fallback deck generator.
        List<DeckCardRequestDTO> currentCards = List.of(new DeckCardRequestDTO("xy1-14", 40));

        List<DeckCardRequestDTO> result = service.autocomplete(currentCards);

        assertEquals(60, result.stream().mapToInt(DeckCardRequestDTO::quantity).sum());
        assertEquals(2, result.size(), "Should only be the original entry plus one energy top-off");
        assertTrue(result.stream().anyMatch(c -> c.cardId().equals("xy1-133") && c.quantity() == 20),
                "Missing exactly 20 cards should be filled entirely with Fire Energy");
    }

    @Test
    void autocomplete_triggersFullRebuildJustPastThreshold() {
        // missing == 21 is one past FROM_SCRATCH_THRESHOLD and must trigger the fallback
        // deck generator, discarding the near-empty original selection entirely.
        List<DeckCardRequestDTO> currentCards = List.of(new DeckCardRequestDTO("xy1-14", 39));

        List<DeckCardRequestDTO> result = service.autocomplete(currentCards);

        assertEquals(60, result.stream().mapToInt(DeckCardRequestDTO::quantity).sum());
        assertTrue(result.stream().noneMatch(c -> c.cardId().equals("xy1-14") && c.quantity() == 39),
                "Missing more than the threshold should discard the near-empty selection and rebuild");
    }

    @Test
    void autocomplete_selectsWaterThemeWhenWaterCardsPresent() {
        List<DeckCardRequestDTO> currentCards = List.of(
                new DeckCardRequestDTO("xy1-35", 4) // Froakie signals the water theme
        );

        List<DeckCardRequestDTO> result = service.autocomplete(currentCards);

        assertEquals(60, result.stream().mapToInt(DeckCardRequestDTO::quantity).sum());
        assertTrue(result.stream().anyMatch(c -> c.cardId().equals("xy1-134")),
                "Water-themed autocomplete should fill with Water Energy");
    }

    @Test
    void getSuggestions_returnsValidData() {
        // Arrange
        List<DeckCardRequestDTO> currentCards = List.of(
                new DeckCardRequestDTO("xy1-14", 4) // Charmander
        );

        // Act
        List<DeckCardResponseDTO> suggestions = service.getSuggestions(currentCards);

        // Assert
        assertTrue(suggestions.size() >= 3, "Should return at least 3 basic suggestions");
        
        boolean suggestsCharmeleon = suggestions.stream().anyMatch(c -> c.cardId().equals("xy1-15"));
        boolean suggestsFireEnergy = suggestions.stream().anyMatch(c -> c.cardId().equals("xy1-133"));
        boolean suggestsSycamore = suggestions.stream().anyMatch(c -> c.cardId().equals("xy1-122"));

        assertTrue(suggestsCharmeleon, "Should suggest Charmeleon for Charmander");
        assertTrue(suggestsFireEnergy, "Should suggest Fire Energy for Charmander");
        assertTrue(suggestsSycamore, "Should suggest Professor Sycamore");
    }

    @Test
    void generateWizardDeck_coversAllFallbackThemes() {
        List<String> themes = List.of(
                "fire", "water", "lightning", "psychic", "fighting",
                "darkness", "metal", "fairy", "colorless", "grass"
        );
        for (String theme : themes) {
            List<DeckCardRequestDTO> deck = service.generateWizardDeck(theme);
            int total = deck.stream().mapToInt(DeckCardRequestDTO::quantity).sum();
            assertEquals(60, total, "Deck for theme " + theme + " must have exactly 60 cards");
        }
    }
}
