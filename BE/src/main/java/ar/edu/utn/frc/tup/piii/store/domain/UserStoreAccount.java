package ar.edu.utn.frc.tup.piii.store.domain;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Immutable snapshot of the slice of a user's state that the store domain cares about: their
 * pokecoin balance and their unlocked titles/avatars/packs. This is a narrow, purpose-built view
 * of the shared {@code UserEntity} aggregate — the store does not own the user, so it never
 * models the full user here, only what a purchase needs to read and update.
 *
 * <p>All ownership/affordability rules and the purchase transformation itself live on this type
 * so they can be unit tested without mocking a repository or a Spring context.</p>
 */
public record UserStoreAccount(String username, int pokecoinBalance, Set<String> unlockedTitles,
                                Set<String> unlockedAvatars, String equippedAvatarIcon, int totalPacks,
                                Map<String, Integer> packsInventory) {

    private static final String DEFAULT_PACK_TYPE = "pack_base";

    public UserStoreAccount {
        Objects.requireNonNull(username, "username must not be null");
        unlockedTitles = Set.copyOf(unlockedTitles);
        unlockedAvatars = Set.copyOf(unlockedAvatars);
        packsInventory = Map.copyOf(packsInventory);
    }

    /** Whether the user already owns a non-repeatable item (titles and avatars; packs stack). */
    public boolean alreadyOwns(final StoreItem item) {
        return switch (item.type()) {
            case TITLE -> unlockedTitles.contains(item.name());
            case AVATAR -> unlockedAvatars.contains(item.name()) || item.imageUrl().equals(equippedAvatarIcon);
            case PACK -> false;
        };
    }

    /** Whether the current balance covers the item's price. */
    public boolean canAfford(final StoreItem item) {
        return pokecoinBalance >= item.price();
    }

    /**
     * Returns a new account reflecting the purchase of {@code item}: pokecoins debited and the
     * item granted (title/avatar unlocked, or a pack added to the inventory).
     */
    public UserStoreAccount purchase(final StoreItem item) {
        final int newBalance = pokecoinBalance - item.price();
        return switch (item.type()) {
            case TITLE -> withNewBalance(newBalance).withUnlockedTitle(item.name());
            case AVATAR -> withNewBalance(newBalance).withUnlockedAvatar(item.name());
            case PACK -> withNewBalance(newBalance).withGrantedPack(item.imageUrl());
        };
    }

    private UserStoreAccount withNewBalance(final int newBalance) {
        return new UserStoreAccount(username, newBalance, unlockedTitles, unlockedAvatars,
                equippedAvatarIcon, totalPacks, packsInventory);
    }

    private UserStoreAccount withUnlockedTitle(final String title) {
        final Set<String> updated = new HashSet<>(unlockedTitles);
        updated.add(title);
        return new UserStoreAccount(username, pokecoinBalance, updated, unlockedAvatars,
                equippedAvatarIcon, totalPacks, packsInventory);
    }

    private UserStoreAccount withUnlockedAvatar(final String avatar) {
        final Set<String> updated = new HashSet<>(unlockedAvatars);
        updated.add(avatar);
        return new UserStoreAccount(username, pokecoinBalance, unlockedTitles, updated,
                equippedAvatarIcon, totalPacks, packsInventory);
    }

    private UserStoreAccount withGrantedPack(final String imageUrl) {
        final String packType = imageUrl != null ? imageUrl : DEFAULT_PACK_TYPE;
        final Map<String, Integer> updated = new HashMap<>(packsInventory);
        updated.merge(packType, 1, Integer::sum);
        return new UserStoreAccount(username, pokecoinBalance, unlockedTitles, unlockedAvatars,
                equippedAvatarIcon, totalPacks + 1, updated);
    }
}
