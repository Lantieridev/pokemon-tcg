package ar.edu.utn.frc.tup.piii.store.adapter.out.persistence;

import ar.edu.utn.frc.tup.piii.persistence.entity.UserEntity;
import ar.edu.utn.frc.tup.piii.persistence.repository.UserRepository;
import ar.edu.utn.frc.tup.piii.store.domain.UserStoreAccount;
import ar.edu.utn.frc.tup.piii.store.domain.exception.StoreUserNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserStoreAccountPersistenceAdapterTest {

    private UserRepository userRepository;
    private UserStoreAccountPersistenceAdapter adapter;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        adapter = new UserStoreAccountPersistenceAdapter(userRepository, new UserStoreAccountMapper());
    }

    @Test
    void findByUsernameMapsThePresentEntity() {
        final UserEntity entity = UserEntity.builder().username("lucas").pokecoins(100).build();
        when(userRepository.findFirstByUsername("lucas")).thenReturn(Optional.of(entity));

        final Optional<UserStoreAccount> result = adapter.findByUsername("lucas");

        assertTrue(result.isPresent());
        assertEquals(100, result.get().pokecoinBalance());
    }

    @Test
    void findByUsernameReturnsEmptyWhenNotFound() {
        when(userRepository.findFirstByUsername("ghost")).thenReturn(Optional.empty());

        assertTrue(adapter.findByUsername("ghost").isEmpty());
    }

    @Test
    void saveReloadsTheEntityAndPersistsOnlyStoreRelevantFields() {
        final UserEntity entity = UserEntity.builder()
                .username("lucas")
                .pokecoins(200)
                .mmr(1500)
                .unlockedTitles(new HashSet<>())
                .unlockedAvatars(new HashSet<>())
                .packsInventory(new HashMap<>())
                .build();
        when(userRepository.findFirstByUsername("lucas")).thenReturn(Optional.of(entity));

        final UserStoreAccount account = new UserStoreAccount("lucas", 100,
                Set.of("VIP"), Set.of(), "default_trainer", 0, Map.of());
        adapter.save(account);

        assertEquals(100, entity.getPokecoins());
        assertTrue(entity.getUnlockedTitles().contains("VIP"));
        assertEquals(1500, entity.getMmr(), "fields outside the store's concern must be left untouched");
        verify(userRepository, times(1)).save(entity);
    }

    @Test
    void saveThrowsWhenTheUnderlyingUserDisappearedBetweenLookups() {
        when(userRepository.findFirstByUsername("ghost")).thenReturn(Optional.empty());
        final UserStoreAccount account =
                new UserStoreAccount("ghost", 0, Set.of(), Set.of(), null, 0, Map.of());

        assertThrows(StoreUserNotFoundException.class, () -> adapter.save(account));
    }
}
