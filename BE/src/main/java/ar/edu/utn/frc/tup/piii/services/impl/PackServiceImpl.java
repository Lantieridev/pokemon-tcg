package ar.edu.utn.frc.tup.piii.services.impl;

import ar.edu.utn.frc.tup.piii.dtos.PackOpeningResultDTO;
import ar.edu.utn.frc.tup.piii.persistence.entity.CardEntity;
import ar.edu.utn.frc.tup.piii.persistence.entity.UserEntity;
import ar.edu.utn.frc.tup.piii.persistence.entity.UserShowcaseInventoryEntity;
import ar.edu.utn.frc.tup.piii.persistence.repository.CardRepository;
import ar.edu.utn.frc.tup.piii.persistence.repository.UserRepository;
import ar.edu.utn.frc.tup.piii.persistence.repository.UserShowcaseInventoryRepository;
import ar.edu.utn.frc.tup.piii.services.PackService;
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

    public PackServiceImpl(UserRepository userRepository, CardRepository cardRepository, UserShowcaseInventoryRepository inventoryRepository) {
        this.userRepository = userRepository;
        this.cardRepository = cardRepository;
        this.inventoryRepository = inventoryRepository;
    }

    @Override
    @Transactional
    public PackOpeningResultDTO openPack(String username, String packType) {
        UserEntity user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        int currentAmount = user.getPacksInventory().getOrDefault(packType, 0);
        if (currentAmount <= 0) {
            // Fallback for backwards compatibility if they have generic packs but not in map
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

        // Get all possible cards
        List<CardEntity> allCards = cardRepository.findAll();
        if (allCards.isEmpty()) {
            throw new IllegalStateException("No hay cartas configuradas en la base de datos");
        }

        List<UserShowcaseInventoryEntity> userInventory = inventoryRepository.findByUserId(user.getId());

        List<PackOpeningResultDTO.PulledCardDTO> pulledCards = new ArrayList<>();
        int coinsRefunded = 0;

        // Pull 5 cards
        for (int i = 0; i < 5; i++) {
            CardEntity randomCard = allCards.get(random.nextInt(allCards.size()));
            
            // 5% chance of foil
            boolean isFoil = random.nextDouble() < 0.05;

            // Check if user already owns it (exact match of card + foil status)
            boolean isDuplicate = userInventory.stream()
                    .anyMatch(inv -> inv.getCardId().equals(randomCard.getId()) && inv.getIsFoil() == isFoil);

            if (isDuplicate) {
                // Refund 50 coins for foil duplicate, 10 for normal duplicate
                coinsRefunded += isFoil ? 50 : 10;
            } else {
                // Add to inventory
                UserShowcaseInventoryEntity newEntry = UserShowcaseInventoryEntity.builder()
                        .user(user)
                        .cardId(randomCard.getId())
                        .isFoil(isFoil)
                        .build();
                inventoryRepository.save(newEntry);
                userInventory.add(newEntry); // to catch duplicates within the same pack
            }

            pulledCards.add(new PackOpeningResultDTO.PulledCardDTO(randomCard.getId(), isFoil, isDuplicate));
        }

        // Add refunded coins
        if (coinsRefunded > 0) {
            user.setPokecoins((user.getPokecoins() != null ? user.getPokecoins() : 0) + coinsRefunded);
        }

        userRepository.save(user);

        return new PackOpeningResultDTO(pulledCards, coinsRefunded);
    }
}
