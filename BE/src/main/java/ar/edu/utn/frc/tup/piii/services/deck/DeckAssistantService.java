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
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

@Slf4j
@Service
public class DeckAssistantService {

    private static final int DECK_SIZE = 60;
    private static final int FROM_SCRATCH_THRESHOLD = 20;
    private static final int DEFAULT_EVOLUTION_LINES_COUNT = 2;
    private static final int MAX_ENERGY_COPIES = 4;
    private static final int[] EVOLUTION_LINE_COPIES = {4, 3, 2};

    private static final String DEFAULT_THEME = "grass";
    private static final String ENERGY_GRASS = "xy1-132";
    private static final String ENERGY_FIRE = "xy1-133";
    private static final String ENERGY_WATER = "xy1-134";
    private static final String ENERGY_LIGHTNING = "xy1-135";
    private static final String ENERGY_PSYCHIC = "xy1-136";
    private static final String ENERGY_FIGHTING = "xy1-137";
    private static final String ENERGY_DARKNESS = "xy1-138";
    private static final String ENERGY_METAL = "xy1-139";
    private static final String ENERGY_FAIRY = "xy1-140";
    private static final String ENERGY_COLORLESS = "xy1-130";

    private static final Map<String, PokemonType> THEME_TO_TYPE = Map.ofEntries(
            Map.entry("fire", PokemonType.FIRE),
            Map.entry("water", PokemonType.WATER),
            Map.entry("lightning", PokemonType.LIGHTNING),
            Map.entry("electric", PokemonType.LIGHTNING),
            Map.entry("psychic", PokemonType.PSYCHIC),
            Map.entry("fighting", PokemonType.FIGHTING),
            Map.entry("darkness", PokemonType.DARKNESS),
            Map.entry("dark", PokemonType.DARKNESS),
            Map.entry("metal", PokemonType.METAL),
            Map.entry("steel", PokemonType.METAL),
            Map.entry("fairy", PokemonType.FAIRY),
            Map.entry("colorless", PokemonType.COLORLESS),
            Map.entry("normal", PokemonType.COLORLESS),
            Map.entry(DEFAULT_THEME, PokemonType.GRASS)
    );

    private static final Map<PokemonType, String> TYPE_TO_ENERGY = Map.of(
            PokemonType.FIRE, ENERGY_FIRE,
            PokemonType.WATER, ENERGY_WATER,
            PokemonType.LIGHTNING, ENERGY_LIGHTNING,
            PokemonType.PSYCHIC, ENERGY_PSYCHIC,
            PokemonType.FIGHTING, ENERGY_FIGHTING,
            PokemonType.DARKNESS, ENERGY_DARKNESS,
            PokemonType.METAL, ENERGY_METAL,
            PokemonType.FAIRY, ENERGY_FAIRY,
            PokemonType.COLORLESS, ENERGY_COLORLESS
    );

    private record ThemeStarterDeck(List<List<String>> evolutionLines, String exCard, String energyId) {
    }

    private static final Map<String, ThemeStarterDeck> FALLBACK_DECKS = buildFallbackDecks();

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
        if (currentSize >= DECK_SIZE) {
            return currentCards;
        }

        // TODO: Refactor to check card supertype/subtype from API instead of hardcoded strings
        // Find dominant type based on templates
        boolean hasFire = currentCards.stream().anyMatch(c -> c.cardId().contains("xy1-14") || c.cardId().contains("xy1-15") || c.cardId().contains("xy1-16"));
        boolean hasWater = currentCards.stream().anyMatch(c -> c.cardId().contains("xy1-35") || c.cardId().contains("xy1-36"));

        int missing = DECK_SIZE - currentSize;

        // Starting mostly from scratch (< 40 cards picked): the trainer+energy
        // top-off below never adds a single Pokemon card, which produces an
        // unplayable deck (every real match requires a Basic Pokemon just to
        // set up). Build a complete, known-valid deck (real evolution lines +
        // trainers + energy, same generator the deck wizard's fallback uses)
        // instead of layering on top of a near-empty selection.
        if (missing > FROM_SCRATCH_THRESHOLD) {
            String theme = hasWater ? "water" : (hasFire ? "fire" : DEFAULT_THEME);
            return generateFallbackWizardDeck(theme);
        }

        List<DeckCardRequestDTO> result = new ArrayList<>(currentCards);
        String energyId = hasWater ? ENERGY_WATER : (hasFire ? ENERGY_FIRE : ENERGY_GRASS);

        // Fill the rest with the corresponding energy
        addOrUpdateCard(result, energyId, missing);

        return result;
    }

    private void addOrUpdateCard(List<DeckCardRequestDTO> cards, String cardId, int amount) {
        if (amount <= 0) {
            return;
        }
        for (int i = 0; i < cards.size(); i++) {
            if (cards.get(i).cardId().equals(cardId)) {
                cards.set(i, new DeckCardRequestDTO(cardId, cards.get(i).quantity() + amount));
                return;
            }
        }
        cards.add(new DeckCardRequestDTO(cardId, amount));
    }

    public List<DeckCardResponseDTO> getSuggestions(List<DeckCardRequestDTO> currentCards) {
        // Suggest trainers if low on them, or evolution if basic is present
        List<DeckCardResponseDTO> suggestions = new ArrayList<>();
        suggestions.add(new DeckCardResponseDTO("xy1-123", "Professor Sycamore", "Trainer", "Supporter", 1));
        suggestions.add(new DeckCardResponseDTO("xy1-122", "Professor's Letter", "Trainer", "Item", 1));
        suggestions.add(new DeckCardResponseDTO("xy1-128", "Super Potion", "Trainer", "Item", 1));

        // TODO: Refactor to check card supertype/subtype from API
        boolean hasCharmander = currentCards.stream().anyMatch(c -> "xy1-14".equals(c.cardId()));
        if (hasCharmander) {
            suggestions.add(new DeckCardResponseDTO("xy1-15", "Charmeleon", "Pokémon", "Stage 1", 1));
            suggestions.add(new DeckCardResponseDTO(ENERGY_FIRE, "Fire Energy", "Energy", "Basic Energy", 1));
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

        List<CardEntity> allEntities = cardRepository.findAll();
        if (allEntities.isEmpty()) {
            return generateFallbackWizardDeck(request.theme());
        }

        List<PokemonType> targetTypes = getTargetTypes(request.preferredTypes(), request.theme());
        List<PokemonCard> pokemonCards = applyGenerationFilter(mapPokemonCards(allEntities), request.generation());

        List<List<PokemonCard>> filteredLines = selectEvolutionLines(pokemonCards, targetTypes);
        if (filteredLines.isEmpty()) {
            return generateFallbackWizardDeck(request.theme());
        }

        List<DeckCardRequestDTO> deck = new ArrayList<>();
        int linesCount = request.evolutionLinesCount() != null && request.evolutionLinesCount() > 0
                ? request.evolutionLinesCount() : DEFAULT_EVOLUTION_LINES_COUNT;
        List<List<PokemonCard>> pickedLines = filteredLines.subList(0, Math.min(linesCount, filteredLines.size()));
        for (List<PokemonCard> line : pickedLines) {
            addEvolutionLine(deck, line.stream().map(PokemonCard::getCardId).toList());
        }

        pokemonCards.stream()
                .filter(p -> p.isEx() && targetTypes.contains(p.getPokemonType()))
                .findFirst()
                .ifPresent(exCard -> deck.add(new DeckCardRequestDTO(exCard.getCardId(), 2)));

        addCoreTrainers(deck);
        fillRemainingEnergy(deck, targetTypes);

        return deck;
    }

    // PMD AvoidCatchingGenericException: CardMapper.map() parses loosely-typed JSON
    // (Integer.parseInt, unchecked casts to String, Map lookups) and can throw
    // NumberFormatException, ClassCastException, IllegalArgumentException or NPE
    // depending on which field is malformed. Narrowing the catch would either miss
    // a failure mode or turn into an equally generic multi-catch, so this is a
    // deliberate exception to the rule, not an oversight.
    @SuppressWarnings("PMD.AvoidCatchingGenericException")
    private List<PokemonCard> mapPokemonCards(List<CardEntity> entities) {
        List<PokemonCard> pokemonCards = new ArrayList<>();
        for (CardEntity entity : entities) {
            String supertype = entity.getSupertype();
            boolean isPokemon = "Pokémon".equalsIgnoreCase(supertype) || (supertype != null && supertype.startsWith("Pok"));
            if (!isPokemon) {
                continue;
            }
            try {
                Card mapped = cardMapper.map(entity);
                if (mapped instanceof PokemonCard pc) {
                    pokemonCards.add(pc);
                }
            } catch (RuntimeException e) {
                log.warn("Skipping malformed card {} while building wizard deck: {}", entity.getId(), e.getMessage());
            }
        }
        return pokemonCards;
    }

    private List<PokemonCard> applyGenerationFilter(List<PokemonCard> pokemonCards, String generation) {
        if (generation == null || generation.isBlank() || "Cualquiera".equalsIgnoreCase(generation)) {
            return pokemonCards;
        }
        String genLower = generation.toLowerCase(Locale.ROOT);
        if (genLower.contains("6") || genLower.contains("xy")) {
            return pokemonCards.stream()
                    .filter(p -> p.getCardId() != null && p.getCardId().startsWith("xy"))
                    .collect(Collectors.toList());
        }
        if (genLower.contains("1") || genLower.contains("base")) {
            return pokemonCards.stream()
                    .filter(p -> p.getCardId() != null && (p.getCardId().startsWith("base") || p.getCardId().startsWith("xy")))
                    .collect(Collectors.toList());
        }
        return pokemonCards;
    }

    private List<List<PokemonCard>> selectEvolutionLines(List<PokemonCard> pokemonCards, List<PokemonType> targetTypes) {
        List<List<PokemonCard>> allEvolutionLines = buildEvolutionLines(pokemonCards);
        List<List<PokemonCard>> filteredLines = allEvolutionLines.stream()
                .filter(line -> !line.isEmpty() && targetTypes.contains(line.get(0).getPokemonType()))
                .collect(Collectors.toList());
        if (filteredLines.isEmpty()) {
            filteredLines = allEvolutionLines;
        }
        Collections.shuffle(filteredLines, new Random());
        return filteredLines;
    }

    private void fillRemainingEnergy(List<DeckCardRequestDTO> deck, List<PokemonType> targetTypes) {
        int currentSize = deck.stream().mapToInt(DeckCardRequestDTO::quantity).sum();
        int missing = DECK_SIZE - currentSize;
        if (missing <= 0) {
            return;
        }
        List<String> energyIds = targetTypes.stream().map(this::getEnergyId).distinct().toList();
        int energyPerType = missing / energyIds.size();
        int remainder = missing % energyIds.size();
        for (int i = 0; i < energyIds.size(); i++) {
            int qty = energyPerType + (i == 0 ? remainder : 0);
            addEnergy(deck, energyIds.get(i), qty);
        }
    }

    private void addEnergy(List<DeckCardRequestDTO> deck, String energyId, int qty) {
        if (qty <= 0) {
            return;
        }
        if (ENERGY_COLORLESS.equals(energyId)) {
            deck.add(new DeckCardRequestDTO(ENERGY_COLORLESS, Math.min(MAX_ENERGY_COPIES, qty)));
            if (qty > MAX_ENERGY_COPIES) {
                deck.add(new DeckCardRequestDTO(ENERGY_WATER, qty - MAX_ENERGY_COPIES));
            }
        } else {
            deck.add(new DeckCardRequestDTO(energyId, qty));
        }
    }

    private void addEvolutionLine(List<DeckCardRequestDTO> deck, List<String> cardIds) {
        for (int i = 0; i < cardIds.size() && i < EVOLUTION_LINE_COPIES.length; i++) {
            deck.add(new DeckCardRequestDTO(cardIds.get(i), EVOLUTION_LINE_COPIES[i]));
        }
    }

    private void addCoreTrainers(List<DeckCardRequestDTO> deck) {
        deck.add(new DeckCardRequestDTO("xy1-123", 4));
        deck.add(new DeckCardRequestDTO("xy1-127", 4));
        deck.add(new DeckCardRequestDTO("xy1-128", 4));
        deck.add(new DeckCardRequestDTO("xy1-125", 4));
        deck.add(new DeckCardRequestDTO("xy1-121", 3));
        deck.add(new DeckCardRequestDTO("xy1-124", 3));
    }

    private List<List<PokemonCard>> buildEvolutionLines(List<PokemonCard> pokemonCards) {
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
        List<List<PokemonCard>> lines = new ArrayList<>();

        // 1. Build Stage 2 lines: Basic -> Stage 1 -> Stage 2
        for (PokemonCard stage2 : stage2Cards) {
            PokemonCard stage1 = findByName(stage1Cards, stage2.getEvolvesFrom());
            PokemonCard basic = stage1 != null ? findByName(basicCards, stage1.getEvolvesFrom()) : null;
            if (stage1 != null && basic != null) {
                lines.add(List.of(basic, stage1, stage2));
                usedStage1.add(stage1);
                usedBasic.add(basic);
            }
        }

        // 2. Build Stage 1 lines: Basic -> Stage 1
        for (PokemonCard stage1 : stage1Cards) {
            if (usedStage1.contains(stage1)) {
                continue;
            }
            PokemonCard basic = findByName(basicCards, stage1.getEvolvesFrom());
            if (basic != null) {
                lines.add(List.of(basic, stage1));
                usedBasic.add(basic);
            }
        }

        // 3. Leftover basic cards
        for (PokemonCard basic : basicCards) {
            if (!usedBasic.contains(basic)) {
                lines.add(List.of(basic));
            }
        }

        return lines;
    }

    private PokemonCard findByName(List<PokemonCard> candidates, String name) {
        if (name == null) {
            return null;
        }
        return candidates.stream()
                .filter(p -> p.getName().equalsIgnoreCase(name))
                .findFirst()
                .orElse(null);
    }

    private List<PokemonType> getTargetTypes(List<String> preferredTypes, String theme) {
        List<PokemonType> targetTypes = new ArrayList<>();
        if (preferredTypes != null) {
            for (String t : preferredTypes) {
                addIfResolvable(targetTypes, t);
            }
        }
        if (targetTypes.isEmpty() && theme != null) {
            // handle "fire y water" joint format
            if (theme.contains(" y ")) {
                for (String t : theme.split(" y ")) {
                    addIfResolvable(targetTypes, t);
                }
            } else {
                addIfResolvable(targetTypes, theme);
            }
        }
        if (targetTypes.isEmpty()) {
            targetTypes.add(PokemonType.GRASS);
        }
        return targetTypes;
    }

    private void addIfResolvable(List<PokemonType> targetTypes, String theme) {
        PokemonType pt = parseThemeToType(theme);
        if (pt != null) {
            targetTypes.add(pt);
        }
    }

    private PokemonType parseThemeToType(String theme) {
        return theme == null ? null : THEME_TO_TYPE.get(theme.toLowerCase(Locale.ROOT).trim());
    }

    private String getEnergyId(PokemonType type) {
        return TYPE_TO_ENERGY.getOrDefault(type, ENERGY_GRASS);
    }

    private List<DeckCardRequestDTO> generateFallbackWizardDeck(String theme) {
        String normalizedTheme = theme != null ? theme.toLowerCase(Locale.ROOT) : DEFAULT_THEME;
        ThemeStarterDeck starter = FALLBACK_DECKS.getOrDefault(normalizedTheme, FALLBACK_DECKS.get(DEFAULT_THEME));

        List<DeckCardRequestDTO> deck = new ArrayList<>();
        List<List<String>> lines = new ArrayList<>(starter.evolutionLines());
        Collections.shuffle(lines, new Random());
        List<List<String>> pickedLines = lines.subList(0, Math.min(DEFAULT_EVOLUTION_LINES_COUNT, lines.size()));

        for (List<String> line : pickedLines) {
            addEvolutionLine(deck, line);
        }

        if (starter.exCard() != null) {
            deck.add(new DeckCardRequestDTO(starter.exCard(), 2));
        }

        addCoreTrainers(deck);

        int currentSize = deck.stream().mapToInt(DeckCardRequestDTO::quantity).sum();
        addEnergy(deck, starter.energyId(), DECK_SIZE - currentSize);

        return deck;
    }

    private static Map<String, ThemeStarterDeck> buildFallbackDecks() {
        Map<String, ThemeStarterDeck> decks = new HashMap<>();

        decks.put("fire", new ThemeStarterDeck(List.of(
                List.of("xy1-24", "xy1-25", "xy1-26"), // Fennekin
                List.of("xy1-20", "xy1-21"),           // Slugma
                List.of("xy1-22", "xy1-23"),           // Pansear
                List.of("xy1-27", "xy1-28")            // Fletchinder
        ), null, ENERGY_FIRE));

        decks.put("water", new ThemeStarterDeck(List.of(
                List.of("xy1-39", "xy1-40", "xy1-41"), // Froakie
                List.of("xy1-31", "xy1-32"),           // Shellder
                List.of("xy1-33", "xy1-34"),           // Staryu
                List.of("xy1-37", "xy1-38")            // Panpour
        ), "xy1-29", ENERGY_WATER)); // Blastoise-EX

        ThemeStarterDeck lightning = new ThemeStarterDeck(List.of(
                List.of("xy1-42", "xy1-43"), // Pikachu
                List.of("xy1-44", "xy1-45")  // Voltorb
        ), "xy1-46", ENERGY_LIGHTNING); // Emolga-EX
        decks.put("lightning", lightning);
        decks.put("electric", lightning);

        decks.put("psychic", new ThemeStarterDeck(List.of(
                List.of("xy1-47", "xy1-48"),           // Ekans
                List.of("xy1-49", "xy1-50"),           // Spoink
                List.of("xy1-51", "xy1-52", "xy1-53"), // Venipede
                List.of("xy1-54", "xy1-55"),           // Phantump
                List.of("xy1-56", "xy1-57")            // Pumpkaboo
        ), null, ENERGY_PSYCHIC));

        decks.put("fighting", new ThemeStarterDeck(List.of(
                List.of("xy1-58", "xy1-59"),           // Diglett
                List.of("xy1-60", "xy1-61", "xy1-62"), // Rhyhorn
                List.of("xy1-65", "xy1-66", "xy1-67")  // Timburr
        ), null, ENERGY_FIGHTING));

        ThemeStarterDeck darkness = new ThemeStarterDeck(List.of(
                List.of("xy1-69", "xy1-70", "xy1-71"), // Sandile
                List.of("xy1-72", "xy1-73"),           // Zorua
                List.of("xy1-74", "xy1-76")            // Inkay
        ), "xy1-79", ENERGY_DARKNESS); // Yveltal-EX
        decks.put("darkness", darkness);
        decks.put("dark", darkness);

        ThemeStarterDeck metal = new ThemeStarterDeck(List.of(
                List.of("xy1-81", "xy1-82"),          // Pawniard
                List.of("xy1-83", "xy1-84", "xy1-85") // Honedge
        ), "xy1-80", ENERGY_METAL); // Skarmory-EX
        decks.put("metal", metal);
        decks.put("steel", metal);

        decks.put("fairy", new ThemeStarterDeck(List.of(
                List.of("xy1-87", "xy1-89"), // Jigglypuff
                List.of("xy1-92", "xy1-93"), // Spritzee
                List.of("xy1-94", "xy1-95")  // Swirlix
        ), "xy1-97", ENERGY_FAIRY)); // Xerneas-EX

        ThemeStarterDeck colorless = new ThemeStarterDeck(List.of(
                List.of("xy1-98", "xy1-99"),             // Doduo
                List.of("xy1-102", "xy1-103"),           // Taillow
                List.of("xy1-104", "xy1-105"),           // Skitty
                List.of("xy1-106", "xy1-107"),           // Bidoof
                List.of("xy1-108", "xy1-109", "xy1-110"), // Lillipup
                List.of("xy1-111", "xy1-112")            // Bunnelby
        ), null, ENERGY_COLORLESS);
        decks.put("colorless", colorless);
        decks.put("normal", colorless);

        decks.put(DEFAULT_THEME, new ThemeStarterDeck(List.of(
                List.of("xy1-12", "xy1-13", "xy1-14"), // Chespin
                List.of("xy1-3", "xy1-4", "xy1-5"),    // Weedle
                List.of("xy1-10", "xy1-11")            // Pansage
        ), "xy1-1", ENERGY_GRASS));

        return decks;
    }
}
