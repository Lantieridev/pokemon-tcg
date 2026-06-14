package ar.edu.utn.frc.tup.piii.services.impl;

import ar.edu.utn.frc.tup.piii.dtos.BattlePassLevelDTO;
import ar.edu.utn.frc.tup.piii.dtos.BattlePassStatusDTO;
import ar.edu.utn.frc.tup.piii.persistence.entity.BattlePassLevelEntity;
import ar.edu.utn.frc.tup.piii.persistence.entity.UserBattlePassEntity;
import ar.edu.utn.frc.tup.piii.persistence.entity.UserEntity;
import ar.edu.utn.frc.tup.piii.persistence.repository.BattlePassLevelRepository;
import ar.edu.utn.frc.tup.piii.persistence.repository.UserBattlePassRepository;
import ar.edu.utn.frc.tup.piii.persistence.repository.UserRepository;
import ar.edu.utn.frc.tup.piii.services.BattlePassService;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class BattlePassServiceImpl implements BattlePassService {

    private final UserRepository userRepository;
    private final UserBattlePassRepository userBattlePassRepository;
    private final BattlePassLevelRepository battlePassLevelRepository;

    public BattlePassServiceImpl(UserRepository userRepository, UserBattlePassRepository userBattlePassRepository, BattlePassLevelRepository battlePassLevelRepository) {
        this.userRepository = userRepository;
        this.userBattlePassRepository = userBattlePassRepository;
        this.battlePassLevelRepository = battlePassLevelRepository;
    }

    @Override
    public BattlePassStatusDTO getStatus(String username) {
        UserEntity user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        UserBattlePassEntity userPass = userBattlePassRepository.findByUserId(user.getId())
                .orElse(UserBattlePassEntity.builder().user(user).build());

        List<BattlePassLevelDTO> levels = battlePassLevelRepository.findAll(Sort.by(Sort.Direction.ASC, "level"))
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());

        // Calculate current level based on user XP
        int currentXp = user.getXp() != null ? user.getXp() : 0;
        int currentLevel = 0;
        for (BattlePassLevelDTO level : levels) {
            if (currentXp >= level.getRequiredXp()) {
                currentLevel = level.getLevel();
            } else {
                break;
            }
        }

        return BattlePassStatusDTO.builder()
                .isPremium(userPass.getIsPremium())
                .currentXp(currentXp)
                .currentLevel(currentLevel)
                .claimedFreeLevel(userPass.getClaimedFreeLevel())
                .claimedPremiumLevel(userPass.getClaimedPremiumLevel())
                .levels(levels)
                .build();
    }

    @Override
    @Transactional
    public void claimReward(String username, int level, boolean isPremium) {
        UserEntity user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        UserBattlePassEntity userPass = userBattlePassRepository.findByUserId(user.getId())
                .orElse(UserBattlePassEntity.builder().user(user).build());

        if (isPremium && !userPass.getIsPremium()) {
            throw new IllegalArgumentException("No tienes el pase de batalla premium");
        }

        BattlePassLevelEntity passLevel = battlePassLevelRepository.findById(level)
                .orElseThrow(() -> new IllegalArgumentException("Nivel de pase no encontrado"));

        int currentXp = user.getXp() != null ? user.getXp() : 0;
        if (currentXp < passLevel.getRequiredXp()) {
            throw new IllegalArgumentException("No tienes la experiencia necesaria para reclamar esta recompensa");
        }

        if (!isPremium) {
            if (level <= userPass.getClaimedFreeLevel()) {
                throw new IllegalArgumentException("Ya reclamaste esta recompensa gratuita");
            }
            if (level != userPass.getClaimedFreeLevel() + 1) {
                throw new IllegalArgumentException("Debes reclamar las recompensas en orden");
            }
            grantReward(user, passLevel.getFreeRewardType(), passLevel.getFreeRewardAmount(), passLevel.getFreeRewardValue());
            userPass.setClaimedFreeLevel(level);
        } else {
            if (level <= userPass.getClaimedPremiumLevel()) {
                throw new IllegalArgumentException("Ya reclamaste esta recompensa premium");
            }
            if (level != userPass.getClaimedPremiumLevel() + 1) {
                throw new IllegalArgumentException("Debes reclamar las recompensas en orden");
            }
            grantReward(user, passLevel.getPremiumRewardType(), passLevel.getPremiumRewardAmount(), passLevel.getPremiumRewardValue());
            userPass.setClaimedPremiumLevel(level);
        }

        userRepository.save(user);
        userBattlePassRepository.save(userPass);
    }

    @Override
    @Transactional
    public void purchasePremium(String username) {
        UserEntity user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        UserBattlePassEntity userPass = userBattlePassRepository.findByUserId(user.getId())
                .orElse(UserBattlePassEntity.builder().user(user).build());

        if (userPass.getIsPremium()) {
            throw new IllegalArgumentException("Ya tienes el pase de batalla premium");
        }

        int balance = user.getPokecoins() != null ? user.getPokecoins() : 0;
        if (balance < 1000) {
            throw new IllegalArgumentException("No tienes suficientes Pokecoins (1000) para comprar el pase premium");
        }

        user.setPokecoins(balance - 1000);
        userPass.setIsPremium(true);

        userRepository.save(user);
        userBattlePassRepository.save(userPass);
    }

    private void grantReward(UserEntity user, String type, Integer amount, String value) {
        if (type == null) return;
        switch (type.toUpperCase()) {
            case "COINS" -> {
                int coins = user.getPokecoins() != null ? user.getPokecoins() : 0;
                user.setPokecoins(coins + (amount != null ? amount : 0));
            }
            case "PACK" -> {
                int packs = user.getPacks() != null ? user.getPacks() : 0;
                user.setPacks(packs + (amount != null ? amount : 0));
            }
            case "TITLE" -> {
                if (value != null) user.getUnlockedTitles().add(value);
            }
            case "AVATAR" -> {
                if (value != null) user.getUnlockedAvatars().add(value);
            }
        }
    }

    private BattlePassLevelDTO mapToDTO(BattlePassLevelEntity entity) {
        return BattlePassLevelDTO.builder()
                .level(entity.getLevel())
                .requiredXp(entity.getRequiredXp())
                .freeRewardType(entity.getFreeRewardType())
                .freeRewardAmount(entity.getFreeRewardAmount())
                .freeRewardValue(entity.getFreeRewardValue())
                .premiumRewardType(entity.getPremiumRewardType())
                .premiumRewardAmount(entity.getPremiumRewardAmount())
                .premiumRewardValue(entity.getPremiumRewardValue())
                .build();
    }
}
