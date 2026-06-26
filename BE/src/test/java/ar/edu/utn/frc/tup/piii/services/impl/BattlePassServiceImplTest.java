package ar.edu.utn.frc.tup.piii.services.impl;

import ar.edu.utn.frc.tup.piii.dtos.BattlePassStatusDTO;
import ar.edu.utn.frc.tup.piii.persistence.entity.BattlePassLevelEntity;
import ar.edu.utn.frc.tup.piii.persistence.entity.UserBattlePassEntity;
import ar.edu.utn.frc.tup.piii.persistence.entity.UserEntity;
import ar.edu.utn.frc.tup.piii.persistence.repository.BattlePassLevelRepository;
import ar.edu.utn.frc.tup.piii.persistence.repository.UserBattlePassRepository;
import ar.edu.utn.frc.tup.piii.persistence.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.domain.Sort;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class BattlePassServiceImplTest {

    private UserRepository userRepository;
    private UserBattlePassRepository userBattlePassRepository;
    private BattlePassLevelRepository battlePassLevelRepository;
    private BattlePassServiceImpl battlePassService;

    @BeforeEach
    public void setUp() {
        userRepository = mock(UserRepository.class);
        userBattlePassRepository = mock(UserBattlePassRepository.class);
        battlePassLevelRepository = mock(BattlePassLevelRepository.class);
        battlePassService = new BattlePassServiceImpl(userRepository, userBattlePassRepository, battlePassLevelRepository);
    }

    @Test
    public void testGetStatusUserNotFound() {
        when(userRepository.findByUsername("unknown")).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> {
            battlePassService.getStatus("unknown");
        });
    }

    @Test
    public void testGetStatusSuccess() {
        UserEntity user = UserEntity.builder().id(1L).username("lucas").xp(150).build();
        UserBattlePassEntity userPass = UserBattlePassEntity.builder().userId(1L).isPremium(true).claimedFreeLevel(1).claimedPremiumLevel(0).build();

        BattlePassLevelEntity level1 = BattlePassLevelEntity.builder().level(1).requiredXp(100).freeRewardType("COINS").freeRewardAmount(100).build();
        BattlePassLevelEntity level2 = BattlePassLevelEntity.builder().level(2).requiredXp(200).freeRewardType("STARDUST").freeRewardAmount(50).build();

        when(userRepository.findByUsername("lucas")).thenReturn(Optional.of(user));
        when(userBattlePassRepository.findByUserId(1L)).thenReturn(Optional.of(userPass));
        when(battlePassLevelRepository.findAll(any(Sort.class))).thenReturn(List.of(level1, level2));

        BattlePassStatusDTO status = battlePassService.getStatus("lucas");

        assertNotNull(status);
        assertTrue(status.isPremium());
        assertEquals(150, status.getCurrentXp());
        assertEquals(1, status.getCurrentLevel()); // reached level 1 (150 >= 100 but 150 < 200)
        assertEquals(1, status.getClaimedFreeLevel());
        assertEquals(0, status.getClaimedPremiumLevel());
    }

    @Test
    public void testClaimRewardUserNotFound() {
        when(userRepository.findByUsername("unknown")).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> {
            battlePassService.claimReward("unknown", 1, false);
        });
    }

    @Test
    public void testClaimRewardNotPremium() {
        UserEntity user = UserEntity.builder().id(1L).username("lucas").build();
        UserBattlePassEntity userPass = UserBattlePassEntity.builder().userId(1L).isPremium(false).build();

        when(userRepository.findByUsername("lucas")).thenReturn(Optional.of(user));
        when(userBattlePassRepository.findByUserId(1L)).thenReturn(Optional.of(userPass));

        assertThrows(IllegalArgumentException.class, () -> {
            battlePassService.claimReward("lucas", 1, true); // try to claim premium reward
        });
    }

    @Test
    public void testClaimRewardLevelNotFound() {
        UserEntity user = UserEntity.builder().id(1L).username("lucas").build();
        UserBattlePassEntity userPass = UserBattlePassEntity.builder().userId(1L).isPremium(false).build();

        when(userRepository.findByUsername("lucas")).thenReturn(Optional.of(user));
        when(userBattlePassRepository.findByUserId(1L)).thenReturn(Optional.of(userPass));
        when(battlePassLevelRepository.findById(1)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> {
            battlePassService.claimReward("lucas", 1, false);
        });
    }

    @Test
    public void testClaimRewardInsufficientXp() {
        UserEntity user = UserEntity.builder().id(1L).username("lucas").xp(50).build();
        UserBattlePassEntity userPass = UserBattlePassEntity.builder().userId(1L).isPremium(false).build();
        BattlePassLevelEntity level = BattlePassLevelEntity.builder().level(1).requiredXp(100).build();

        when(userRepository.findByUsername("lucas")).thenReturn(Optional.of(user));
        when(userBattlePassRepository.findByUserId(1L)).thenReturn(Optional.of(userPass));
        when(battlePassLevelRepository.findById(1)).thenReturn(Optional.of(level));

        assertThrows(IllegalArgumentException.class, () -> {
            battlePassService.claimReward("lucas", 1, false);
        });
    }

    @Test
    public void testClaimRewardAlreadyClaimed() {
        UserEntity user = UserEntity.builder().id(1L).username("lucas").xp(120).build();
        UserBattlePassEntity userPass = UserBattlePassEntity.builder().userId(1L).isPremium(false).claimedFreeLevel(1).build();
        BattlePassLevelEntity level = BattlePassLevelEntity.builder().level(1).requiredXp(100).build();

        when(userRepository.findByUsername("lucas")).thenReturn(Optional.of(user));
        when(userBattlePassRepository.findByUserId(1L)).thenReturn(Optional.of(userPass));
        when(battlePassLevelRepository.findById(1)).thenReturn(Optional.of(level));

        assertThrows(IllegalArgumentException.class, () -> {
            battlePassService.claimReward("lucas", 1, false);
        });
    }

    @Test
    public void testClaimRewardSuccessCoins() {
        UserEntity user = UserEntity.builder()
                .id(1L)
                .username("lucas")
                .xp(150)
                .pokecoins(200)
                .unlockedTitles(new HashSet<>())
                .unlockedAvatars(new HashSet<>())
                .packsInventory(new java.util.HashMap<>())
                .build();
        UserBattlePassEntity userPass = UserBattlePassEntity.builder().userId(1L).isPremium(false).claimedFreeLevel(0).build();
        BattlePassLevelEntity level = BattlePassLevelEntity.builder()
                .level(1)
                .requiredXp(100)
                .freeRewardType("COINS")
                .freeRewardAmount(150)
                .build();

        when(userRepository.findByUsername("lucas")).thenReturn(Optional.of(user));
        when(userBattlePassRepository.findByUserId(1L)).thenReturn(Optional.of(userPass));
        when(battlePassLevelRepository.findById(1)).thenReturn(Optional.of(level));

        battlePassService.claimReward("lucas", 1, false);

        assertEquals(350, user.getPokecoins());
        assertEquals(1, userPass.getClaimedFreeLevel());
        verify(userRepository, times(1)).save(user);
        verify(userBattlePassRepository, times(1)).saveAndFlush(userPass);
    }

    @Test
    public void testPurchasePremiumInsufficientCoins() {
        UserEntity user = UserEntity.builder().id(1L).username("lucas").pokecoins(500).build();
        when(userRepository.findByUsername("lucas")).thenReturn(Optional.of(user));
        when(userBattlePassRepository.findByUserId(1L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> {
            battlePassService.purchasePremium("lucas");
        });
    }

    @Test
    public void testPurchasePremiumSuccess() {
        UserEntity user = UserEntity.builder().id(1L).username("lucas").pokecoins(1200).build();
        when(userRepository.findByUsername("lucas")).thenReturn(Optional.of(user));
        when(userBattlePassRepository.findByUserId(1L)).thenReturn(Optional.empty());

        battlePassService.purchasePremium("lucas");

        assertEquals(200, user.getPokecoins());
        verify(userBattlePassRepository, times(1)).saveAndFlush(argThat(UserBattlePassEntity::getIsPremium));
    }
}
