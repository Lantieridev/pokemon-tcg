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
import org.springframework.security.access.AccessDeniedException;

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

    // --- getByUsername ---

    @Test
    void getByUsername_returnsEmptyListWhenNoDecks() {
        final UserEntity user = UserEntity.builder().id(1L).username("player").build();
        when(userRepository.findFirstByUsername("player")).thenReturn(Optional.of(user));
        when(deckRepository.findByUserId(1L)).thenReturn(List.of());

        final List<DeckSummaryDTO> result = service.getByUsername("player");

        assertTrue(result.isEmpty());
    }

    @Test
    void getByUsername_returnsMappedSummariesForThatUserOnly() {
        final UserEntity user = UserEntity.builder().id(1L).username("player").build();
        when(userRepository.findFirstByUsername("player")).thenReturn(Optional.of(user));
        final DeckEntity deck = buildDeckEntity(1L, "Test Deck", List.of(
                buildDeckCard("xy1-1", "Bulbasaur", 4),
                buildDeckCard("xy1-2", "Ivysaur", 4)
        ));
        when(deckRepository.findByUserId(1L)).thenReturn(List.of(deck));

        final List<DeckSummaryDTO> result = service.getByUsername("player");

        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).id());
        assertEquals("Test Deck", result.get(0).name());
        assertEquals(8, result.get(0).totalCards());
    }

    @Test
    void getByUsername_throwsWhenUserNotFound() {
        when(userRepository.findFirstByUsername("ghost")).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class, () -> service.getByUsername("ghost"));
    }

    // --- getById ---

    @Test
    void getById_returnsDtoWhenOwnerMatches() {
        final DeckEntity deck = buildOwnedDeckEntity(42L, "My Deck", "player", List.of(
                buildDeckCard("xy1-1", "Bulbasaur", 4)
        ));
        when(deckRepository.findById(42L)).thenReturn(Optional.of(deck));

        final DeckResponseDTO result = service.getById(42L, "player");

        assertEquals(42L, result.id());
        assertEquals("My Deck", result.name());
        assertEquals(1, result.cards().size());
        assertEquals("xy1-1", result.cards().get(0).cardId());
        assertEquals(4, result.cards().get(0).quantity());
    }

    @Test
    void getById_throwsNoSuchElementWhenNotFound() {
        when(deckRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class, () -> service.getById(99L, "player"));
    }

    @Test
    void getById_throwsAccessDeniedWhenRequesterIsNotOwner() {
        final DeckEntity deck = buildOwnedDeckEntity(42L, "My Deck", "player", List.of());
        when(deckRepository.findById(42L)).thenReturn(Optional.of(deck));

        assertThrows(AccessDeniedException.class, () -> service.getById(42L, "attacker"));
    }

    // --- create ---

    @Test
    void create_throwsWhenUserNotFound() {
        when(userRepository.findFirstByUsername("ghost")).thenReturn(Optional.empty());

        final DeckRequestDTO request = new DeckRequestDTO("My Deck", ar.edu.utn.frc.tup.piii.engine.model.DeckStatus.VALID, List.of());

        assertThrows(NoSuchElementException.class, () -> service.create(request, "ghost"));
        verify(validator, never()).validate(anyList(), any(), any(), any());
    }

    @Test
    void create_derivesOwnerFromRequestingUsername_ignoringAnyClientSuppliedId() {
        final UserEntity user = UserEntity.builder().id(1L).username("player").build();
        final CardEntity card = buildCardEntity("xy1-1", "Bulbasaur", "Pokémon", "Basic");

        when(userRepository.findFirstByUsername("player")).thenReturn(Optional.of(user));
        when(cardRepository.findById("xy1-1")).thenReturn(Optional.of(card));

        final DeckEntity savedDeck = buildDeckEntity(10L, "My Deck", new ArrayList<>());
        when(deckRepository.save(any())).thenReturn(savedDeck);

        final DeckRequestDTO request = new DeckRequestDTO("My Deck", ar.edu.utn.frc.tup.piii.engine.model.DeckStatus.VALID,
                List.of(new DeckCardRequestDTO("xy1-1", 60)));

        service.create(request, "player");

        // Only the requesting user's identity is ever looked up — there is no
        // client-controlled owner field on DeckRequestDTO to trust or ignore.
        verify(userRepository).findFirstByUsername("player");
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

        when(userRepository.findFirstByUsername("player")).thenReturn(Optional.of(user));
        when(cardRepository.findById("xy1-1")).thenReturn(Optional.empty());
        when(apiClient.findById("xy1-1")).thenReturn(Optional.of(apiCard));
        when(cardRepository.save(any(CardEntity.class))).thenReturn(savedCard);

        final DeckEntity savedDeck = buildDeckEntity(10L, "My Deck", new ArrayList<>());
        when(deckRepository.save(any(DeckEntity.class))).thenReturn(savedDeck);

        final DeckRequestDTO request = new DeckRequestDTO("My Deck", ar.edu.utn.frc.tup.piii.engine.model.DeckStatus.VALID,
                List.of(new DeckCardRequestDTO("xy1-1", 60)));

        service.create(request, "player");

        verify(apiClient).findById("xy1-1");
        verify(cardRepository).save(any(CardEntity.class));
    }

    @Test
    void create_throwsWhenCardNotFoundInApi() {
        final UserEntity user = UserEntity.builder().id(1L).username("player").build();

        when(userRepository.findFirstByUsername("player")).thenReturn(Optional.of(user));
        when(cardRepository.findById("unknown-1")).thenReturn(Optional.empty());
        when(apiClient.findById("unknown-1")).thenReturn(Optional.empty());

        final DeckRequestDTO request = new DeckRequestDTO("My Deck", ar.edu.utn.frc.tup.piii.engine.model.DeckStatus.VALID,
                List.of(new DeckCardRequestDTO("unknown-1", 60)));

        assertThrows(NoSuchElementException.class, () -> service.create(request, "player"));
    }

    @Test
    void create_propagatesValidationException() {
        final UserEntity user = UserEntity.builder().id(1L).username("player").build();
        final CardEntity card = buildCardEntity("xy1-1", "Bulbasaur", "Pokémon", "Basic");

        when(userRepository.findFirstByUsername("player")).thenReturn(Optional.of(user));
        when(cardRepository.findById("xy1-1")).thenReturn(Optional.of(card));
        doThrow(new InvalidDeckException("deck has 59 cards, expected 60"))
                .when(validator).validate(anyList(), any(), any(), any());

        final DeckRequestDTO request = new DeckRequestDTO("My Deck", ar.edu.utn.frc.tup.piii.engine.model.DeckStatus.VALID,
                List.of(new DeckCardRequestDTO("xy1-1", 59)));

        assertThrows(InvalidDeckException.class, () -> service.create(request, "player"));
        verify(deckRepository, never()).save(any());
    }

    @Test
    void create_savesDeckAndReturnsMappedDto() {
        final UserEntity user = UserEntity.builder().id(1L).username("player").build();
        final CardEntity card = buildCardEntity("xy1-1", "Bulbasaur", "Pokémon", "Basic");

        when(userRepository.findFirstByUsername("player")).thenReturn(Optional.of(user));
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

        final DeckRequestDTO request = new DeckRequestDTO("My Deck", ar.edu.utn.frc.tup.piii.engine.model.DeckStatus.VALID,
                List.of(new DeckCardRequestDTO("xy1-1", 60)));

        final DeckResponseDTO result = service.create(request, "player");

        assertNotNull(result);
        assertEquals(7L, result.id());
        assertEquals("My Deck", result.name());
        verify(deckRepository, times(2)).save(any());
    }

    // --- update ---

    @Test
    void update_throwsAccessDeniedWhenRequesterIsNotOwner() {
        final DeckEntity deck = buildOwnedDeckEntity(42L, "My Deck", "player", List.of());
        when(deckRepository.findById(42L)).thenReturn(Optional.of(deck));

        final DeckRequestDTO request = new DeckRequestDTO("My Deck", ar.edu.utn.frc.tup.piii.engine.model.DeckStatus.VALID, List.of());

        assertThrows(AccessDeniedException.class, () -> service.update(42L, request, "attacker"));
        verify(deckRepository, never()).save(any());
    }

    @Test
    void update_throwsNoSuchElementWhenNotFound() {
        when(deckRepository.findById(99L)).thenReturn(Optional.empty());

        final DeckRequestDTO request = new DeckRequestDTO("My Deck", ar.edu.utn.frc.tup.piii.engine.model.DeckStatus.VALID, List.of());

        assertThrows(NoSuchElementException.class, () -> service.update(99L, request, "player"));
    }

    @Test
    void update_replacesCardsAndReturnsMappedDtoWhenOwnerMatches() {
        final DeckEntity deck = buildOwnedDeckEntity(42L, "Old Name", "player", new ArrayList<>(List.of(
                buildDeckCard("xy1-1", "Bulbasaur", 60))));
        when(deckRepository.findById(42L)).thenReturn(Optional.of(deck));

        final CardEntity newCard = buildCardEntity("xy1-2", "Ivysaur", "Pokémon", "Stage1");
        when(cardRepository.findById("xy1-2")).thenReturn(Optional.of(newCard));
        when(deckRepository.save(any(DeckEntity.class))).thenReturn(deck);

        final DeckRequestDTO request = new DeckRequestDTO("New Name", ar.edu.utn.frc.tup.piii.engine.model.DeckStatus.VALID,
                List.of(new DeckCardRequestDTO("xy1-2", 60)));

        final DeckResponseDTO result = service.update(42L, request, "player");

        assertEquals("New Name", result.name());
        assertEquals(1, deck.getCards().size());
        assertEquals("xy1-2", deck.getCards().get(0).getCard().getId());
        verify(deckRepository).save(deck);
    }

    // --- delete ---

    @Test
    void delete_throwsAccessDeniedWhenRequesterIsNotOwner() {
        final DeckEntity deck = buildOwnedDeckEntity(42L, "My Deck", "player", List.of());
        when(deckRepository.findById(42L)).thenReturn(Optional.of(deck));

        assertThrows(AccessDeniedException.class, () -> service.delete(42L, "attacker"));
        verify(deckRepository, never()).delete(any());
    }

    @Test
    void delete_throwsNoSuchElementWhenNotFound() {
        when(deckRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class, () -> service.delete(99L, "player"));
    }

    @Test
    void delete_removesDeckWhenOwnerMatches() {
        final DeckEntity deck = buildOwnedDeckEntity(42L, "My Deck", "player", List.of());
        when(deckRepository.findById(42L)).thenReturn(Optional.of(deck));

        service.delete(42L, "player");

        verify(deckRepository).delete(deck);
    }

    // --- helpers ---

    private static DeckEntity buildDeckEntity(final Long id, final String name,
                                               final List<DeckCardEntity> cards) {
        return buildOwnedDeckEntity(id, name, "player", cards);
    }

    private static DeckEntity buildOwnedDeckEntity(final Long id, final String name, final String ownerUsername,
                                                    final List<DeckCardEntity> cards) {
        return DeckEntity.builder()
                .id(id)
                .user(UserEntity.builder().id(1L).username(ownerUsername).build())
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
