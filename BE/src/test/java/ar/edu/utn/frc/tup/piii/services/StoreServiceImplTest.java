package ar.edu.utn.frc.tup.piii.services;

import ar.edu.utn.frc.tup.piii.dtos.StoreItemDTO;
import ar.edu.utn.frc.tup.piii.persistence.entity.StoreItemEntity;
import ar.edu.utn.frc.tup.piii.persistence.entity.StoreItemType;
import ar.edu.utn.frc.tup.piii.persistence.entity.UserEntity;
import ar.edu.utn.frc.tup.piii.persistence.repository.StoreItemRepository;
import ar.edu.utn.frc.tup.piii.persistence.repository.UserRepository;
import ar.edu.utn.frc.tup.piii.services.impl.StoreServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class StoreServiceImplTest {

    private StoreItemRepository storeItemRepository;
    private UserRepository userRepository;
    private StoreService storeService;

    @BeforeEach
    public void setUp() {
        storeItemRepository = mock(StoreItemRepository.class);
        userRepository = mock(UserRepository.class);
        storeService = new StoreServiceImpl(storeItemRepository, userRepository);
    }

    @Test
    public void testGetAvailableItems() {
        StoreItemEntity item1 = StoreItemEntity.builder()
                .id(1L)
                .name("Avatar Pikachu")
                .price(100)
                .itemType(StoreItemType.AVATAR)
                .isActive(true)
                .build();
        when(storeItemRepository.findAllByIsActiveTrue()).thenReturn(List.of(item1));

        List<StoreItemDTO> items = storeService.getAvailableItems();
        assertEquals(1, items.size());
        assertEquals("Avatar Pikachu", items.get(0).getName());
    }

    @Test
    public void testBuyItemSuccessAvatar() {
        UserEntity user = UserEntity.builder()
                .id(1L)
                .username("lucas")
                .pokecoins(200)
                .avatarIcon("default")
                .unlockedAvatars(new HashSet<>())
                .build();

        StoreItemEntity item = StoreItemEntity.builder()
                .id(1L)
                .name("Avatar Pikachu")
                .imageUrl("pikachu.png")
                .price(100)
                .itemType(StoreItemType.AVATAR)
                .isActive(true)
                .build();

        when(userRepository.findByUsername("lucas")).thenReturn(Optional.of(user));
        when(storeItemRepository.findById(1L)).thenReturn(Optional.of(item));

        storeService.buyItem("lucas", 1L);

        verify(userRepository, times(1)).save(user);
        assertEquals(100, user.getPokecoins());
        assertTrue(user.getUnlockedAvatars().contains("Avatar Pikachu"));
    }

    @Test
    public void testBuyItemInsufficientFunds() {
        UserEntity user = UserEntity.builder()
                .id(1L)
                .username("lucas")
                .pokecoins(50)
                .build();

        StoreItemEntity item = StoreItemEntity.builder()
                .id(1L)
                .name("Avatar Pikachu")
                .price(100)
                .isActive(true)
                .build();

        when(userRepository.findByUsername("lucas")).thenReturn(Optional.of(user));
        when(storeItemRepository.findById(1L)).thenReturn(Optional.of(item));

        assertThrows(IllegalArgumentException.class, () -> storeService.buyItem("lucas", 1L));
    }

    @Test
    public void testBuyItemAlreadyOwnedTitle() {
        UserEntity user = UserEntity.builder()
                .id(1L)
                .username("lucas")
                .pokecoins(200)
                .unlockedTitles(new HashSet<>(List.of("VIP")))
                .build();

        StoreItemEntity item = StoreItemEntity.builder()
                .id(1L)
                .name("VIP")
                .price(100)
                .itemType(StoreItemType.TITLE)
                .isActive(true)
                .build();

        when(userRepository.findByUsername("lucas")).thenReturn(Optional.of(user));
        when(storeItemRepository.findById(1L)).thenReturn(Optional.of(item));

        assertThrows(IllegalArgumentException.class, () -> storeService.buyItem("lucas", 1L));
    }
}
