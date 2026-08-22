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
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Slf4j
@Service
@SuppressWarnings({"PMD.GodClass", "PMD.TooManyMethods"})
// Battle-pass domain service: status/claim/purchase/reward-granting all live here because they
// share the same UserBattlePassEntity invariants; per-method complexity is kept low by the
// grant*/claim*Reward helper extraction above (that extraction is also why method count grew).
public class BattlePassServiceImpl implements BattlePassService {

    private final UserRepository userRepository;
    private final UserBattlePassRepository userBattlePassRepository;
    private final BattlePassLevelRepository battlePassLevelRepository;
    private final int premiumPrice;

    public BattlePassServiceImpl(UserRepository userRepository, UserBattlePassRepository userBattlePassRepository,
                                  BattlePassLevelRepository battlePassLevelRepository,
                                  @Value("${economy.battle-pass.premium-price:1000}") int premiumPrice) {
        this.userRepository = userRepository;
        this.userBattlePassRepository = userBattlePassRepository;
        this.battlePassLevelRepository = battlePassLevelRepository;
        this.premiumPrice = premiumPrice;
    }

    @Override
    public BattlePassStatusDTO getStatus(String username) {
        UserEntity user = userRepository.findFirstByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        UserBattlePassEntity userPass = userBattlePassRepository.findByUserId(user.getId())
                .orElse(UserBattlePassEntity.builder().user(user).userId(user.getId()).build());

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
                .isPremium(Boolean.TRUE.equals(userPass.getIsPremium()))
                .currentXp(currentXp)
                .currentLevel(currentLevel)
                .claimedFreeLevel(userPass.getClaimedFreeLevel() != null ? userPass.getClaimedFreeLevel() : 0)
                .claimedPremiumLevel(userPass.getClaimedPremiumLevel() != null ? userPass.getClaimedPremiumLevel() : 0)
                .levels(levels)
                .build();
    }

    @Override
    @Transactional
    public void claimReward(String username, int level, boolean isPremium) {
        UserEntity user = userRepository.findFirstByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        UserBattlePassEntity userPass = userBattlePassRepository.findByUserId(user.getId())
                .orElse(UserBattlePassEntity.builder().user(user).userId(user.getId()).isNew(true).build());

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
            claimFreeReward(user, userPass, level, passLevel);
        } else {
            claimPremiumReward(user, userPass, level, passLevel);
        }

        userRepository.save(user);
        userBattlePassRepository.saveAndFlush(userPass);
    }

    private void claimFreeReward(UserEntity user, UserBattlePassEntity userPass, int level, BattlePassLevelEntity passLevel) {
        if (level <= userPass.getClaimedFreeLevel()) {
            throw new IllegalArgumentException("Ya reclamaste esta recompensa gratuita");
        }
        for (int i = userPass.getClaimedFreeLevel() + 1; i < level; i++) {
            BattlePassLevelEntity intermediate = battlePassLevelRepository.findById(i).orElse(null);
            if (intermediate != null && intermediate.getFreeRewardType() != null) {
                throw new IllegalArgumentException("Debes reclamar las recompensas anteriores primero");
            }
        }
        grantReward(user, passLevel.getFreeRewardType(), passLevel.getFreeRewardAmount(), passLevel.getFreeRewardValue());
        userPass.setClaimedFreeLevel(level);
    }

    private void claimPremiumReward(UserEntity user, UserBattlePassEntity userPass, int level, BattlePassLevelEntity passLevel) {
        if (level <= userPass.getClaimedPremiumLevel()) {
            throw new IllegalArgumentException("Ya reclamaste esta recompensa premium");
        }
        for (int i = userPass.getClaimedPremiumLevel() + 1; i < level; i++) {
            BattlePassLevelEntity intermediate = battlePassLevelRepository.findById(i).orElse(null);
            if (intermediate != null && intermediate.getPremiumRewardType() != null) {
                throw new IllegalArgumentException("Debes reclamar las recompensas anteriores primero");
            }
        }
        grantReward(user, passLevel.getPremiumRewardType(), passLevel.getPremiumRewardAmount(), passLevel.getPremiumRewardValue());
        userPass.setClaimedPremiumLevel(level);
    }

    @Override
    @Transactional
    public void purchasePremium(String username) {
        UserEntity user = userRepository.findFirstByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        UserBattlePassEntity userPass = userBattlePassRepository.findByUserId(user.getId())
                .orElse(UserBattlePassEntity.builder().user(user).userId(user.getId()).isNew(true).build());

        if (userPass.getIsPremium()) {
            throw new IllegalArgumentException("Ya tienes el pase de batalla premium");
        }

        int balance = user.getPokecoins() != null ? user.getPokecoins() : 0;
        if (balance < premiumPrice) {
            throw new IllegalArgumentException("No tienes suficientes Pokecoins (" + premiumPrice + ") para comprar el pase premium");
        }

        user.setPokecoins(balance - premiumPrice);
        userPass.setIsPremium(true);

        userRepository.save(user);
        userBattlePassRepository.saveAndFlush(userPass);
    }

    private void grantReward(UserEntity user, String type, Integer amount, String value) {
        if (type == null) {
            return;
        }
        switch (type.toUpperCase(Locale.ROOT)) {
            case "COINS" -> grantCoinsReward(user, amount);
            case "PACK" -> grantPackReward(user, amount, value);
            case "TITLE" -> grantTitleReward(user, value);
            case "AVATAR" -> grantAvatarReward(user, value);
            default -> log.warn("Unrecognized battle pass reward type '{}' for user {}", type, user.getUsername());
        }
    }

    private void grantCoinsReward(UserEntity user, Integer amount) {
        int coins = user.getPokecoins() != null ? user.getPokecoins() : 0;
        user.setPokecoins(coins + (amount != null ? amount : 0));
    }

    private void grantTitleReward(UserEntity user, String value) {
        if (value != null) {
            user.getUnlockedTitles().add(value);
        }
    }

    private void grantAvatarReward(UserEntity user, String value) {
        if (value != null) {
            user.getUnlockedAvatars().add(value);
        }
    }

    private void grantPackReward(UserEntity user, Integer amount, String value) {
        int addAmount = amount != null ? amount : 0;
        int packs = user.getPacks() != null ? user.getPacks() : 0;
        user.setPacks(packs + addAmount);

        String packKey = normalizePackKey(value);
        user.getPacksInventory().put(packKey, user.getPacksInventory().getOrDefault(packKey, 0) + addAmount);
    }

    private String normalizePackKey(String value) {
        if (value == null || value.trim().isEmpty()) {
            return "pack_comun";
        }
        String norm = java.text.Normalizer.normalize(value.toLowerCase(Locale.ROOT), java.text.Normalizer.Form.NFD);
        norm = norm.replaceAll("[\\p{InCombiningDiacriticalMarks}]", "");
        norm = norm.replaceAll("\\s+", "_");
        norm = norm.replaceAll("[^a-z0-9_]", "");
        return "pack_" + norm;
    }

    @SuppressWarnings("PMD.UnusedPrivateMethod")
    // False positive: used via method reference (this::mapToDTO) in getStatus — PMD 7.0.0
    // does not resolve method-reference usages for this rule.
    private BattlePassLevelDTO mapToDTO(BattlePassLevelEntity entity) {
        return BattlePassLevelDTO.builder()
                .level(entity.getLevel())
                .requiredXp(entity.getRequiredXp())
                .freeRewardType(entity.getFreeRewardType())
                .freeRewardAmount(entity.getFreeRewardAmount() != null ? entity.getFreeRewardAmount() : 0)
                .freeRewardValue(entity.getFreeRewardValue())
                .premiumRewardType(entity.getPremiumRewardType())
                .premiumRewardAmount(entity.getPremiumRewardAmount() != null ? entity.getPremiumRewardAmount() : 0)
                .premiumRewardValue(entity.getPremiumRewardValue())
                .build();
    }
}
