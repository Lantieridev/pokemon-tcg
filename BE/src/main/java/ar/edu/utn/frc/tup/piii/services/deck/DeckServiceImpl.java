package ar.edu.utn.frc.tup.piii.services.deck;

import ar.edu.utn.frc.tup.piii.client.PokemonTcgApiClient;
import ar.edu.utn.frc.tup.piii.client.dto.PokemonTcgCardDTO;
import ar.edu.utn.frc.tup.piii.dtos.deck.DeckCardRequestDTO;
import ar.edu.utn.frc.tup.piii.dtos.deck.DeckCardResponseDTO;
import ar.edu.utn.frc.tup.piii.dtos.deck.DeckRequestDTO;
import ar.edu.utn.frc.tup.piii.dtos.deck.DeckResponseDTO;
import ar.edu.utn.frc.tup.piii.dtos.deck.DeckSummaryDTO;
import ar.edu.utn.frc.tup.piii.persistence.entity.CardEntity;
import ar.edu.utn.frc.tup.piii.persistence.entity.DeckCardEntity;
import ar.edu.utn.frc.tup.piii.persistence.entity.DeckEntity;
import ar.edu.utn.frc.tup.piii.persistence.entity.UserEntity;
import ar.edu.utn.frc.tup.piii.persistence.repository.CardRepository;
import ar.edu.utn.frc.tup.piii.persistence.repository.DeckRepository;
import ar.edu.utn.frc.tup.piii.persistence.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class DeckServiceImpl implements DeckService {

    private final DeckRepository deckRepository;
    private final CardRepository cardRepository;
    private final UserRepository userRepository;
    private final PokemonTcgApiClient apiClient;
    private final DeckBuilderValidator validator;

    public DeckServiceImpl(final DeckRepository deckRepository,
                           final CardRepository cardRepository,
                           final UserRepository userRepository,
                           final PokemonTcgApiClient apiClient,
                           final DeckBuilderValidator validator) {
        this.deckRepository = Objects.requireNonNull(deckRepository);
        this.cardRepository = Objects.requireNonNull(cardRepository);
        this.userRepository = Objects.requireNonNull(userRepository);
        this.apiClient = Objects.requireNonNull(apiClient);
        this.validator = Objects.requireNonNull(validator);
    }

    @Override
    public List<DeckSummaryDTO> getAll() {
        return deckRepository.findAll().stream()
                .map(this::toSummaryDTO)
                .toList();
    }

    @Override
    public DeckResponseDTO getById(final Long id) {
        return deckRepository.findById(id)
                .map(this::toResponseDTO)
                .orElseThrow(() -> new NoSuchElementException("Deck not found: " + id));
    }

    @Override
    @Transactional
    public DeckResponseDTO create(final DeckRequestDTO request) {
        final UserEntity user = userRepository.findById(request.userId())
                .orElseThrow(() -> new NoSuchElementException("User not found: " + request.userId()));

        final Map<String, CardEntity> cardMap = request.cards().stream()
                .map(c -> findOrFetchCard(c.cardId()))
                .collect(Collectors.toMap(CardEntity::getId, Function.identity()));

        final List<DeckEntry> entries = request.cards().stream()
                .map(c -> toEntry(cardMap.get(c.cardId()), c.quantity()))
                .toList();

        validator.validate(entries);

        final DeckEntity deck = deckRepository.save(DeckEntity.builder()
                .user(user)
                .name(request.name())
                .cards(new ArrayList<>())
                .build());

        final List<DeckCardEntity> deckCards = request.cards().stream()
                .map(c -> {
                    final CardEntity card = cardMap.get(c.cardId());
                    return DeckCardEntity.builder()
                            .id(new DeckCardEntity.DeckCardId(deck.getId(), card.getId()))
                            .deck(deck)
                            .card(card)
                            .quantity(c.quantity())
                            .build();
                })
                .toList();

        deck.getCards().addAll(deckCards);
        return toResponseDTO(deckRepository.save(deck));
    }

    private CardEntity findOrFetchCard(final String cardId) {
        return cardRepository.findById(cardId)
                .orElseGet(() -> {
                    final PokemonTcgCardDTO dto = apiClient.findById(cardId)
                            .orElseThrow(() -> new NoSuchElementException("Card not found in API: " + cardId));
                    return cardRepository.save(toCardEntity(dto));
                });
    }

    private CardEntity toCardEntity(final PokemonTcgCardDTO dto) {
        return CardEntity.builder()
                .id(dto.id())
                .name(dto.name())
                .supertype(dto.supertype())
                .subtype(dto.subtypes() != null ? String.join(", ", dto.subtypes()) : null)
                .hp(parseHp(dto.hp()))
                .rules(dto.rules())
                .attacks(dto.attacks())
                .weaknesses(dto.weaknesses())
                .resistances(dto.resistances())
                .retreatCost(dto.retreatCost())
                .setId(dto.set() != null ? dto.set().id() : "unknown")
                .build();
    }

    private Integer parseHp(final String hp) {
        if (hp == null || hp.isBlank()) {
            return null;
        }
        final String digits = hp.replaceAll("[^0-9]", "");
        return digits.isEmpty() ? null : Integer.parseInt(digits);
    }

    private DeckEntry toEntry(final CardEntity card, final int quantity) {
        return new DeckEntry(
                card.getId(),
                card.getName(),
                card.getSupertype(),
                card.getSubtype(),
                extractRules(card.getRules()),
                quantity);
    }

    @SuppressWarnings("unchecked")
    private List<String> extractRules(final Object raw) {
        if (raw == null) {
            return List.of();
        }
        if (raw instanceof List<?> list) {
            return list.stream()
                    .filter(item -> item instanceof String)
                    .map(item -> (String) item)
                    .toList();
        }
        return List.of();
    }

    private DeckSummaryDTO toSummaryDTO(final DeckEntity deck) {
        final int total = deck.getCards().stream()
                .mapToInt(DeckCardEntity::getQuantity)
                .sum();
        return new DeckSummaryDTO(deck.getId(), deck.getName(), deck.getCreatedAt(), total);
    }

    private DeckResponseDTO toResponseDTO(final DeckEntity deck) {
        final List<DeckCardResponseDTO> cards = deck.getCards().stream()
                .map(dc -> new DeckCardResponseDTO(
                        dc.getCard().getId(),
                        dc.getCard().getName(),
                        dc.getCard().getSupertype(),
                        dc.getCard().getSubtype(),
                        dc.getQuantity()))
                .toList();
        return new DeckResponseDTO(deck.getId(), deck.getName(), deck.getCreatedAt(), cards);
    }
}
