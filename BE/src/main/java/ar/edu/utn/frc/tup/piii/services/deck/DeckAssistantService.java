package ar.edu.utn.frc.tup.piii.services.deck;

import ar.edu.utn.frc.tup.piii.dtos.deck.DeckCardRequestDTO;
import ar.edu.utn.frc.tup.piii.dtos.deck.DeckCardResponseDTO;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class DeckAssistantService {

    private final DeckTemplateService templateService;

    public DeckAssistantService(DeckTemplateService templateService) {
        this.templateService = templateService;
    }

    public List<DeckCardRequestDTO> autocomplete(List<DeckCardRequestDTO> currentCards) {
        int currentSize = currentCards.stream().mapToInt(DeckCardRequestDTO::quantity).sum();
        if (currentSize >= 60) {
            return currentCards;
        }

        List<DeckCardRequestDTO> result = new ArrayList<>(currentCards);
        
        // TODO: Refactor to check card supertype/subtype from API instead of hardcoded strings
        // Find dominant type based on templates
        boolean hasFire = currentCards.stream().anyMatch(c -> c.cardId().contains("xy1-14") || c.cardId().contains("xy1-15") || c.cardId().contains("xy1-16"));
        boolean hasWater = currentCards.stream().anyMatch(c -> c.cardId().contains("xy1-35") || c.cardId().contains("xy1-36"));
        
        String energyId = hasWater ? "xy1-134" : (hasFire ? "xy1-133" : "xy1-132");
        
        int missing = 60 - currentSize;

        // If a lot of cards are missing, inject some essential trainers
        if (missing > 20) {
            addOrUpdateCard(result, "xy1-123", 4); // Professor Sycamore
            addOrUpdateCard(result, "xy1-127", 4); // Shauna
            addOrUpdateCard(result, "xy1-122", 2); // Professor's Letter
            missing -= 10;
        }

        // Fill the rest with the corresponding energy
        addOrUpdateCard(result, energyId, missing);

        return result;
    }

    private void addOrUpdateCard(List<DeckCardRequestDTO> cards, String cardId, int amount) {
        if (amount <= 0) return;
        boolean found = false;
        for (int i = 0; i < cards.size(); i++) {
            if (cards.get(i).cardId().equals(cardId)) {
                cards.set(i, new DeckCardRequestDTO(cardId, cards.get(i).quantity() + amount));
                found = true;
                break;
            }
        }
        if (!found) {
            cards.add(new DeckCardRequestDTO(cardId, amount));
        }
    }

    public List<DeckCardResponseDTO> getSuggestions(List<DeckCardRequestDTO> currentCards) {
        // Suggest trainers if low on them, or evolution if basic is present
        List<DeckCardResponseDTO> suggestions = new ArrayList<>();
        suggestions.add(new DeckCardResponseDTO("xy1-123", "Professor Sycamore", "Trainer", "Supporter", 1));
        suggestions.add(new DeckCardResponseDTO("xy1-122", "Professor's Letter", "Trainer", "Item", 1));
        suggestions.add(new DeckCardResponseDTO("xy1-128", "Super Potion", "Trainer", "Item", 1));
        
        // TODO: Refactor to check card supertype/subtype from API
        boolean hasCharmander = currentCards.stream().anyMatch(c -> c.cardId().equals("xy1-14"));
        if (hasCharmander) {
            suggestions.add(new DeckCardResponseDTO("xy1-15", "Charmeleon", "Pokémon", "Stage 1", 1));
            suggestions.add(new DeckCardResponseDTO("xy1-133", "Fire Energy", "Energy", "Basic Energy", 1));
        }

        return suggestions;
    }

    public List<DeckCardRequestDTO> generateWizardDeck(String theme) {
        Long templateId = -3L; // Grass default
        if ("fire".equalsIgnoreCase(theme)) {
            templateId = -1L;
        } else if ("water".equalsIgnoreCase(theme)) {
            templateId = -2L;
        }
        
        return templateService.getTemplateById(templateId).cards().stream()
                .map(c -> new DeckCardRequestDTO(c.cardId(), c.quantity()))
                .collect(Collectors.toList());
    }
}
