package ar.edu.utn.frc.tup.piii.store.adapter.out.persistence;

import ar.edu.utn.frc.tup.piii.persistence.entity.UserEntity;
import ar.edu.utn.frc.tup.piii.store.domain.UserStoreAccount;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Translates between {@link UserEntity} (JPA, owned by the auth/profile context) and
 * {@link UserStoreAccount} (the store domain's narrow view of a user). {@code applyToEntity}
 * mutates the entity's existing {@code @ElementCollection} fields in place rather than replacing
 * them, matching how Hibernate expects those managed collections to be updated and how the
 * pre-refactor code already mutated them.
 */
@Component
public class UserStoreAccountMapper {

    public UserStoreAccount toDomain(final UserEntity entity) {
        return new UserStoreAccount(
                entity.getUsername(),
                entity.getPokecoins() != null ? entity.getPokecoins() : 0,
                entity.getUnlockedTitles() != null ? entity.getUnlockedTitles() : Set.of(),
                entity.getUnlockedAvatars() != null ? entity.getUnlockedAvatars() : Set.of(),
                entity.getAvatarIcon(),
                entity.getPacks() != null ? entity.getPacks() : 0,
                entity.getPacksInventory() != null ? entity.getPacksInventory() : Map.of());
    }

    public void applyToEntity(final UserStoreAccount account, final UserEntity entity) {
        entity.setPokecoins(account.pokecoinBalance());
        entity.setPacks(account.totalPacks());
        syncSet(entity.getUnlockedTitles(), account.unlockedTitles());
        syncSet(entity.getUnlockedAvatars(), account.unlockedAvatars());
        syncMap(entity.getPacksInventory(), account.packsInventory());
    }

    private void syncSet(final Set<String> managedCollection, final Set<String> desiredState) {
        managedCollection.clear();
        managedCollection.addAll(new HashSet<>(desiredState));
    }

    private void syncMap(final Map<String, Integer> managedCollection, final Map<String, Integer> desiredState) {
        managedCollection.clear();
        managedCollection.putAll(new HashMap<>(desiredState));
    }
}
