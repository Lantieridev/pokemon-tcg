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
        List<DeckCardRequestDTO> currentCards = List.of(
                new DeckCardRequestDTO("xy1-14", 4) // Charmander (signals the fire theme)
        );

        List<DeckCardRequestDTO> result = service.autocomplete(currentCards);

        int totalCards = result.stream().mapToInt(DeckCardRequestDTO::quantity).sum();
        assertEquals(60, totalCards, "The total cards should be exactly 60");

        long pokemonCopies = result.stream()
                .filter(c -> !c.cardId().equals("xy1-123") && !c.cardId().equals("xy1-127")
                        && !c.cardId().equals("xy1-122") && !c.cardId().equals("xy1-128")
                        && !c.cardId().equals("xy1-125") && !c.cardId().equals("xy1-121")
                        && !c.cardId().equals("xy1-124") && !c.cardId().equals("xy1-133")
                        && !c.cardId().equals("xy1-134") && !c.cardId().equals("xy1-132")
                        && !c.cardId().equals("xy1-130"))
                .mapToInt(DeckCardRequestDTO::quantity)
                .sum();
        assertTrue(pokemonCopies > 0, "The generated deck must contain real Pokemon cards, not just trainers and energy");
    }

    @Test
    void autocomplete_returnsCurrentCardsUnchangedWhenAlreadyAt60() {
        List<DeckCardRequestDTO> full = List.of(new DeckCardRequestDTO("xy1-132", 60));
        assertEquals(full, service.autocomplete(full));
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
        boolean suggestsSycamore = suggestions.stream().anyMatch(c -> c.cardId().equals("xy1-123"));

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
