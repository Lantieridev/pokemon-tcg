package ar.edu.utn.frc.tup.piii.services.deck;

import ar.edu.utn.frc.tup.piii.dtos.deck.DeckCardRequestDTO;
import ar.edu.utn.frc.tup.piii.dtos.deck.DeckCardResponseDTO;
import ar.edu.utn.frc.tup.piii.dtos.deck.SmartDeckRequestDTO;
import ar.edu.utn.frc.tup.piii.engine.model.Card;
import ar.edu.utn.frc.tup.piii.engine.model.EvolutionStage;
import ar.edu.utn.frc.tup.piii.engine.model.PokemonCard;
import ar.edu.utn.frc.tup.piii.engine.model.PokemonType;
import ar.edu.utn.frc.tup.piii.persistence.entity.CardEntity;
import ar.edu.utn.frc.tup.piii.persistence.mapper.CardMapper;
import ar.edu.utn.frc.tup.piii.persistence.repository.CardRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.stream.Collectors;

@Service
public class DeckAssistantService {

    private final DeckTemplateService templateService;
    private final CardRepository cardRepository;
    private final CardMapper cardMapper;

    public DeckAssistantService(DeckTemplateService templateService, CardRepository cardRepository, CardMapper cardMapper) {
        this.templateService = templateService;
        this.cardRepository = cardRepository;
        this.cardMapper = cardMapper;
    }

    public List<DeckCardRequestDTO> autocomplete(List<DeckCardRequestDTO> currentCards) {
        int currentSize = currentCards.stream().mapToInt(DeckCardRequestDTO::quantity).sum();
        if (currentSize >= 60) {
            return currentCards;
        }

        // TODO: Refactor to check card supertype/subtype from API instead of hardcoded strings
        // Find dominant type based on templates
        boolean hasFire = currentCards.stream().anyMatch(c -> c.cardId().contains("xy1-14") || c.cardId().contains("xy1-15") || c.cardId().contains("xy1-16"));
        boolean hasWater = currentCards.stream().anyMatch(c -> c.cardId().contains("xy1-35") || c.cardId().contains("xy1-36"));

        int missing = 60 - currentSize;

        // Starting mostly from scratch (< 40 cards picked): the trainer+energy
        // top-off below never adds a single Pokemon card, which produces an
        // unplayable deck (every real match requires a Basic Pokemon just to
        // set up). Build a complete, known-valid deck (real evolution lines +
        // trainers + energy, same generator the deck wizard's fallback uses)
        // instead of layering on top of a near-empty selection.
        if (missing > 20) {
            String theme = hasWater ? "water" : (hasFire ? "fire" : "grass");
            return generateFallbackWizardDeck(theme);
        }

        List<DeckCardRequestDTO> result = new ArrayList<>(currentCards);
        String energyId = hasWater ? "xy1-134" : (hasFire ? "xy1-133" : "xy1-132");

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
        return generateWizardDeck(new SmartDeckRequestDTO(theme, null, null, null));
    }

    public List<DeckCardRequestDTO> generateWizardDeck(SmartDeckRequestDTO request) {
        if (cardRepository == null || cardMapper == null) {
            return generateFallbackWizardDeck(request.theme());
        }

        List<DeckCardRequestDTO> deck = new ArrayList<>();
        Random rand = new Random();

        // 1. Resolve target types and generation
        List<PokemonType> targetTypes = getTargetTypes(request.preferredTypes(), request.theme());
        String generation = request.generation();

        // 2. Fetch all cards from CardRepository
        List<CardEntity> allEntities = cardRepository.findAll();
        if (allEntities.isEmpty()) {
            return generateFallbackWizardDeck(request.theme());
        }

        // 3. Filter Pokémon cards matching targetTypes & generation (if specified)
        List<PokemonCard> pokemonCards = new ArrayList<>();
        for (CardEntity entity : allEntities) {
            if ("Pokémon".equalsIgnoreCase(entity.getSupertype()) || (entity.getSupertype() != null && entity.getSupertype().startsWith("Pok"))) {
                try {
                    Card mapped = cardMapper.map(entity);
                    if (mapped instanceof PokemonCard pc) {
                        pokemonCards.add(pc);
                    }
                } catch (Exception e) {
                    // Ignore malformed cards
                }
            }
        }

        // Apply generation filter
        if (generation != null && !generation.isBlank() && !"Cualquiera".equalsIgnoreCase(generation)) {
            String genLower = generation.toLowerCase();
            if (genLower.contains("6") || genLower.contains("xy")) {
                pokemonCards = pokemonCards.stream()
                        .filter(p -> p.getCardId() != null && p.getCardId().startsWith("xy"))
                        .collect(Collectors.toList());
            } else if (genLower.contains("1") || genLower.contains("base")) {
                pokemonCards = pokemonCards.stream()
                        .filter(p -> p.getCardId() != null && (p.getCardId().startsWith("base") || p.getCardId().startsWith("xy")))
                        .collect(Collectors.toList());
            }
        }

        // 4. Build evolution lines from Pokemon cards
        List<List<PokemonCard>> allEvolutionLines = buildEvolutionLines(pokemonCards);

        // Filter lines matching target types
        List<List<PokemonCard>> filteredLines = allEvolutionLines.stream()
                .filter(line -> !line.isEmpty() && targetTypes.contains(line.get(0).getPokemonType()))
                .collect(Collectors.toList());

        // Fallback if filters are too restrictive and returned nothing
        if (filteredLines.isEmpty()) {
            filteredLines = allEvolutionLines;
        }

        if (filteredLines.isEmpty()) {
            return generateFallbackWizardDeck(request.theme());
        }

        // Shuffle lines to add variety
        Collections.shuffle(filteredLines, rand);

        // Determine lines count: default to 2
        int linesCount = (request.evolutionLinesCount() != null && request.evolutionLinesCount() > 0)
                ? request.evolutionLinesCount() : 2;

        List<List<PokemonCard>> pickedLines = filteredLines.subList(0, Math.min(linesCount, filteredLines.size()));

        // Add picked lines to deck
        for (List<PokemonCard> line : pickedLines) {
            if (line.size() == 3) {
                deck.add(new DeckCardRequestDTO(line.get(0).getCardId(), 4)); // Basic
                deck.add(new DeckCardRequestDTO(line.get(1).getCardId(), 3)); // Stage 1
                deck.add(new DeckCardRequestDTO(line.get(2).getCardId(), 2)); // Stage 2
            } else if (line.size() == 2) {
                deck.add(new DeckCardRequestDTO(line.get(0).getCardId(), 4)); // Basic
                deck.add(new DeckCardRequestDTO(line.get(1).getCardId(), 3)); // Stage 1
            } else if (line.size() == 1) {
                deck.add(new DeckCardRequestDTO(line.get(0).getCardId(), 4)); // Basic
            }
        }

        // 5. Add EX card of target types if available
        PokemonCard exCard = pokemonCards.stream()
                .filter(p -> p.isEx() && targetTypes.contains(p.getPokemonType()))
                .findFirst()
                .orElse(null);
        if (exCard != null) {
            deck.add(new DeckCardRequestDTO(exCard.getCardId(), 2));
        }

        // 6. Add Core Trainers (22 cards)
        deck.add(new DeckCardRequestDTO("xy1-123", 4)); // Professor's Letter
        deck.add(new DeckCardRequestDTO("xy1-127", 4)); // Shauna
        deck.add(new DeckCardRequestDTO("xy1-128", 4)); // Super Potion
        deck.add(new DeckCardRequestDTO("xy1-125", 4)); // Roller Skates
        deck.add(new DeckCardRequestDTO("xy1-121", 3)); // Muscle Band
        deck.add(new DeckCardRequestDTO("xy1-124", 3)); // Red Card

        // 7. Fill remaining with corresponding Energies
        int currentSize = deck.stream().mapToInt(DeckCardRequestDTO::quantity).sum();
        int missing = 60 - currentSize;
        if (missing > 0) {
            List<String> energyIds = targetTypes.stream()
                    .map(this::getEnergyId)
                    .distinct()
                    .toList();

            int energyPerType = missing / energyIds.size();
            int remainder = missing % energyIds.size();

            for (int i = 0; i < energyIds.size(); i++) {
                int qty = energyPerType + (i == 0 ? remainder : 0);
                if (qty > 0) {
                    if (energyIds.get(i).equals("xy1-130")) {
                        deck.add(new DeckCardRequestDTO("xy1-130", Math.min(4, qty)));
                        if (qty > 4) {
                            deck.add(new DeckCardRequestDTO("xy1-134", qty - 4));
                        }
                    } else {
                        deck.add(new DeckCardRequestDTO(energyIds.get(i), qty));
                    }
                }
            }
        }

        return deck;
    }

    private List<List<PokemonCard>> buildEvolutionLines(List<PokemonCard> pokemonCards) {
        List<List<PokemonCard>> lines = new ArrayList<>();
        
        List<PokemonCard> stage2Cards = pokemonCards.stream()
                .filter(p -> p.getEvolutionStage() == EvolutionStage.STAGE_2)
                .toList();
        List<PokemonCard> stage1Cards = pokemonCards.stream()
                .filter(p -> p.getEvolutionStage() == EvolutionStage.STAGE_1)
                .toList();
        List<PokemonCard> basicCards = pokemonCards.stream()
                .filter(p -> p.getEvolutionStage() == EvolutionStage.BASIC)
                .toList();

        List<PokemonCard> usedStage1 = new ArrayList<>();
        List<PokemonCard> usedBasic = new ArrayList<>();

        // 1. Build Stage 2 lines: Basic -> Stage 1 -> Stage 2
        for (PokemonCard stage2 : stage2Cards) {
            String evolvesFromStage1 = stage2.getEvolvesFrom();
            if (evolvesFromStage1 != null) {
                PokemonCard stage1 = stage1Cards.stream()
                        .filter(p -> p.getName().equalsIgnoreCase(evolvesFromStage1))
                        .findFirst()
                        .orElse(null);
                if (stage1 != null) {
                    String evolvesFromBasic = stage1.getEvolvesFrom();
                    if (evolvesFromBasic != null) {
                        PokemonCard basic = basicCards.stream()
                                .filter(p -> p.getName().equalsIgnoreCase(evolvesFromBasic))
                                .findFirst()
                                .orElse(null);
                        if (basic != null) {
                            lines.add(List.of(basic, stage1, stage2));
                            usedStage1.add(stage1);
                            usedBasic.add(basic);
                            continue;
                        }
                    }
                }
            }
        }

        // 2. Build Stage 1 lines: Basic -> Stage 1
        for (PokemonCard stage1 : stage1Cards) {
            if (usedStage1.contains(stage1)) continue;
            String evolvesFromBasic = stage1.getEvolvesFrom();
            if (evolvesFromBasic != null) {
                PokemonCard basic = basicCards.stream()
                        .filter(p -> p.getName().equalsIgnoreCase(evolvesFromBasic))
                        .findFirst()
                        .orElse(null);
                if (basic != null) {
                    lines.add(List.of(basic, stage1));
                    usedBasic.add(basic);
                }
            }
        }

        // 3. Leftover basic cards
        for (PokemonCard basic : basicCards) {
            if (usedBasic.contains(basic)) continue;
            lines.add(List.of(basic));
        }

        return lines;
    }

    private List<PokemonType> getTargetTypes(List<String> preferredTypes, String theme) {
        List<PokemonType> targetTypes = new ArrayList<>();
        if (preferredTypes != null && !preferredTypes.isEmpty()) {
            for (String t : preferredTypes) {
                PokemonType pt = parseThemeToType(t);
                if (pt != null) {
                    targetTypes.add(pt);
                }
            }
        }
        if (targetTypes.isEmpty() && theme != null) {
            // handle "fire y water" joint format
            if (theme.contains(" y ")) {
                for (String t : theme.split(" y ")) {
                    PokemonType pt = parseThemeToType(t);
                    if (pt != null) {
                        targetTypes.add(pt);
                    }
                }
            } else {
                PokemonType pt = parseThemeToType(theme);
                if (pt != null) {
                    targetTypes.add(pt);
                }
            }
        }
        if (targetTypes.isEmpty()) {
            targetTypes.add(PokemonType.GRASS);
        }
        return targetTypes;
    }

    private PokemonType parseThemeToType(String theme) {
        if (theme == null) return null;
        String t = theme.toLowerCase().trim();
        switch (t) {
            case "fire": return PokemonType.FIRE;
            case "water": return PokemonType.WATER;
            case "lightning":
            case "electric": return PokemonType.LIGHTNING;
            case "psychic": return PokemonType.PSYCHIC;
            case "fighting": return PokemonType.FIGHTING;
            case "darkness":
            case "dark": return PokemonType.DARKNESS;
            case "metal":
            case "steel": return PokemonType.METAL;
            case "fairy": return PokemonType.FAIRY;
            case "colorless":
            case "normal": return PokemonType.COLORLESS;
            case "grass": return PokemonType.GRASS;
            default: return null;
        }
    }

    private String getEnergyId(PokemonType type) {
        if (type == null) return "xy1-132";
        switch (type) {
            case FIRE: return "xy1-133";
            case WATER: return "xy1-134";
            case LIGHTNING: return "xy1-135";
            case PSYCHIC: return "xy1-136";
            case FIGHTING: return "xy1-137";
            case DARKNESS: return "xy1-138";
            case METAL: return "xy1-139";
            case FAIRY: return "xy1-140";
            case COLORLESS: return "xy1-130";
            case GRASS:
            default: return "xy1-132";
        }
    }

    private List<DeckCardRequestDTO> generateFallbackWizardDeck(String theme) {
        List<DeckCardRequestDTO> deck = new ArrayList<>();
        Random rand = new Random();

        List<List<String>> allLines;
        String energyId;
        String exCard = null;
        String normalizedTheme = theme != null ? theme.toLowerCase() : "grass";

        switch (normalizedTheme) {
            case "fire":
                allLines = new ArrayList<>(Arrays.asList(
                        Arrays.asList("xy1-24", "xy1-25", "xy1-26"), // Fennekin
                        Arrays.asList("xy1-20", "xy1-21"),           // Slugma
                        Arrays.asList("xy1-22", "xy1-23"),           // Pansear
                        Arrays.asList("xy1-27", "xy1-28")            // Fletchinder
                ));
                energyId = "xy1-133";
                break;
            case "water":
                allLines = new ArrayList<>(Arrays.asList(
                        Arrays.asList("xy1-39", "xy1-40", "xy1-41"), // Froakie
                        Arrays.asList("xy1-31", "xy1-32"),           // Shellder
                        Arrays.asList("xy1-33", "xy1-34"),           // Staryu
                        Arrays.asList("xy1-37", "xy1-38")            // Panpour
                ));
                exCard = "xy1-29"; // Blastoise-EX
                energyId = "xy1-134";
                break;
            case "lightning":
            case "electric":
                allLines = new ArrayList<>(Arrays.asList(
                        Arrays.asList("xy1-42", "xy1-43"),           // Pikachu
                        Arrays.asList("xy1-44", "xy1-45")            // Voltorb
                ));
                exCard = "xy1-46"; // Emolga-EX
                energyId = "xy1-135";
                break;
            case "psychic":
                allLines = new ArrayList<>(Arrays.asList(
                        Arrays.asList("xy1-47", "xy1-48"),           // Ekans
                        Arrays.asList("xy1-49", "xy1-50"),           // Spoink
                        Arrays.asList("xy1-51", "xy1-52", "xy1-53"), // Venipede
                        Arrays.asList("xy1-54", "xy1-55"),           // Phantump
                        Arrays.asList("xy1-56", "xy1-57")            // Pumpkaboo
                ));
                energyId = "xy1-136";
                break;
            case "fighting":
                allLines = new ArrayList<>(Arrays.asList(
                        Arrays.asList("xy1-58", "xy1-59"),           // Diglett
                        Arrays.asList("xy1-60", "xy1-61", "xy1-62"), // Rhyhorn
                        Arrays.asList("xy1-65", "xy1-66", "xy1-67")  // Timburr
                ));
                energyId = "xy1-137";
                break;
            case "darkness":
            case "dark":
                allLines = new ArrayList<>(Arrays.asList(
                        Arrays.asList("xy1-69", "xy1-70", "xy1-71"), // Sandile
                        Arrays.asList("xy1-72", "xy1-73"),           // Zorua
                        Arrays.asList("xy1-74", "xy1-76")            // Inkay
                ));
                exCard = "xy1-79"; // Yveltal-EX
                energyId = "xy1-138";
                break;
            case "metal":
            case "steel":
                allLines = new ArrayList<>(Arrays.asList(
                        Arrays.asList("xy1-81", "xy1-82"),           // Pawniard
                        Arrays.asList("xy1-83", "xy1-84", "xy1-85")  // Honedge
                ));
                exCard = "xy1-80"; // Skarmory-EX
                energyId = "xy1-139";
                break;
            case "fairy":
                allLines = new ArrayList<>(Arrays.asList(
                        Arrays.asList("xy1-87", "xy1-89"),           // Jigglypuff
                        Arrays.asList("xy1-92", "xy1-93"),           // Spritzee
                        Arrays.asList("xy1-94", "xy1-95")            // Swirlix
                ));
                exCard = "xy1-97"; // Xerneas-EX
                energyId = "xy1-140";
                break;
            case "colorless":
            case "normal":
                allLines = new ArrayList<>(Arrays.asList(
                        Arrays.asList("xy1-98", "xy1-99"),           // Doduo
                        Arrays.asList("xy1-102", "xy1-103"),         // Taillow
                        Arrays.asList("xy1-104", "xy1-105"),         // Skitty
                        Arrays.asList("xy1-106", "xy1-107"),         // Bidoof
                        Arrays.asList("xy1-108", "xy1-109", "xy1-110"), // Lillipup
                        Arrays.asList("xy1-111", "xy1-112")          // Bunnelby
                ));
                energyId = "xy1-130";
                break;
            case "grass":
            default:
                allLines = new ArrayList<>(Arrays.asList(
                        Arrays.asList("xy1-12", "xy1-13", "xy1-14"), // Chespin
                        Arrays.asList("xy1-3", "xy1-4", "xy1-5"),    // Weedle
                        Arrays.asList("xy1-10", "xy1-11")            // Pansage
                ));
                exCard = "xy1-1";
                energyId = "xy1-132";
                break;
        }

        Collections.shuffle(allLines, rand);
        List<List<String>> pickedLines = allLines.subList(0, Math.min(2, allLines.size()));

        for (List<String> line : pickedLines) {
            if (line.size() == 3) {
                deck.add(new DeckCardRequestDTO(line.get(0), 4));
                deck.add(new DeckCardRequestDTO(line.get(1), 3));
                deck.add(new DeckCardRequestDTO(line.get(2), 2));
            } else if (line.size() == 2) {
                deck.add(new DeckCardRequestDTO(line.get(0), 4));
                deck.add(new DeckCardRequestDTO(line.get(1), 3));
            }
        }

        if (exCard != null) {
            deck.add(new DeckCardRequestDTO(exCard, 2));
        }

        deck.add(new DeckCardRequestDTO("xy1-123", 4));
        deck.add(new DeckCardRequestDTO("xy1-127", 4));
        deck.add(new DeckCardRequestDTO("xy1-128", 4));
        deck.add(new DeckCardRequestDTO("xy1-125", 4));
        deck.add(new DeckCardRequestDTO("xy1-121", 3));
        deck.add(new DeckCardRequestDTO("xy1-124", 3));

        int currentSize = deck.stream().mapToInt(DeckCardRequestDTO::quantity).sum();
        int missing = 60 - currentSize;
        if (missing > 0) {
            if (energyId.equals("xy1-130")) {
                deck.add(new DeckCardRequestDTO("xy1-130", Math.min(4, missing)));
                if (missing > 4) {
                    deck.add(new DeckCardRequestDTO("xy1-134", missing - 4));
                }
            } else {
                deck.add(new DeckCardRequestDTO(energyId, missing));
            }
        }

        return deck;
    }
}
