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

    private static final String SUPERTYPE_POKEMON = "Pokémon";
    private static final String SUPERTYPE_TRAINER = "Trainer";
    private static final String SUBTYPE_BASIC = "Basic";
    private static final String SUBTYPE_ITEM = "Item";
    private static final String SUBTYPE_SUPPORTER = "Supporter";

    private final Map<Long, DeckResponseDTO> templates;

    public DeckTemplateService() {
        // Mazo Fuego XY1
        final List<DeckCardResponseDTO> fireCards = List.of(
                new DeckCardResponseDTO("xy1-14", "Charmander", SUPERTYPE_POKEMON, SUBTYPE_BASIC, 4),
                new DeckCardResponseDTO("xy1-15", "Charmeleon", SUPERTYPE_POKEMON, "Stage 1", 3),
                new DeckCardResponseDTO("xy1-16", "Charizard", SUPERTYPE_POKEMON, "Stage 2", 2),
                new DeckCardResponseDTO("xy1-120", "Max Revive", SUPERTYPE_TRAINER, SUBTYPE_ITEM, 2),
                new DeckCardResponseDTO("xy1-121", "Muscle Band", SUPERTYPE_TRAINER, SUBTYPE_ITEM, 4),
                new DeckCardResponseDTO("xy1-122", "Professor's Letter", SUPERTYPE_TRAINER, SUBTYPE_ITEM, 4),
                new DeckCardResponseDTO("xy1-123", "Professor Sycamore", SUPERTYPE_TRAINER, SUBTYPE_SUPPORTER, 4),
                new DeckCardResponseDTO("xy1-124", "Red Card", SUPERTYPE_TRAINER, SUBTYPE_ITEM, 2),
                new DeckCardResponseDTO("xy1-125", "Roller Skates", SUPERTYPE_TRAINER, SUBTYPE_ITEM, 4),
                new DeckCardResponseDTO("xy1-126", "Shadow Circle", SUPERTYPE_TRAINER, "Stadium", 2),
                new DeckCardResponseDTO("xy1-127", "Shauna", SUPERTYPE_TRAINER, SUBTYPE_SUPPORTER, 4),
                new DeckCardResponseDTO("xy1-128", "Super Potion", SUPERTYPE_TRAINER, SUBTYPE_ITEM, 2),
                new DeckCardResponseDTO("xy1-129", "Team Flare Grunt", SUPERTYPE_TRAINER, SUBTYPE_SUPPORTER, 3),
                new DeckCardResponseDTO("xy1-133", "Fire Energy", "Energy", "Basic Energy", 20)
        );

        // Mazo Agua XY1
        final List<DeckCardResponseDTO> waterCards = List.of(
                new DeckCardResponseDTO("xy1-35", "Froakie", SUPERTYPE_POKEMON, SUBTYPE_BASIC, 4),
                new DeckCardResponseDTO("xy1-36", "Frogadier", SUPERTYPE_POKEMON, "Stage 1", 3),
                new DeckCardResponseDTO("xy1-37", "Greninja", SUPERTYPE_POKEMON, "Stage 2", 2),
                new DeckCardResponseDTO("xy1-38", "Lapras", SUPERTYPE_POKEMON, SUBTYPE_BASIC, 3),
                new DeckCardResponseDTO("xy1-118", "Evosoda", SUPERTYPE_TRAINER, SUBTYPE_ITEM, 4),
                new DeckCardResponseDTO("xy1-122", "Professor's Letter", SUPERTYPE_TRAINER, SUBTYPE_ITEM, 4),
                new DeckCardResponseDTO("xy1-123", "Professor Sycamore", SUPERTYPE_TRAINER, SUBTYPE_SUPPORTER, 4),
                new DeckCardResponseDTO("xy1-125", "Roller Skates", SUPERTYPE_TRAINER, SUBTYPE_ITEM, 4),
                new DeckCardResponseDTO("xy1-127", "Shauna", SUPERTYPE_TRAINER, SUBTYPE_SUPPORTER, 4),
                new DeckCardResponseDTO("xy1-128", "Super Potion", SUPERTYPE_TRAINER, SUBTYPE_ITEM, 4),
                new DeckCardResponseDTO("xy1-129", "Team Flare Grunt", SUPERTYPE_TRAINER, SUBTYPE_SUPPORTER, 4),
                new DeckCardResponseDTO("xy1-134", "Water Energy", "Energy", "Basic Energy", 20)
        );

        // Mazo Planta XY1
        final List<DeckCardResponseDTO> grassCards = List.of(
                new DeckCardResponseDTO("xy1-1", "Weedle", SUPERTYPE_POKEMON, SUBTYPE_BASIC, 4),
                new DeckCardResponseDTO("xy1-2", "Kakuna", SUPERTYPE_POKEMON, "Stage 1", 3),
                new DeckCardResponseDTO("xy1-3", "Beedrill", SUPERTYPE_POKEMON, "Stage 2", 2),
                new DeckCardResponseDTO("xy1-11", "Pinsir", SUPERTYPE_POKEMON, SUBTYPE_BASIC, 3),
                new DeckCardResponseDTO("xy1-118", "Evosoda", SUPERTYPE_TRAINER, SUBTYPE_ITEM, 4),
                new DeckCardResponseDTO("xy1-122", "Professor's Letter", SUPERTYPE_TRAINER, SUBTYPE_ITEM, 4),
                new DeckCardResponseDTO("xy1-123", "Professor Sycamore", SUPERTYPE_TRAINER, SUBTYPE_SUPPORTER, 4),
                new DeckCardResponseDTO("xy1-125", "Roller Skates", SUPERTYPE_TRAINER, SUBTYPE_ITEM, 4),
                new DeckCardResponseDTO("xy1-127", "Shauna", SUPERTYPE_TRAINER, SUBTYPE_SUPPORTER, 4),
                new DeckCardResponseDTO("xy1-128", "Super Potion", SUPERTYPE_TRAINER, SUBTYPE_ITEM, 4),
                new DeckCardResponseDTO("xy1-129", "Team Flare Grunt", SUPERTYPE_TRAINER, SUBTYPE_SUPPORTER, 4),
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
