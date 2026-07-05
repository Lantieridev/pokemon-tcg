package ar.edu.utn.frc.tup.piii.services.deck;

import ar.edu.utn.frc.tup.piii.client.PokemonTcgApiClient;
import ar.edu.utn.frc.tup.piii.client.dto.PokemonTcgCardDTO;
import ar.edu.utn.frc.tup.piii.client.dto.PokemonTcgSetDTO;
import ar.edu.utn.frc.tup.piii.dtos.deck.DeckCardRequestDTO;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DeckServiceImplTest {

    private DeckRepository deckRepository;
    private CardRepository cardRepository;
    private UserRepository userRepository;
    private PokemonTcgApiClient apiClient;
    private DeckBuilderValidator validator;
    private DeckServiceImpl service;

    @BeforeEach
    void setUp() {
        deckRepository = mock(DeckRepository.class);
        cardRepository = mock(CardRepository.class);
        userRepository = mock(UserRepository.class);
        apiClient = mock(PokemonTcgApiClient.class);
        validator = mock(DeckBuilderValidator.class);
        service = new DeckServiceImpl(deckRepository, cardRepository, userRepository, apiClient, validator);
    }

    // --- getAll ---

    @Test
    void getAll_returnsEmptyListWhenNoDecks() {
        when(deckRepository.findAll()).thenReturn(List.of());

        final List<DeckSummaryDTO> result = service.getAll();

        assertTrue(result.isEmpty());
    }

    @Test
    void getAll_returnsMappedSummaries() {
        final DeckEntity deck = buildDeckEntity(1L, "Test Deck", List.of(
                buildDeckCard("xy1-1", "Bulbasaur", 4),
                buildDeckCard("xy1-2", "Ivysaur", 4)
        ));
        when(deckRepository.findAll()).thenReturn(List.of(deck));

        final List<DeckSummaryDTO> result = service.getAll();

        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).id());
        assertEquals("Test Deck", result.get(0).name());
        assertEquals(8, result.get(0).totalCards());
    }

    // --- getById ---

    @Test
    void getById_returnsDtoWhenFound() {
        final DeckEntity deck = buildDeckEntity(42L, "My Deck", List.of(
                buildDeckCard("xy1-1", "Bulbasaur", 4)
        ));
        when(deckRepository.findById(42L)).thenReturn(Optional.of(deck));

        final DeckResponseDTO result = service.getById(42L);

        assertEquals(42L, result.id());
        assertEquals("My Deck", result.name());
        assertEquals(1, result.cards().size());
        assertEquals("xy1-1", result.cards().get(0).cardId());
        assertEquals(4, result.cards().get(0).quantity());
    }

    @Test
    void getById_throwsNoSuchElementWhenNotFound() {
        when(deckRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class, () -> service.getById(99L));
    }

    // --- create ---

    @Test
    void create_throwsWhenUserNotFound() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        final DeckRequestDTO request = new DeckRequestDTO(1L, "My Deck", ar.edu.utn.frc.tup.piii.engine.model.DeckStatus.VALID, List.of());

        assertThrows(NoSuchElementException.class, () -> service.create(request));
        verify(validator, never()).validate(anyList(), any(), any(), any());
    }

    @Test
    void create_usesExistingCardFromDb() {
        final UserEntity user = UserEntity.builder().id(1L).username("player").build();
        final CardEntity card = buildCardEntity("xy1-1", "Bulbasaur", "Pokémon", "Basic");

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(cardRepository.findById("xy1-1")).thenReturn(Optional.of(card));

        final DeckEntity savedDeck = buildDeckEntity(10L, "My Deck", new ArrayList<>());
        when(deckRepository.save(any())).thenReturn(savedDeck);

        final DeckRequestDTO request = new DeckRequestDTO(1L, "My Deck", ar.edu.utn.frc.tup.piii.engine.model.DeckStatus.VALID,
                List.of(new DeckCardRequestDTO("xy1-1", 60)));

        service.create(request);

        verify(apiClient, never()).findById(anyString());
        verify(cardRepository, never()).save(any(CardEntity.class));
        verify(validator).validate(anyList(), any(), any(), any());
    }

    @Test
    void create_fetchesCardFromApiWhenNotInDb() {
        final UserEntity user = UserEntity.builder().id(1L).username("player").build();
        final PokemonTcgCardDTO apiCard = new PokemonTcgCardDTO(
                "xy1-1", "Bulbasaur", "Pokémon", List.of("Basic"), null,
                "60", null, null, null, null, null, null,
                new PokemonTcgSetDTO("xy1", "XY"));
        final CardEntity savedCard = buildCardEntity("xy1-1", "Bulbasaur", "Pokémon", "Basic");

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(cardRepository.findById("xy1-1")).thenReturn(Optional.empty());
        when(apiClient.findById("xy1-1")).thenReturn(Optional.of(apiCard));
        when(cardRepository.save(any(CardEntity.class))).thenReturn(savedCard);

        final DeckEntity savedDeck = buildDeckEntity(10L, "My Deck", new ArrayList<>());
        when(deckRepository.save(any(DeckEntity.class))).thenReturn(savedDeck);

        final DeckRequestDTO request = new DeckRequestDTO(1L, "My Deck", ar.edu.utn.frc.tup.piii.engine.model.DeckStatus.VALID,
                List.of(new DeckCardRequestDTO("xy1-1", 60)));

        service.create(request);

        verify(apiClient).findById("xy1-1");
        verify(cardRepository).save(any(CardEntity.class));
    }

    @Test
    void create_throwsWhenCardNotFoundInApi() {
        final UserEntity user = UserEntity.builder().id(1L).username("player").build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(cardRepository.findById("unknown-1")).thenReturn(Optional.empty());
        when(apiClient.findById("unknown-1")).thenReturn(Optional.empty());

        final DeckRequestDTO request = new DeckRequestDTO(1L, "My Deck", ar.edu.utn.frc.tup.piii.engine.model.DeckStatus.VALID,
                List.of(new DeckCardRequestDTO("unknown-1", 60)));

        assertThrows(NoSuchElementException.class, () -> service.create(request));
    }

    @Test
    void create_propagatesValidationException() {
        final UserEntity user = UserEntity.builder().id(1L).username("player").build();
        final CardEntity card = buildCardEntity("xy1-1", "Bulbasaur", "Pokémon", "Basic");

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(cardRepository.findById("xy1-1")).thenReturn(Optional.of(card));
        doThrow(new InvalidDeckException("deck has 59 cards, expected 60"))
                .when(validator).validate(anyList(), any(), any(), any());

        final DeckRequestDTO request = new DeckRequestDTO(1L, "My Deck", ar.edu.utn.frc.tup.piii.engine.model.DeckStatus.VALID,
                List.of(new DeckCardRequestDTO("xy1-1", 59)));

        assertThrows(InvalidDeckException.class, () -> service.create(request));
        verify(deckRepository, never()).save(any());
    }

    @Test
    void create_savesDeckAndReturnsMappedDto() {
        final UserEntity user = UserEntity.builder().id(1L).username("player").build();
        final CardEntity card = buildCardEntity("xy1-1", "Bulbasaur", "Pokémon", "Basic");

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(cardRepository.findById("xy1-1")).thenReturn(Optional.of(card));

        final DeckEntity firstSave = buildDeckEntity(7L, "My Deck", new ArrayList<>());
        final DeckCardEntity deckCard = buildDeckCard("xy1-1", "Bulbasaur", 60);
        deckCard.setDeck(firstSave);
        deckCard.setCard(card);
        deckCard.setQuantity(60);
        deckCard.setId(new DeckCardEntity.DeckCardId(7L, "xy1-1"));
        final DeckEntity secondSave = buildDeckEntity(7L, "My Deck", List.of(deckCard));

        when(deckRepository.save(any(DeckEntity.class)))
                .thenReturn(firstSave)
                .thenReturn(secondSave);

        final DeckRequestDTO request = new DeckRequestDTO(1L, "My Deck", ar.edu.utn.frc.tup.piii.engine.model.DeckStatus.VALID,
                List.of(new DeckCardRequestDTO("xy1-1", 60)));

        final DeckResponseDTO result = service.create(request);

        assertNotNull(result);
        assertEquals(7L, result.id());
        assertEquals("My Deck", result.name());
        verify(deckRepository, times(2)).save(any());
    }

    // --- helpers ---

    private static DeckEntity buildDeckEntity(final Long id, final String name,
                                               final List<DeckCardEntity> cards) {
        return DeckEntity.builder()
                .id(id)
                .name(name)
                .status(ar.edu.utn.frc.tup.piii.engine.model.DeckStatus.VALID)
                .createdAt(LocalDateTime.now())
                .cards(new ArrayList<>(cards))
                .build();
    }

    private static DeckCardEntity buildDeckCard(final String cardId, final String cardName, final int qty) {
        final CardEntity card = buildCardEntity(cardId, cardName, "Pokémon", "Basic");
        final DeckCardEntity dc = new DeckCardEntity();
        dc.setId(new DeckCardEntity.DeckCardId(1L, cardId));
        dc.setCard(card);
        dc.setQuantity(qty);
        return dc;
    }

    private static CardEntity buildCardEntity(final String id, final String name,
                                              final String supertype, final String subtype) {
        return CardEntity.builder()
                .id(id).name(name).supertype(supertype).subtype(subtype)
                .build();
    }
}
