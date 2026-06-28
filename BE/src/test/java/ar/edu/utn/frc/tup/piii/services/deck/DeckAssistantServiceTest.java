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
    void autocomplete_injectsTrainersAndEnergiesWhenMissingMoreThan20Cards() {
        // Arrange
        // Current size: 4
        List<DeckCardRequestDTO> currentCards = List.of(
                new DeckCardRequestDTO("xy1-14", 4) // Charmander
        );

        // Act
        List<DeckCardRequestDTO> result = service.autocomplete(currentCards);

        // Assert
        int totalCards = result.stream().mapToInt(DeckCardRequestDTO::quantity).sum();
        assertEquals(60, totalCards, "The total cards should be exactly 60");

        boolean hasSycamore = result.stream().anyMatch(c -> c.cardId().equals("xy1-123") && c.quantity() == 4);
        boolean hasShauna = result.stream().anyMatch(c -> c.cardId().equals("xy1-127") && c.quantity() == 4);
        boolean hasLetter = result.stream().anyMatch(c -> c.cardId().equals("xy1-122") && c.quantity() == 2);
        boolean hasFireEnergy = result.stream().anyMatch(c -> c.cardId().equals("xy1-133") && c.quantity() == 46);

        assertTrue(hasSycamore, "Should inject 4 Professor Sycamore");
        assertTrue(hasShauna, "Should inject 4 Shauna");
        assertTrue(hasLetter, "Should inject 2 Professor's Letter");
        assertTrue(hasFireEnergy, "Should inject 46 Fire Energy cards to complete the deck");
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
