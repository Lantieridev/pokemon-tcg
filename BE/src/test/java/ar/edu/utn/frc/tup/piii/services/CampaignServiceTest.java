package ar.edu.utn.frc.tup.piii.services;

import ar.edu.utn.frc.tup.piii.dtos.CampaignProgressResponseDTO;
import ar.edu.utn.frc.tup.piii.engine.model.Card;
import ar.edu.utn.frc.tup.piii.persistence.entity.CardEntity;
import ar.edu.utn.frc.tup.piii.persistence.entity.UserEntity;
import ar.edu.utn.frc.tup.piii.persistence.mapper.CardMapper;
import ar.edu.utn.frc.tup.piii.persistence.repository.CardRepository;
import ar.edu.utn.frc.tup.piii.persistence.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class CampaignServiceTest {

    private UserRepository userRepository;
    private CardRepository cardRepository;
    private CardMapper cardMapper;
    private MatchCreationService matchCreationService;
    private CardResolutionService cardResolutionService;

    private CampaignService campaignService;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        cardRepository = mock(CardRepository.class);
        cardMapper = mock(CardMapper.class);
        matchCreationService = mock(MatchCreationService.class);
        cardResolutionService = mock(CardResolutionService.class);

        campaignService = new CampaignService(
                userRepository, cardRepository, cardMapper, matchCreationService, cardResolutionService
        );
    }

    @Test
    void getCampaignProgress_whenUserNotFound_throwsNoSuchElementException() {
        when(userRepository.findByUsername("unknown")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> campaignService.getCampaignProgress("unknown"))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessageContaining("Usuario no encontrado: unknown");
    }

    @Test
    void getCampaignProgress_whenUserExists_returnsProgress() {
        Set<Integer> cleared = new HashSet<>(List.of(1)); // Brock completed
        UserEntity user = UserEntity.builder()
                .username("testUser")
                .clearedStoryNodes(cleared)
                .build();

        when(userRepository.findByUsername("testUser")).thenReturn(Optional.of(user));

        CampaignProgressResponseDTO result = campaignService.getCampaignProgress("testUser");

        assertThat(result.getClearedNodesCount()).isEqualTo(1);
        assertThat(result.getTotalNodesCount()).isEqualTo(8);
        assertThat(result.getNodes()).hasSize(8);
        
        // Node 1 (Brock) should be CLEARED
        assertThat(result.getNodes().get(0).getStatus()).isEqualTo("CLEARED");
        // Node 2 (Misty) should be UNLOCKED
        assertThat(result.getNodes().get(1).getStatus()).isEqualTo("UNLOCKED");
        // Node 3 (Lt. Surge) should be LOCKED
        assertThat(result.getNodes().get(2).getStatus()).isEqualTo("LOCKED");
    }

    @Test
    void iniciarDesafioPvE_whenUserNotFound_throwsNoSuchElementException() {
        when(userRepository.findByUsername("unknown")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> campaignService.iniciarDesafioPvE("unknown", 1, 10L))
                .isInstanceOf(NoSuchElementException.class);
    }

    @Test
    void iniciarDesafioPvE_whenNodeInvalid_throwsIllegalArgumentException() {
        UserEntity user = UserEntity.builder().username("testUser").build();
        when(userRepository.findByUsername("testUser")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> campaignService.iniciarDesafioPvE("testUser", 9, 10L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Nodo de campaña inválido: 9");
    }

    @Test
    void iniciarDesafioPvE_whenNodeLocked_throwsIllegalArgumentException() {
        UserEntity user = UserEntity.builder()
                .username("testUser")
                .clearedStoryNodes(new HashSet<>()) // empty -> node 2 is locked
                .build();
        when(userRepository.findByUsername("testUser")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> campaignService.iniciarDesafioPvE("testUser", 2, 10L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Este nodo de la campaña se encuentra bloqueado.");
    }

    @Test
    void iniciarDesafioPvE_whenPlayerDeckIsEmpty_throwsIllegalArgumentException() {
        UserEntity user = UserEntity.builder()
                .username("testUser")
                .clearedStoryNodes(new HashSet<>()) // node 1 is unlocked by default
                .build();
        when(userRepository.findByUsername("testUser")).thenReturn(Optional.of(user));
        when(cardResolutionService.resolveCards(10L)).thenReturn(List.of()); // Empty deck

        assertThatThrownBy(() -> campaignService.iniciarDesafioPvE("testUser", 1, 10L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("El mazo seleccionado está vacío o no es válido.");
    }

    @Test
    void iniciarDesafioPvE_whenSuccessful_createsMatchAndReturnsMatchId() {
        UserEntity user = UserEntity.builder()
                .username("testUser")
                .clearedStoryNodes(new HashSet<>())
                .build();
        when(userRepository.findByUsername("testUser")).thenReturn(Optional.of(user));

        // Player deck of 60 cards
        List<Card> playerDeck = new ArrayList<>();
        for (int i = 0; i < 60; i++) playerDeck.add(mock(Card.class));
        when(cardResolutionService.resolveCards(10L)).thenReturn(playerDeck);

        // Stub card loading for bot deck
        CardEntity fakeCardEntity = new CardEntity();
        Card fakeCard = mock(Card.class);
        when(cardRepository.findById(anyString())).thenReturn(Optional.of(fakeCardEntity));
        when(cardMapper.map(any())).thenReturn(fakeCard);

        when(matchCreationService.createMatch(eq("testUser"), eq("Bot-Brock"), eq(playerDeck), any(), eq(false)))
                .thenReturn("match-story-1");

        String result = campaignService.iniciarDesafioPvE("testUser", 1, 10L);

        assertThat(result).isEqualTo("match-story-1");
        verify(matchCreationService).createMatch(eq("testUser"), eq("Bot-Brock"), eq(playerDeck), any(), eq(false));
    }

    @Test
    void completeNode_whenNodeNotFound_throwsIllegalArgumentException() {
        assertThatThrownBy(() -> campaignService.completeNode("testUser", 9, "match-1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Nodo de campaña no encontrado: 9");
    }

    @Test
    void completeNode_whenNodeNotCleared_givesRewardsAndSaves() {
        UserEntity user = UserEntity.builder()
                .username("testUser")
                .clearedStoryNodes(new HashSet<>())
                .pokecoins(10)
                .xp(50)
                .build();
        when(userRepository.findByUsername("testUser")).thenReturn(Optional.of(user));

        campaignService.completeNode("testUser", 1, "match-1");

        assertThat(user.getClearedStoryNodes()).contains(1);
        assertThat(user.getPokecoins()).isEqualTo(60); // 10 + 50 reward
        assertThat(user.getXp()).isEqualTo(150); // 50 + 100 reward
        verify(userRepository).save(user);
    }

    @Test
    void completeNode_whenNodeAlreadyCleared_ignoresRewardsAndDoesNotSave() {
        UserEntity user = UserEntity.builder()
                .username("testUser")
                .clearedStoryNodes(new HashSet<>(List.of(1)))
                .pokecoins(10)
                .xp(50)
                .build();
        when(userRepository.findByUsername("testUser")).thenReturn(Optional.of(user));

        campaignService.completeNode("testUser", 1, "match-1");

        assertThat(user.getClearedStoryNodes()).containsOnly(1);
        assertThat(user.getPokecoins()).isEqualTo(10); // Unchanged
        assertThat(user.getXp()).isEqualTo(50); // Unchanged
        verify(userRepository, never()).save(any());
    }
}
