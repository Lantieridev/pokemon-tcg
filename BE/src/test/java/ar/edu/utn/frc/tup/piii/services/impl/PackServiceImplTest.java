package ar.edu.utn.frc.tup.piii.services.impl;

import ar.edu.utn.frc.tup.piii.dtos.PackOpeningResultDTO;
import ar.edu.utn.frc.tup.piii.persistence.entity.CardEntity;
import ar.edu.utn.frc.tup.piii.persistence.entity.UserEntity;
import ar.edu.utn.frc.tup.piii.persistence.entity.UserShowcaseInventoryEntity;
import ar.edu.utn.frc.tup.piii.persistence.repository.CardRepository;
import ar.edu.utn.frc.tup.piii.persistence.repository.UserRepository;
import ar.edu.utn.frc.tup.piii.persistence.repository.UserShowcaseInventoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Map;

public class PackServiceImplTest {

    private UserRepository userRepository;
    private CardRepository cardRepository;
    private UserShowcaseInventoryRepository inventoryRepository;
    private PackServiceImpl packService;

    @BeforeEach
    public void setUp() {
        userRepository = mock(UserRepository.class);
        cardRepository = mock(CardRepository.class);
        inventoryRepository = mock(UserShowcaseInventoryRepository.class);
        packService = new PackServiceImpl(userRepository, cardRepository, inventoryRepository);
    }

    @Test
    public void testOpenPackUserNotFound() {
        when(userRepository.findFirstByUsername("unknown")).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> {
            packService.openPack("unknown", "pack_base");
        });
    }

    @Test
    public void testOpenPackNoPacksAvailable() {
        UserEntity user = UserEntity.builder()
                .username("lucas")
                .packsInventory(new HashMap<>())
                .packs(0)
                .build();
        when(userRepository.findFirstByUsername("lucas")).thenReturn(Optional.of(user));

        assertThrows(IllegalArgumentException.class, () -> {
            packService.openPack("lucas", "pack_base");
        });
    }

    @Test
    public void testOpenPackEmptyCardDatabase() {
        UserEntity user = UserEntity.builder()
                .username("lucas")
                .packsInventory(new HashMap<>(Map.of("pack_base", 1)))
                .packs(1)
                .build();
        when(userRepository.findFirstByUsername("lucas")).thenReturn(Optional.of(user));
        when(cardRepository.findAll()).thenReturn(List.of());

        assertThrows(IllegalStateException.class, () -> {
            packService.openPack("lucas", "pack_base");
        });
    }

    @Test
    public void testOpenPackSuccessWithDuplicatesAndNewCards() {
        UserEntity user = UserEntity.builder()
                .id(1L)
                .username("lucas")
                .packsInventory(new HashMap<>(Map.of("pack_base", 2)))
                .packs(2)
                .pokecoins(100)
                .build();

        CardEntity card1 = CardEntity.builder().id("card-1").name("Pikachu").subtype("Basic").build();
        CardEntity card2 = CardEntity.builder().id("card-2").name("Charizard EX").subtype("EX").build();
        CardEntity card3 = CardEntity.builder().id("card-3").name("Blastoise Stage 2").subtype("STAGE 2").build();
        CardEntity card4 = CardEntity.builder().id("card-4").name("Venusaur Stage 1").subtype("STAGE 1").build();
        CardEntity card5 = CardEntity.builder().id("card-5").name("Pidgey").subtype("Basic").build();
        CardEntity card6 = CardEntity.builder().id("card-6").name("Rattata").subtype("Basic").build();
        CardEntity card7 = CardEntity.builder().id("card-7").name("Caterpie").subtype("Basic").build();
        CardEntity card8 = CardEntity.builder().id("card-8").name("Weedle").subtype("Basic").build();
        CardEntity card9 = CardEntity.builder().id("card-9").name("Pidgeotto").subtype("Basic").build();

        when(userRepository.findFirstByUsername("lucas")).thenReturn(Optional.of(user));
        when(cardRepository.findAll()).thenReturn(List.of(card1, card2, card3, card4, card5, card6, card7, card8, card9));

        // User already has card1 in inventory
        UserShowcaseInventoryEntity existingItem = UserShowcaseInventoryEntity.builder()
                .user(user)
                .cardId("card-1")
                .isFoil(false)
                .build();
        when(inventoryRepository.findByUserId(1L)).thenReturn(new ArrayList<>(List.of(existingItem)));

        PackOpeningResultDTO result = packService.openPack("lucas", "pack_base");

        assertNotNull(result);
        assertEquals(5, result.cards().size());
        // Verify pack was deducted
        assertEquals(1, user.getPacksInventory().get("pack_base"));
        assertEquals(1, user.getPacks());
        verify(userRepository, times(1)).save(user);
        // Verify inventory is saved for non-duplicate cards
        verify(inventoryRepository, atLeastOnce()).save(any(UserShowcaseInventoryEntity.class));
    }

    @Test
    public void testOpenPackSuccessLegendaryGuaranteed() {
        UserEntity user = UserEntity.builder()
                .id(1L)
                .username("lucas")
                .packsInventory(new HashMap<>(Map.of("pack_legendario", 1)))
                .packs(1)
                .pokecoins(0)
                .build();

        CardEntity card1 = CardEntity.builder().id("card-1").name("Mewtwo EX").subtype("EX").build();
        when(userRepository.findFirstByUsername("lucas")).thenReturn(Optional.of(user));
        when(cardRepository.findAll()).thenReturn(List.of(card1));
        when(inventoryRepository.findByUserId(1L)).thenReturn(new ArrayList<>());

        PackOpeningResultDTO result = packService.openPack("lucas", "pack_legendario");

        assertNotNull(result);
        assertEquals(5, result.cards().size());
        // Legendary pack guarantees first card is legendary and foil
        assertTrue(result.cards().get(0).isFoil());
        assertEquals("card-1", result.cards().get(0).cardId());
    }
}
