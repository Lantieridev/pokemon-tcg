package ar.edu.utn.frc.tup.piii.services.impl;

import ar.edu.utn.frc.tup.piii.dtos.StoreItemDTO;
import ar.edu.utn.frc.tup.piii.persistence.entity.StoreItemEntity;
import ar.edu.utn.frc.tup.piii.persistence.entity.UserEntity;
import ar.edu.utn.frc.tup.piii.persistence.repository.StoreItemRepository;
import ar.edu.utn.frc.tup.piii.persistence.repository.UserRepository;
import ar.edu.utn.frc.tup.piii.services.StoreService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class StoreServiceImpl implements StoreService {

    private final StoreItemRepository storeItemRepository;
    private final UserRepository userRepository;

    public StoreServiceImpl(StoreItemRepository storeItemRepository, UserRepository userRepository) {
        this.storeItemRepository = storeItemRepository;
        this.userRepository = userRepository;
    }

    @Override
    public List<StoreItemDTO> getAvailableItems() {
        return storeItemRepository.findAllByIsActiveTrue().stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void buyItem(String username, Long itemId) {
        UserEntity user = userRepository.findFirstByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        StoreItemEntity item = storeItemRepository.findById(itemId)
                .orElseThrow(() -> new IllegalArgumentException("Artículo no encontrado"));

        if (!item.getIsActive()) {
            throw new IllegalArgumentException("El artículo ya no está disponible");
        }

        int balance = user.getPokecoins() != null ? user.getPokecoins() : 0;
        if (balance < item.getPrice()) {
            throw new IllegalArgumentException("No tienes suficientes pokecoins para comprar este artículo");
        }

        // Check if user already owns it (for titles and avatars)
        if (item.getItemType() == ar.edu.utn.frc.tup.piii.persistence.entity.StoreItemType.TITLE) {
            if (user.getUnlockedTitles().contains(item.getName())) {
                throw new IllegalArgumentException("Ya posees este título");
            }
        } else if (item.getItemType() == ar.edu.utn.frc.tup.piii.persistence.entity.StoreItemType.AVATAR) {
            if (user.getUnlockedAvatars().contains(item.getName()) || item.getImageUrl().equals(user.getAvatarIcon())) {
                throw new IllegalArgumentException("Ya posees este avatar");
            }
        }

        // Deduct coins
        user.setPokecoins(balance - item.getPrice());

        // Grant item
        switch (item.getItemType()) {
            case TITLE -> user.getUnlockedTitles().add(item.getName());
            case AVATAR -> user.getUnlockedAvatars().add(item.getName());
            case PACK -> {
                user.setPacks(user.getPacks() + 1); // Keep the global counter for total packs
                String packType = item.getImageUrl() != null ? item.getImageUrl() : "pack_base";
                user.getPacksInventory().put(packType, user.getPacksInventory().getOrDefault(packType, 0) + 1);
            }
        }

        userRepository.save(user);
    }

    private StoreItemDTO mapToDTO(StoreItemEntity entity) {
        return StoreItemDTO.builder()
                .id(entity.getId())
                .name(entity.getName())
                .description(entity.getDescription())
                .price(entity.getPrice())
                .itemType(entity.getItemType())
                .imageUrl(entity.getImageUrl())
                .build();
    }
}
