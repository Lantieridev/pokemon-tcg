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
            if (level <= userPass.getClaimedFreeLevel()) {
                throw new IllegalArgumentException("Ya reclamaste esta recompensa gratuita");
            }
            // Check that skipped levels are actually empty
            for (int i = userPass.getClaimedFreeLevel() + 1; i < level; i++) {
                BattlePassLevelEntity intermediate = battlePassLevelRepository.findById(i).orElse(null);
                if (intermediate != null && intermediate.getFreeRewardType() != null) {
                    throw new IllegalArgumentException("Debes reclamar las recompensas anteriores primero");
                }
            }
            grantReward(user, passLevel.getFreeRewardType(), passLevel.getFreeRewardAmount(), passLevel.getFreeRewardValue());
            userPass.setClaimedFreeLevel(level);
        } else {
            if (level <= userPass.getClaimedPremiumLevel()) {
                throw new IllegalArgumentException("Ya reclamaste esta recompensa premium");
            }
            // Check that skipped levels are actually empty
            for (int i = userPass.getClaimedPremiumLevel() + 1; i < level; i++) {
                BattlePassLevelEntity intermediate = battlePassLevelRepository.findById(i).orElse(null);
                if (intermediate != null && intermediate.getPremiumRewardType() != null) {
                    throw new IllegalArgumentException("Debes reclamar las recompensas anteriores primero");
                }
            }
            grantReward(user, passLevel.getPremiumRewardType(), passLevel.getPremiumRewardAmount(), passLevel.getPremiumRewardValue());
            userPass.setClaimedPremiumLevel(level);
        }

        userRepository.save(user);
        userBattlePassRepository.saveAndFlush(userPass);
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
        if (type == null) return;
        switch (type.toUpperCase(Locale.ROOT)) {
            case "COINS" -> {
                int coins = user.getPokecoins() != null ? user.getPokecoins() : 0;
                user.setPokecoins(coins + (amount != null ? amount : 0));
            }
            case "PACK" -> {
                int addAmount = amount != null ? amount : 0;
                int packs = user.getPacks() != null ? user.getPacks() : 0;
                user.setPacks(packs + addAmount);
                
                String packKey = "pack_comun";
                if (value != null && !value.trim().isEmpty()) {
                    String norm = java.text.Normalizer.normalize(value.toLowerCase(Locale.ROOT), java.text.Normalizer.Form.NFD);
                    norm = norm.replaceAll("[\\p{InCombiningDiacriticalMarks}]", "");
                    norm = norm.replaceAll("\\s+", "_");
                    norm = norm.replaceAll("[^a-z0-9_]", "");
                    packKey = "pack_" + norm;
                }
                user.getPacksInventory().put(packKey, user.getPacksInventory().getOrDefault(packKey, 0) + addAmount);
            }
            case "TITLE" -> {
                if (value != null) user.getUnlockedTitles().add(value);
            }
            case "AVATAR" -> {
                if (value != null) user.getUnlockedAvatars().add(value);
            }
            default -> log.warn("Unrecognized battle pass reward type '{}' for user {}", type, user.getUsername());
        }
    }

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
