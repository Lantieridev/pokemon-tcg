package ar.edu.utn.frc.tup.piii.services;

import ar.edu.utn.frc.tup.piii.dtos.HonorType;
import ar.edu.utn.frc.tup.piii.dtos.UpdateProfileRequestDTO;
import ar.edu.utn.frc.tup.piii.dtos.UpdateShowcaseRequestDTO;
import ar.edu.utn.frc.tup.piii.dtos.UserProfileResponseDTO;
import ar.edu.utn.frc.tup.piii.persistence.entity.UserEntity;
import ar.edu.utn.frc.tup.piii.persistence.repository.CardRepository;
import ar.edu.utn.frc.tup.piii.persistence.repository.MatchRepository;
import ar.edu.utn.frc.tup.piii.persistence.repository.UserRepository;
import ar.edu.utn.frc.tup.piii.persistence.repository.UserShowcaseRepository;
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
    private ProfileService profileService;

    @BeforeEach
    public void setUp() {
        userRepository = mock(UserRepository.class);
        userShowcaseRepository = mock(UserShowcaseRepository.class);
        matchRepository = mock(MatchRepository.class);
        cardRepository = mock(CardRepository.class);
        honorService = mock(HonorService.class);

        profileService = new ProfileServiceImpl(
                userRepository,
                userShowcaseRepository,
                matchRepository,
                cardRepository,
                honorService
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
        profileService.awardXpAndCheckAchievements(1L, true);

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
        profileService.awardXpAndCheckAchievements(1L, false);

        verify(userRepository, times(1)).save(user);
        assertEquals(1, user.getLevel());
        assertEquals(35, user.getXp());
    }
}
