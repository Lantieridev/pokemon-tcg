package ar.edu.utn.frc.tup.piii.services.impl;

import ar.edu.utn.frc.tup.piii.dtos.PackOpeningResultDTO;
import ar.edu.utn.frc.tup.piii.persistence.entity.CardEntity;
import ar.edu.utn.frc.tup.piii.persistence.entity.UserEntity;
import ar.edu.utn.frc.tup.piii.persistence.entity.UserShowcaseInventoryEntity;
import ar.edu.utn.frc.tup.piii.persistence.repository.CardRepository;
import ar.edu.utn.frc.tup.piii.persistence.repository.UserRepository;
import ar.edu.utn.frc.tup.piii.persistence.repository.UserShowcaseInventoryRepository;
import ar.edu.utn.frc.tup.piii.services.PackService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Service
public class PackServiceImpl implements PackService {

    private final UserRepository userRepository;
    private final CardRepository cardRepository;
    private final UserShowcaseInventoryRepository inventoryRepository;
    private final Random random = new Random();
    private final int cardsPerOpen;
    private final int duplicateRefundNormal;
    private final int duplicateRefundFoil;

    public PackServiceImpl(UserRepository userRepository, CardRepository cardRepository,
                            UserShowcaseInventoryRepository inventoryRepository,
                            @Value("${economy.pack.cards-per-open:5}") int cardsPerOpen,
                            @Value("${economy.pack.duplicate-refund-normal:10}") int duplicateRefundNormal,
                            @Value("${economy.pack.duplicate-refund-foil:50}") int duplicateRefundFoil) {
        this.userRepository = userRepository;
        this.cardRepository = cardRepository;
        this.inventoryRepository = inventoryRepository;
        this.cardsPerOpen = cardsPerOpen;
        this.duplicateRefundNormal = duplicateRefundNormal;
        this.duplicateRefundFoil = duplicateRefundFoil;
    }

    private int getPackTier(String packType) {
        if (packType == null) return 1;
        switch (packType) {
            case "pack_comun":
            case "pack_kanto_base":
            case "pack_base":
                return 1;
            case "pack_johto_retro":
                return 2;
            case "pack_raro":
            case "pack_hoenn_avanzado":
                return 3;
            case "pack_sinnoh_mistico":
                return 4;
            case "pack_epico":
            case "pack_unova_dragones":
                return 5;
            case "pack_kalos_hadas":
                return 6;
            case "pack_legendario":
            case "pack_alola_solluna":
                return 7;
            default:
                return 1;
        }
    }

    private double getFoilChance(int tier) {
        switch (tier) {
            case 1: return 0.05;
            case 2: return 0.10;
            case 3: return 0.20;
            case 4: return 0.30;
            case 5: return 0.40;
            case 6: return 0.60;
            case 7: return 1.00;
            default: return 0.05;
        }
    }

    private boolean isLegendary(CardEntity card) {
        String subtype = card.getSubtype() != null ? card.getSubtype().toUpperCase() : "";
        return subtype.contains("EX") || subtype.contains("GX") || subtype.contains(" V") || subtype.equals("V") || subtype.contains("MEGA") || subtype.contains("LEGEND");
    }

    private boolean isEpic(CardEntity card) {
        String subtype = card.getSubtype() != null ? card.getSubtype().toUpperCase() : "";
        return subtype.contains("STAGE 2");
    }

    private boolean isRare(CardEntity card) {
        String subtype = card.getSubtype() != null ? card.getSubtype().toUpperCase() : "";
        return subtype.contains("STAGE 1");
    }

    @Override
    @Transactional
    public PackOpeningResultDTO openPack(String username, String packType) {
        UserEntity user = userRepository.findFirstByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        int currentAmount = user.getPacksInventory().getOrDefault(packType, 0);
        if (currentAmount <= 0) {
            // Fallback for backwards compatibility
            if (packType.equals("pack_base") && user.getPacks() != null && user.getPacks() > 0) {
                currentAmount = user.getPacks();
            } else {
                throw new IllegalArgumentException("No tienes sobres de este tipo disponibles para abrir");
            }
        }

        // Deduct pack
        user.getPacksInventory().put(packType, currentAmount - 1);
        if (user.getPacks() != null && user.getPacks() > 0) {
            user.setPacks(user.getPacks() - 1);
        }

        List<CardEntity> allCards = cardRepository.findAll();
        if (allCards.isEmpty()) {
            throw new IllegalStateException("No hay cartas configuradas en la base de datos");
        }

        // Categorize cards in memory
        List<CardEntity> commonCards = new ArrayList<>();
        List<CardEntity> rareCards = new ArrayList<>();
        List<CardEntity> epicCards = new ArrayList<>();
        List<CardEntity> legendaryCards = new ArrayList<>();

        for (CardEntity card : allCards) {
            if (isLegendary(card)) {
                legendaryCards.add(card);
            } else if (isEpic(card)) {
                epicCards.add(card);
            } else if (isRare(card)) {
                rareCards.add(card);
            } else {
                commonCards.add(card);
            }
        }
        
        // Fallbacks in case some lists are empty
        if (legendaryCards.isEmpty()) legendaryCards.addAll(allCards);
        if (epicCards.isEmpty()) epicCards.addAll(rareCards.isEmpty() ? allCards : rareCards);
        if (rareCards.isEmpty()) rareCards.addAll(commonCards.isEmpty() ? allCards : commonCards);
        if (commonCards.isEmpty()) commonCards.addAll(allCards);

        int tier = getPackTier(packType);
        double baseFoilChance = getFoilChance(tier);

        List<UserShowcaseInventoryEntity> userInventory = inventoryRepository.findByUserId(user.getId());
        List<PackOpeningResultDTO.PulledCardDTO> pulledCards = new ArrayList<>();
        int coinsRefunded = 0;

        for (int i = 0; i < cardsPerOpen; i++) {
            CardEntity pulledCard;
            boolean isFoil = random.nextDouble() < baseFoilChance;
            
            // Determine rarity for this slot based on Tier
            if (i == 0) { // First card is the guaranteed slot
                if (tier == 7) {
                    pulledCard = legendaryCards.get(random.nextInt(legendaryCards.size()));
                    isFoil = true; // Guaranteed foil
                } else if (tier == 6 || tier == 5) {
                    pulledCard = epicCards.get(random.nextInt(epicCards.size()));
                } else if (tier == 4 || tier == 3) {
                    pulledCard = rareCards.get(random.nextInt(rareCards.size()));
                } else {
                    // Tiers 1 and 2: Mostly common, tiny chance of rare
                    pulledCard = random.nextDouble() < 0.9 ? commonCards.get(random.nextInt(commonCards.size())) : rareCards.get(random.nextInt(rareCards.size()));
                }
            } else {
                // Other 4 cards have small chances of higher rarities scaling with tier
                double rand = random.nextDouble();
                if (tier >= 6 && rand < 0.05) {
                    pulledCard = legendaryCards.get(random.nextInt(legendaryCards.size()));
                } else if (tier >= 4 && rand < 0.15) {
                    pulledCard = epicCards.get(random.nextInt(epicCards.size()));
                } else if (tier >= 2 && rand < 0.25) {
                    pulledCard = rareCards.get(random.nextInt(rareCards.size()));
                } else if (rand < 0.10) {
                    pulledCard = rareCards.get(random.nextInt(rareCards.size()));
                } else {
                    pulledCard = commonCards.get(random.nextInt(commonCards.size()));
                }
            }

            final boolean finalIsFoil = isFoil;
            boolean isDuplicate = userInventory.stream()
                    .anyMatch(inv -> inv.getCardId().equals(pulledCard.getId()) && inv.getIsFoil() == finalIsFoil);

            if (isDuplicate) {
                // High rarities give more coins if duplicated (optional scaling, but we keep standard 50 for foil, 10 for normal to be safe)
                coinsRefunded += finalIsFoil ? duplicateRefundFoil : duplicateRefundNormal;
            } else {
                UserShowcaseInventoryEntity newEntry = UserShowcaseInventoryEntity.builder()
                        .user(user)
                        .cardId(pulledCard.getId())
                        .isFoil(finalIsFoil)
                        .build();
                inventoryRepository.save(newEntry);
                userInventory.add(newEntry);
            }

            pulledCards.add(new PackOpeningResultDTO.PulledCardDTO(pulledCard.getId(), finalIsFoil, isDuplicate));
        }

        if (coinsRefunded > 0) {
            user.setPokecoins((user.getPokecoins() != null ? user.getPokecoins() : 0) + coinsRefunded);
        }

        userRepository.save(user);

        return new PackOpeningResultDTO(pulledCards, coinsRefunded);
    }
}
