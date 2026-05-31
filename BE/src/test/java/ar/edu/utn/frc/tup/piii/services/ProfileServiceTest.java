package ar.edu.utn.frc.tup.piii.services;

import ar.edu.utn.frc.tup.piii.dtos.HonorType;
import ar.edu.utn.frc.tup.piii.dtos.UpdateProfileRequestDTO;
import ar.edu.utn.frc.tup.piii.dtos.UpdateShowcaseRequestDTO;
import ar.edu.utn.frc.tup.piii.dtos.UserProfileResponseDTO;
import ar.edu.utn.frc.tup.piii.persistence.entity.UserEntity;
import ar.edu.utn.frc.tup.piii.persistence.entity.UserShowcaseEntity;
import ar.edu.utn.frc.tup.piii.persistence.entity.DeckEntity;
import ar.edu.utn.frc.tup.piii.persistence.entity.MatchEntity;
import ar.edu.utn.frc.tup.piii.persistence.repository.CardRepository;
import ar.edu.utn.frc.tup.piii.persistence.repository.MatchRepository;
import ar.edu.utn.frc.tup.piii.persistence.repository.UserRepository;
import ar.edu.utn.frc.tup.piii.persistence.repository.UserShowcaseRepository;
import ar.edu.utn.frc.tup.piii.persistence.repository.DeckRepository;
import ar.edu.utn.frc.tup.piii.services.impl.ProfileServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class ProfileServiceTest {

    private UserRepository userRepository;
    private UserShowcaseRepository userShowcaseRepository;
    private MatchRepository matchRepository;
    private CardRepository cardRepository;
    private HonorService honorService;
    private DeckRepository deckRepository;
    private ProfanityFilterService profanityFilterService;
    private ProfileService profileService;

    @BeforeEach
    public void setUp() {
        userRepository = mock(UserRepository.class);
        userShowcaseRepository = mock(UserShowcaseRepository.class);
        matchRepository = mock(MatchRepository.class);
        cardRepository = mock(CardRepository.class);
        honorService = mock(HonorService.class);
        deckRepository = mock(DeckRepository.class);
        profanityFilterService = mock(ProfanityFilterService.class);

        profileService = new ProfileServiceImpl(
                userRepository,
                userShowcaseRepository,
                matchRepository,
                cardRepository,
                honorService,
                deckRepository,
                profanityFilterService
        );
    }

    @Test
    public void testGetProfileSuccess() {
        final UserEntity user = UserEntity.builder()
                .id(1L)
                .username("lucas")
                .level(1)
                .xp(20)
                .avatarIcon("pikachu_icon")
                .description("Pro player")
                .activeTitle("Novato")
                .unlockedTitles(new HashSet<>(List.of("Novato", "Entrenador")))
                .build();

        when(userRepository.findByUsername("lucas")).thenReturn(Optional.of(user));
        when(matchRepository.findMatchesByUsername("lucas")).thenReturn(Collections.emptyList());
        when(userShowcaseRepository.findByUserUsernameOrderBySlotPositionAsc("lucas")).thenReturn(Collections.emptyList());

        final Map<HonorType, Integer> mockHonors = new HashMap<>();
        mockHonors.put(HonorType.GOOD_SPORTSMAN, 3);
        when(honorService.getHonors("lucas")).thenReturn(mockHonors);

        final UserProfileResponseDTO profile = profileService.getProfile("lucas");

        assertNotNull(profile);
        assertEquals("lucas", profile.getUsername());
        assertEquals("pikachu_icon", profile.getAvatarIcon());
        assertEquals("Pro player", profile.getDescription());
        assertEquals("Novato", profile.getActiveTitle());
        assertEquals(1, profile.getLevel());
        assertEquals(20, profile.getXp());
        assertEquals(100, profile.getXpToNextLevel());
        assertEquals(3, profile.getHonors().get(HonorType.GOOD_SPORTSMAN));
    }

    @Test
    public void testUpdateProfileCustomizations() {
        final UserEntity user = UserEntity.builder()
                .id(1L)
                .username("lucas")
                .level(1)
                .xp(10)
                .unlockedTitles(new HashSet<>(List.of("Novato", "Estratega")))
                .activeTitle("Novato")
                .build();

        when(userRepository.findByUsername("lucas")).thenReturn(Optional.of(user));

        final UpdateProfileRequestDTO request = UpdateProfileRequestDTO.builder()
                .avatarIcon("charizard_icon")
                .description("New bio")
                .activeTitle("Estratega")
                .build();

        profileService.updateProfile("lucas", request);

        verify(userRepository, times(1)).save(user);
        assertEquals("charizard_icon", user.getAvatarIcon());
        assertEquals("New bio", user.getDescription());
        assertEquals("Estratega", user.getActiveTitle());
    }

    @Test
    public void testAwardXpSubidaDeNivelTramos() {
        final UserEntity user = UserEntity.builder()
                .id(1L)
                .username("lucas")
                .level(1)
                .xp(80)
                .unlockedTitles(new HashSet<>())
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(matchRepository.findMatchesByUsername("lucas")).thenReturn(Collections.emptyList());
        when(honorService.getHonors("lucas")).thenReturn(Collections.emptyMap());

        // Gana partida (+50 XP) -> Total 130 XP -> Nivel 2 y queda con 30 XP
        profileService.awardXpAndCheckAchievements(1L, true, false, false, 0);

        verify(userRepository, times(1)).save(user);
        assertEquals(2, user.getLevel());
        assertEquals(30, user.getXp());
    }

    @Test
    public void testAwardXpDerrota() {
        final UserEntity user = UserEntity.builder()
                .id(1L)
                .username("lucas")
                .level(1)
                .xp(10)
                .unlockedTitles(new HashSet<>())
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(matchRepository.findMatchesByUsername("lucas")).thenReturn(Collections.emptyList());
        when(honorService.getHonors("lucas")).thenReturn(Collections.emptyMap());

        // Pierde partida (+25 XP) -> Total 35 XP -> Sigue nivel 1
        profileService.awardXpAndCheckAchievements(1L, false, false, false, 0);

        verify(userRepository, times(1)).save(user);
        assertEquals(1, user.getLevel());
        assertEquals(35, user.getXp());
    }

    @Test
    public void testUpdateProfileDescriptionSuccess() {
        final UserEntity user = UserEntity.builder()
                .id(1L)
                .username("lucas")
                .level(1)
                .xp(10)
                .unlockedTitles(new HashSet<>(List.of("Novato")))
                .activeTitle("Novato")
                .build();

        when(userRepository.findByUsername("lucas")).thenReturn(Optional.of(user));
        when(profanityFilterService.getProfaneWords("Valid bio")).thenReturn(Collections.emptyList());

        final UpdateProfileRequestDTO request = UpdateProfileRequestDTO.builder()
                .description("Valid bio")
                .build();

        profileService.updateProfile("lucas", request);

        verify(userRepository, times(1)).save(user);
        assertEquals("Valid bio", user.getDescription());
    }

    @Test
    public void testUpdateProfileDescriptionTooLong() {
        final UserEntity user = UserEntity.builder()
                .id(1L)
                .username("lucas")
                .build();

        when(userRepository.findByUsername("lucas")).thenReturn(Optional.of(user));

        final String longDesc = "a".repeat(151);
        final UpdateProfileRequestDTO request = UpdateProfileRequestDTO.builder()
                .description(longDesc)
                .build();

        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class, () -> {
            profileService.updateProfile("lucas", request);
        });
    }

    @Test
    public void testUpdateProfileDescriptionProfane() {
        final UserEntity user = UserEntity.builder()
                .id(1L)
                .username("lucas")
                .build();

        when(userRepository.findByUsername("lucas")).thenReturn(Optional.of(user));
        when(profanityFilterService.getProfaneWords("you loser")).thenReturn(List.of("loser"));

        final UpdateProfileRequestDTO request = UpdateProfileRequestDTO.builder()
                .description("you loser")
                .build();

        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class, () -> {
            profileService.updateProfile("lucas", request);
        });
    }

    @Test
    public void testUpdateShowcaseSuccess() {
        final UserEntity user = UserEntity.builder()
                .id(1L)
                .username("lucas")
                .build();

        when(userRepository.findByUsername("lucas")).thenReturn(Optional.of(user));
        when(cardRepository.existsById("XY1_001")).thenReturn(true);
        when(deckRepository.existsByUserIdAndCardId(1L, "XY1_001")).thenReturn(true);
        when(userShowcaseRepository.findByUserAndSlotPosition(user, 1)).thenReturn(Optional.empty());

        final UpdateShowcaseRequestDTO request = UpdateShowcaseRequestDTO.builder()
                .slots(List.of(UpdateShowcaseRequestDTO.ShowcaseSlot.builder()
                        .slotPosition(1)
                        .cardId("XY1_001")
                        .build()))
                .build();

        profileService.updateShowcase("lucas", request);

        verify(userShowcaseRepository, times(1)).save(any(UserShowcaseEntity.class));
    }

    @Test
    public void testUpdateShowcaseCardNotExist() {
        final UserEntity user = UserEntity.builder()
                .id(1L)
                .username("lucas")
                .build();

        when(userRepository.findByUsername("lucas")).thenReturn(Optional.of(user));
        when(cardRepository.existsById("XY1_001")).thenReturn(false);

        final UpdateShowcaseRequestDTO request = UpdateShowcaseRequestDTO.builder()
                .slots(List.of(UpdateShowcaseRequestDTO.ShowcaseSlot.builder()
                        .slotPosition(1)
                        .cardId("XY1_001")
                        .build()))
                .build();

        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class, () -> {
            profileService.updateShowcase("lucas", request);
        });
    }

    @Test
    public void testUpdateShowcaseCardNotOwned() {
        final UserEntity user = UserEntity.builder()
                .id(1L)
                .username("lucas")
                .build();

        when(userRepository.findByUsername("lucas")).thenReturn(Optional.of(user));
        when(cardRepository.existsById("XY1_001")).thenReturn(true);
        when(deckRepository.existsByUserIdAndCardId(1L, "XY1_001")).thenReturn(false);

        final UpdateShowcaseRequestDTO request = UpdateShowcaseRequestDTO.builder()
                .slots(List.of(UpdateShowcaseRequestDTO.ShowcaseSlot.builder()
                        .slotPosition(1)
                        .cardId("XY1_001")
                        .build()))
                .build();

        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class, () -> {
            profileService.updateShowcase("lucas", request);
        });
    }

    @Test
    public void testUpdateShowcaseDeckSuccess() {
        final UserEntity user = UserEntity.builder()
                .id(1L)
                .username("lucas")
                .build();
        final DeckEntity deck = DeckEntity.builder()
                .id(100L)
                .user(user)
                .name("My Fire Deck")
                .build();

        when(userRepository.findByUsername("lucas")).thenReturn(Optional.of(user));
        when(deckRepository.findById(100L)).thenReturn(Optional.of(deck));

        profileService.updateShowcaseDeck("lucas", 100L);

        verify(userRepository, times(1)).save(user);
        assertEquals(deck, user.getShowcasedDeck());
    }

    @Test
    public void testUpdateShowcaseDeckNullSuccess() {
        final UserEntity user = UserEntity.builder()
                .id(1L)
                .username("lucas")
                .showcasedDeck(DeckEntity.builder().id(100L).build())
                .build();

        when(userRepository.findByUsername("lucas")).thenReturn(Optional.of(user));

        profileService.updateShowcaseDeck("lucas", null);

        verify(userRepository, times(1)).save(user);
        org.junit.jupiter.api.Assertions.assertNull(user.getShowcasedDeck());
    }

    @Test
    public void testUpdateShowcaseDeckNotOwned() {
        final UserEntity user = UserEntity.builder()
                .id(1L)
                .username("lucas")
                .build();
        final UserEntity otherUser = UserEntity.builder()
                .id(2L)
                .username("other")
                .build();
        final DeckEntity deck = DeckEntity.builder()
                .id(100L)
                .user(otherUser)
                .name("Other's Deck")
                .build();

        when(userRepository.findByUsername("lucas")).thenReturn(Optional.of(user));
        when(deckRepository.findById(100L)).thenReturn(Optional.of(deck));

        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class, () -> {
            profileService.updateShowcaseDeck("lucas", 100L);
        });
    }

    @Test
    public void testCheckAndUnlockTitlesVariousMilestones() {
        final UserEntity user = UserEntity.builder()
                .id(1L)
                .username("lucas")
                .level(10)
                .xp(10)
                .unlockedTitles(new HashSet<>())
                .build();

        final List<ar.edu.utn.frc.tup.piii.dtos.MatchHistoryProjectionDto> matches = new java.util.ArrayList<>();
        for (int i = 0; i < 50; i++) {
            matches.add(new ar.edu.utn.frc.tup.piii.dtos.MatchHistoryProjectionDto(
                    (long) i, "FINISHED", "lucas", "other", "lucas", java.time.LocalDateTime.now()
            ));
        }
        for (int i = 50; i < 100; i++) {
            matches.add(new ar.edu.utn.frc.tup.piii.dtos.MatchHistoryProjectionDto(
                    (long) i, "FINISHED", "lucas", "other", "other", java.time.LocalDateTime.now()
            ));
        }

        when(userRepository.findByUsername("lucas")).thenReturn(Optional.of(user));
        when(matchRepository.findMatchesByUsername("lucas")).thenReturn(matches);
        when(userShowcaseRepository.findByUserUsernameOrderBySlotPositionAsc("lucas")).thenReturn(Collections.emptyList());
        when(honorService.getHonors("lucas")).thenReturn(Collections.emptyMap());
        when(deckRepository.countUniqueCardsByUserId(1L)).thenReturn(100);

        final UserProfileResponseDTO response = profileService.getProfile("lucas");

        assertNotNull(response);
        final Set<String> titles = response.getUnlockedTitles();
        assertNotNull(titles);

        org.junit.jupiter.api.Assertions.assertTrue(titles.contains("Novato"));
        org.junit.jupiter.api.Assertions.assertTrue(titles.contains("Entrenador"));
        org.junit.jupiter.api.Assertions.assertTrue(titles.contains("Estratega en Crecimiento"));
        org.junit.jupiter.api.Assertions.assertTrue(titles.contains("Maestro de Cartas"));

        org.junit.jupiter.api.Assertions.assertTrue(titles.contains("Ganador Prometedor"));
        org.junit.jupiter.api.Assertions.assertTrue(titles.contains("Ganador Implacable"));
        org.junit.jupiter.api.Assertions.assertTrue(titles.contains("Campeón del Tablero"));
        org.junit.jupiter.api.Assertions.assertFalse(titles.contains("Leyenda del Tablero"));

        org.junit.jupiter.api.Assertions.assertTrue(titles.contains("Combatiente"));
        org.junit.jupiter.api.Assertions.assertTrue(titles.contains("Combatiente Tenaz"));
        org.junit.jupiter.api.Assertions.assertTrue(titles.contains("Veterano de Batallas"));
        org.junit.jupiter.api.Assertions.assertTrue(titles.contains("Leyenda de Batallas"));

        org.junit.jupiter.api.Assertions.assertTrue(titles.contains("Coleccionista Novato"));
        org.junit.jupiter.api.Assertions.assertTrue(titles.contains("Coleccionista Experto"));
        org.junit.jupiter.api.Assertions.assertTrue(titles.contains("Coleccionista de Élite"));
        org.junit.jupiter.api.Assertions.assertFalse(titles.contains("Maestro Coleccionista"));
    }

    @Test
    public void testExtendedStatsAndAchievementsProgress() {
        final UserEntity user = UserEntity.builder()
                .id(1L)
                .username("lucas")
                .level(3)
                .xp(10)
                .perfectWins(2)
                .comebackWins(1)
                .totalKos(5)
                .trainerCardsPlayed(12)
                .totalDamageDealt(450)
                .unlockedTitles(new HashSet<>(List.of("Novato", "Entrenador")))
                .build();

        when(userRepository.findByUsername("lucas")).thenReturn(Optional.of(user));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(matchRepository.findMatchesByUsername("lucas")).thenReturn(Collections.emptyList());
        when(honorService.getHonors("lucas")).thenReturn(Collections.emptyMap());
        when(deckRepository.countUniqueCardsByUserId(1L)).thenReturn(10);

        // Test tracking damage and trainer cards
        profileService.trackDamageDealt("lucas", 50);
        profileService.trackTrainerCardPlayed("lucas");

        verify(userRepository, times(2)).save(user);
        assertEquals(500, user.getTotalDamageDealt());
        assertEquals(13, user.getTrainerCardsPlayed());

        // Test award XP with match stats
        profileService.awardXpAndCheckAchievements(1L, true, true, true, 3);
        assertEquals(3, user.getPerfectWins());
        assertEquals(2, user.getComebackWins());
        assertEquals(8, user.getTotalKos());

        // Test achievement progress DTOs
        final List<ar.edu.utn.frc.tup.piii.dtos.UserAchievementProgressDTO> progress = profileService.getAchievementsProgress("lucas");
        assertNotNull(progress);
        assertEquals(17, progress.size());

        final ar.edu.utn.frc.tup.piii.dtos.UserAchievementProgressDTO novato = progress.stream()
                .filter(p -> p.getTitle().equals("Novato"))
                .findFirst()
                .orElse(null);
        assertNotNull(novato);
        org.junit.jupiter.api.Assertions.assertTrue(novato.getUnlocked());
        assertEquals(1, novato.getProgress());
        assertEquals(1, novato.getTarget());

        final ar.edu.utn.frc.tup.piii.dtos.UserAchievementProgressDTO estratega = progress.stream()
                .filter(p -> p.getTitle().equals("Estratega en Crecimiento"))
                .findFirst()
                .orElse(null);
        assertNotNull(estratega);
        org.junit.jupiter.api.Assertions.assertFalse(estratega.getUnlocked());
        assertEquals(3, estratega.getProgress());
        assertEquals(5, estratega.getTarget());
    }
}
