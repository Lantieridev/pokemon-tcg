package ar.edu.utn.frc.tup.piii.services.deck;

import ar.edu.utn.frc.tup.piii.dtos.deck.DeckCardResponseDTO;
import ar.edu.utn.frc.tup.piii.dtos.deck.DeckResponseDTO;
import ar.edu.utn.frc.tup.piii.dtos.deck.DeckSummaryDTO;
import ar.edu.utn.frc.tup.piii.engine.model.DeckStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

@Service
public class DeckTemplateService {

    private final Map<Long, DeckResponseDTO> templates;

    public DeckTemplateService() {
        // Mazo Fuego XY1
        final List<DeckCardResponseDTO> fireCards = List.of(
                new DeckCardResponseDTO("xy1-14", "Charmander", "Pokémon", "Basic", 4),
                new DeckCardResponseDTO("xy1-15", "Charmeleon", "Pokémon", "Stage 1", 3),
                new DeckCardResponseDTO("xy1-16", "Charizard", "Pokémon", "Stage 2", 2),
                new DeckCardResponseDTO("xy1-120", "Max Revive", "Trainer", "Item", 2),
                new DeckCardResponseDTO("xy1-121", "Muscle Band", "Trainer", "Item", 4),
                new DeckCardResponseDTO("xy1-122", "Professor's Letter", "Trainer", "Item", 4),
                new DeckCardResponseDTO("xy1-123", "Professor Sycamore", "Trainer", "Supporter", 4),
                new DeckCardResponseDTO("xy1-124", "Red Card", "Trainer", "Item", 2),
                new DeckCardResponseDTO("xy1-125", "Roller Skates", "Trainer", "Item", 4),
                new DeckCardResponseDTO("xy1-126", "Shadow Circle", "Trainer", "Stadium", 2),
                new DeckCardResponseDTO("xy1-127", "Shauna", "Trainer", "Supporter", 4),
                new DeckCardResponseDTO("xy1-128", "Super Potion", "Trainer", "Item", 2),
                new DeckCardResponseDTO("xy1-129", "Team Flare Grunt", "Trainer", "Supporter", 3),
                new DeckCardResponseDTO("xy1-133", "Fire Energy", "Energy", "Basic Energy", 20)
        );

        // Mazo Agua XY1
        final List<DeckCardResponseDTO> waterCards = List.of(
                new DeckCardResponseDTO("xy1-35", "Froakie", "Pokémon", "Basic", 4),
                new DeckCardResponseDTO("xy1-36", "Frogadier", "Pokémon", "Stage 1", 3),
                new DeckCardResponseDTO("xy1-37", "Greninja", "Pokémon", "Stage 2", 2),
                new DeckCardResponseDTO("xy1-38", "Lapras", "Pokémon", "Basic", 3),
                new DeckCardResponseDTO("xy1-118", "Evosoda", "Trainer", "Item", 4),
                new DeckCardResponseDTO("xy1-122", "Professor's Letter", "Trainer", "Item", 4),
                new DeckCardResponseDTO("xy1-123", "Professor Sycamore", "Trainer", "Supporter", 4),
                new DeckCardResponseDTO("xy1-125", "Roller Skates", "Trainer", "Item", 4),
                new DeckCardResponseDTO("xy1-127", "Shauna", "Trainer", "Supporter", 4),
                new DeckCardResponseDTO("xy1-128", "Super Potion", "Trainer", "Item", 4),
                new DeckCardResponseDTO("xy1-129", "Team Flare Grunt", "Trainer", "Supporter", 4),
                new DeckCardResponseDTO("xy1-134", "Water Energy", "Energy", "Basic Energy", 20)
        );

        // Mazo Planta XY1
        final List<DeckCardResponseDTO> grassCards = List.of(
                new DeckCardResponseDTO("xy1-1", "Weedle", "Pokémon", "Basic", 4),
                new DeckCardResponseDTO("xy1-2", "Kakuna", "Pokémon", "Stage 1", 3),
                new DeckCardResponseDTO("xy1-3", "Beedrill", "Pokémon", "Stage 2", 2),
                new DeckCardResponseDTO("xy1-11", "Pinsir", "Pokémon", "Basic", 3),
                new DeckCardResponseDTO("xy1-118", "Evosoda", "Trainer", "Item", 4),
                new DeckCardResponseDTO("xy1-122", "Professor's Letter", "Trainer", "Item", 4),
                new DeckCardResponseDTO("xy1-123", "Professor Sycamore", "Trainer", "Supporter", 4),
                new DeckCardResponseDTO("xy1-125", "Roller Skates", "Trainer", "Item", 4),
                new DeckCardResponseDTO("xy1-127", "Shauna", "Trainer", "Supporter", 4),
                new DeckCardResponseDTO("xy1-128", "Super Potion", "Trainer", "Item", 4),
                new DeckCardResponseDTO("xy1-129", "Team Flare Grunt", "Trainer", "Supporter", 4),
                new DeckCardResponseDTO("xy1-132", "Grass Energy", "Energy", "Basic Energy", 20)
        );

        templates = Map.of(
                -1L, new DeckResponseDTO(-1L, "Mazo Fuego XY1", DeckStatus.PRECONSTRUCTED, LocalDateTime.now(), fireCards),
                -2L, new DeckResponseDTO(-2L, "Mazo Agua XY1", DeckStatus.PRECONSTRUCTED, LocalDateTime.now(), waterCards),
                -3L, new DeckResponseDTO(-3L, "Mazo Planta XY1", DeckStatus.PRECONSTRUCTED, LocalDateTime.now(), grassCards)
        );
    }

    public List<DeckSummaryDTO> getTemplates() {
        return templates.values().stream()
                .map(t -> new DeckSummaryDTO(t.id(), t.name(), t.status(), t.createdAt(), 60))
                .collect(Collectors.toList());
    }

    public DeckResponseDTO getTemplateById(Long id) {
        if (!templates.containsKey(id)) {
            throw new NoSuchElementException("Template not found: " + id);
        }
        return templates.get(id);
    }
}
