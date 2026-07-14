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
import java.util.Locale;
import java.util.Map;
import java.util.Random;

@Service
public class PackServiceImpl implements PackService {

    private static final String LEGACY_PACK_TYPE = "pack_base";

    private static final int DEFAULT_TIER = 1;
    private static final int TIER_RARE_MIN = 3;
    private static final int TIER_EPIC_MIN = 5;
    private static final int TIER_LEGENDARY = 7;

    private static final Map<String, Integer> PACK_TIERS = Map.ofEntries(
            Map.entry("pack_comun", 1),
            Map.entry("pack_kanto_base", 1),
            Map.entry(LEGACY_PACK_TYPE, 1),
            Map.entry("pack_johto_retro", 2),
            Map.entry("pack_raro", TIER_RARE_MIN),
            Map.entry("pack_hoenn_avanzado", TIER_RARE_MIN),
            Map.entry("pack_sinnoh_mistico", 4),
            Map.entry("pack_epico", TIER_EPIC_MIN),
            Map.entry("pack_unova_dragones", TIER_EPIC_MIN),
            Map.entry("pack_kalos_hadas", 6),
            Map.entry("pack_legendario", TIER_LEGENDARY),
            Map.entry("pack_alola_solluna", TIER_LEGENDARY)
    );

    // First (guaranteed) slot: tiers 1-2 pull mostly common with a small chance of rare.
    private static final double FIRST_SLOT_COMMON_CHANCE = 0.9;

    // Slots after the first: independent chance rolls scaling with pack tier.
    private static final int OTHER_SLOT_LEGENDARY_MIN_TIER = 6;
    private static final double OTHER_SLOT_LEGENDARY_CHANCE = 0.05;
    private static final int OTHER_SLOT_EPIC_MIN_TIER = 4;
    private static final double OTHER_SLOT_EPIC_CHANCE = 0.15;
    private static final int OTHER_SLOT_RARE_MIN_TIER = 2;
    private static final double OTHER_SLOT_RARE_CHANCE = 0.25;
    private static final double OTHER_SLOT_BASE_RARE_CHANCE = 0.10;

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

    private record CardPools(List<CardEntity> common, List<CardEntity> rare, List<CardEntity> epic, List<CardEntity> legendary) {
    }

    private record PullOutcome(boolean isDuplicate, int coinsRefunded) {
    }

    private int getPackTier(String packType) {
        return packType == null ? DEFAULT_TIER : PACK_TIERS.getOrDefault(packType, DEFAULT_TIER);
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
        String subtype = card.getSubtype() != null ? card.getSubtype().toUpperCase(Locale.ROOT) : "";
        return subtype.contains("EX") || subtype.contains("GX") || subtype.contains(" V") || "V".equals(subtype) || subtype.contains("MEGA") || subtype.contains("LEGEND");
    }

    private boolean isEpic(CardEntity card) {
        String subtype = card.getSubtype() != null ? card.getSubtype().toUpperCase(Locale.ROOT) : "";
        return subtype.contains("STAGE 2");
    }

    private boolean isRare(CardEntity card) {
        String subtype = card.getSubtype() != null ? card.getSubtype().toUpperCase(Locale.ROOT) : "";
        return subtype.contains("STAGE 1");
    }

    @Override
    @Transactional
    public PackOpeningResultDTO openPack(String username, String packType) {
        UserEntity user = userRepository.findFirstByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        deductPack(user, packType);

        List<CardEntity> allCards = cardRepository.findAll();
        if (allCards.isEmpty()) {
            throw new IllegalStateException("No hay cartas configuradas en la base de datos");
        }
        CardPools pools = categorizeCards(allCards);

        int tier = getPackTier(packType);
        double baseFoilChance = getFoilChance(tier);

        List<UserShowcaseInventoryEntity> userInventory = inventoryRepository.findByUserId(user.getId());
        List<PackOpeningResultDTO.PulledCardDTO> pulledCards = new ArrayList<>();
        int coinsRefunded = 0;

        for (int i = 0; i < cardsPerOpen; i++) {
            boolean isFoil = random.nextDouble() < baseFoilChance;
            final CardEntity pulledCard = i == 0 ? pullGuaranteedSlot(tier, pools) : pullRandomSlot(tier, pools);
            if (i == 0 && tier == TIER_LEGENDARY) {
                isFoil = true; // Guaranteed foil
            }

            final PullOutcome outcome = recordPulledCard(user, pulledCard, isFoil, userInventory);
            coinsRefunded += outcome.coinsRefunded();
            pulledCards.add(new PackOpeningResultDTO.PulledCardDTO(pulledCard.getId(), isFoil, outcome.isDuplicate()));
        }

        if (coinsRefunded > 0) {
            user.setPokecoins((user.getPokecoins() != null ? user.getPokecoins() : 0) + coinsRefunded);
        }

        userRepository.save(user);

        return new PackOpeningResultDTO(pulledCards, coinsRefunded);
    }

    private void deductPack(final UserEntity user, final String packType) {
        int currentAmount = user.getPacksInventory().getOrDefault(packType, 0);
        if (currentAmount <= 0) {
            // Fallback for backwards compatibility
            if (LEGACY_PACK_TYPE.equals(packType) && user.getPacks() != null && user.getPacks() > 0) {
                currentAmount = user.getPacks();
            } else {
                throw new IllegalArgumentException("No tienes sobres de este tipo disponibles para abrir");
            }
        }

        user.getPacksInventory().put(packType, currentAmount - 1);
        if (user.getPacks() != null && user.getPacks() > 0) {
            user.setPacks(user.getPacks() - 1);
        }
    }

    private CardPools categorizeCards(final List<CardEntity> allCards) {
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
        if (legendaryCards.isEmpty()) {
            legendaryCards.addAll(allCards);
        }
        if (epicCards.isEmpty()) {
            epicCards.addAll(rareCards.isEmpty() ? allCards : rareCards);
        }
        if (rareCards.isEmpty()) {
            rareCards.addAll(commonCards.isEmpty() ? allCards : commonCards);
        }
        if (commonCards.isEmpty()) {
            commonCards.addAll(allCards);
        }

        return new CardPools(commonCards, rareCards, epicCards, legendaryCards);
    }

    // First (guaranteed) card of the pack: rarity floor scales with tier.
    private CardEntity pullGuaranteedSlot(final int tier, final CardPools pools) {
        if (tier == TIER_LEGENDARY) {
            return pools.legendary().get(random.nextInt(pools.legendary().size()));
        }
        if (tier == TIER_EPIC_MIN || tier == TIER_EPIC_MIN + 1) {
            return pools.epic().get(random.nextInt(pools.epic().size()));
        }
        if (tier == TIER_RARE_MIN || tier == TIER_RARE_MIN + 1) {
            return pools.rare().get(random.nextInt(pools.rare().size()));
        }
        return random.nextDouble() < FIRST_SLOT_COMMON_CHANCE
                ? pools.common().get(random.nextInt(pools.common().size()))
                : pools.rare().get(random.nextInt(pools.rare().size()));
    }

    // Remaining cards of the pack: small independent chance of a higher rarity, scaling with tier.
    private CardEntity pullRandomSlot(final int tier, final CardPools pools) {
        final double rand = random.nextDouble();
        if (tier >= OTHER_SLOT_LEGENDARY_MIN_TIER && rand < OTHER_SLOT_LEGENDARY_CHANCE) {
            return pools.legendary().get(random.nextInt(pools.legendary().size()));
        }
        if (tier >= OTHER_SLOT_EPIC_MIN_TIER && rand < OTHER_SLOT_EPIC_CHANCE) {
            return pools.epic().get(random.nextInt(pools.epic().size()));
        }
        if (tier >= OTHER_SLOT_RARE_MIN_TIER && rand < OTHER_SLOT_RARE_CHANCE) {
            return pools.rare().get(random.nextInt(pools.rare().size()));
        }
        if (rand < OTHER_SLOT_BASE_RARE_CHANCE) {
            return pools.rare().get(random.nextInt(pools.rare().size()));
        }
        return pools.common().get(random.nextInt(pools.common().size()));
    }

    private PullOutcome recordPulledCard(final UserEntity user, final CardEntity pulledCard, final boolean isFoil,
                                          final List<UserShowcaseInventoryEntity> userInventory) {
        final boolean isDuplicate = userInventory.stream()
                .anyMatch(inv -> inv.getCardId().equals(pulledCard.getId()) && inv.getIsFoil() == isFoil);

        if (isDuplicate) {
            return new PullOutcome(true, isFoil ? duplicateRefundFoil : duplicateRefundNormal);
        }

        final UserShowcaseInventoryEntity newEntry = UserShowcaseInventoryEntity.builder()
                .user(user)
                .cardId(pulledCard.getId())
                .isFoil(isFoil)
                .build();
        inventoryRepository.save(newEntry);
        userInventory.add(newEntry);
        return new PullOutcome(false, 0);
    }
}
