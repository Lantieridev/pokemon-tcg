package ar.edu.utn.frc.tup.piii.store.adapter.out.persistence;

import ar.edu.utn.frc.tup.piii.persistence.entity.UserEntity;
import ar.edu.utn.frc.tup.piii.store.domain.UserStoreAccount;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserStoreAccountMapperTest {

    private final UserStoreAccountMapper mapper = new UserStoreAccountMapper();

    @Test
    void mapsAllFieldsToDomain() {
        final UserEntity entity = UserEntity.builder()
                .username("lucas")
                .pokecoins(150)
                .unlockedTitles(new HashSet<>(Set.of("VIP")))
                .unlockedAvatars(new HashSet<>(Set.of("Pikachu")))
                .avatarIcon("pikachu.png")
                .packs(3)
                .packsInventory(new HashMap<>(Map.of("pack_base", 3)))
                .build();

        final UserStoreAccount account = mapper.toDomain(entity);

        assertEquals("lucas", account.username());
        assertEquals(150, account.pokecoinBalance());
        assertTrue(account.unlockedTitles().contains("VIP"));
        assertTrue(account.unlockedAvatars().contains("Pikachu"));
        assertEquals("pikachu.png", account.equippedAvatarIcon());
        assertEquals(3, account.totalPacks());
        assertEquals(3, account.packsInventory().get("pack_base"));
    }

    @Test
    void treatsNullNumericAndCollectionFieldsAsEmptyDefaults() {
        final UserEntity entity = UserEntity.builder()
                .username("lucas")
                .pokecoins(null)
                .unlockedTitles(null)
                .unlockedAvatars(null)
                .packs(null)
                .packsInventory(null)
                .build();

        final UserStoreAccount account = mapper.toDomain(entity);

        assertEquals(0, account.pokecoinBalance());
        assertEquals(0, account.totalPacks());
        assertTrue(account.unlockedTitles().isEmpty());
        assertTrue(account.unlockedAvatars().isEmpty());
        assertTrue(account.packsInventory().isEmpty());
    }

    @Test
    void applyToEntityMutatesTheManagedCollectionsInPlaceRatherThanReplacingThem() {
        final Set<String> managedTitles = new HashSet<>(Set.of("Old Title"));
        final Set<String> managedAvatars = new HashSet<>();
        final Map<String, Integer> managedPacks = new HashMap<>();
        final UserEntity entity = UserEntity.builder()
                .username("lucas")
                .unlockedTitles(managedTitles)
                .unlockedAvatars(managedAvatars)
                .packsInventory(managedPacks)
                .build();

        final UserStoreAccount account = new UserStoreAccount("lucas", 50,
                Set.of("New Title"), Set.of("New Avatar"), "pikachu.png", 2, Map.of("pack_base", 2));

        mapper.applyToEntity(account, entity);

        assertEquals(50, entity.getPokecoins());
        assertEquals(2, entity.getPacks());
        assertEquals(Set.of("New Title"), entity.getUnlockedTitles());
        assertEquals(Set.of("New Avatar"), entity.getUnlockedAvatars());
        assertEquals(Map.of("pack_base", 2), entity.getPacksInventory());
        assertSame(managedTitles, entity.getUnlockedTitles(),
                "must mutate the same collection instance Hibernate manages, not replace it");
        assertSame(managedAvatars, entity.getUnlockedAvatars());
        assertSame(managedPacks, entity.getPacksInventory());
    }
}
