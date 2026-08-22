package ar.edu.utn.frc.tup.piii.store.domain;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserStoreAccountTest {

    private static final StoreItem AVATAR_ITEM =
            new StoreItem(1L, "Avatar Pikachu", "desc", 100, StoreItemType.AVATAR, "pikachu.png", true);
    private static final StoreItem TITLE_ITEM =
            new StoreItem(2L, "VIP", "desc", 50, StoreItemType.TITLE, null, true);
    private static final StoreItem PACK_ITEM =
            new StoreItem(3L, "Booster", "desc", 30, StoreItemType.PACK, "pack_special", true);
    private static final StoreItem PACK_ITEM_NO_IMAGE =
            new StoreItem(4L, "Booster", "desc", 30, StoreItemType.PACK, null, true);

    private static UserStoreAccount freshAccount(final int balance) {
        return new UserStoreAccount("lucas", balance, Set.of(), Set.of(), "default_trainer", 0, Map.of());
    }

    @Test
    void rejectsNullUsername() {
        assertThrows(NullPointerException.class,
                () -> new UserStoreAccount(null, 0, Set.of(), Set.of(), "default", 0, Map.of()));
    }

    @Test
    void collectionsAreDefensivelyCopiedAndImmutable() {
        final UserStoreAccount account = freshAccount(100);
        assertThrows(UnsupportedOperationException.class, () -> account.unlockedTitles().add("x"));
        assertThrows(UnsupportedOperationException.class, () -> account.unlockedAvatars().add("x"));
        assertThrows(UnsupportedOperationException.class, () -> account.packsInventory().put("x", 1));
    }

    @Test
    void alreadyOwnsTitleWhenPresentInUnlockedTitles() {
        final UserStoreAccount account =
                new UserStoreAccount("lucas", 100, Set.of("VIP"), Set.of(), "default", 0, Map.of());
        assertTrue(account.alreadyOwns(TITLE_ITEM));
    }

    @Test
    void doesNotOwnTitleWhenAbsent() {
        assertFalse(freshAccount(100).alreadyOwns(TITLE_ITEM));
    }

    @Test
    void alreadyOwnsAvatarWhenUnlocked() {
        final UserStoreAccount account =
                new UserStoreAccount("lucas", 100, Set.of(), Set.of("Avatar Pikachu"), "default", 0, Map.of());
        assertTrue(account.alreadyOwns(AVATAR_ITEM));
    }

    @Test
    void alreadyOwnsAvatarWhenCurrentlyEquipped() {
        final UserStoreAccount account =
                new UserStoreAccount("lucas", 100, Set.of(), Set.of(), "pikachu.png", 0, Map.of());
        assertTrue(account.alreadyOwns(AVATAR_ITEM));
    }

    @Test
    void doesNotOwnAvatarWhenNeitherUnlockedNorEquipped() {
        assertFalse(freshAccount(100).alreadyOwns(AVATAR_ITEM));
    }

    @Test
    void packsAreNeverAlreadyOwned() {
        assertFalse(freshAccount(100).alreadyOwns(PACK_ITEM));
    }

    @Test
    void canAffordWhenBalanceCoversPrice() {
        assertTrue(freshAccount(100).canAfford(TITLE_ITEM));
        assertTrue(freshAccount(50).canAfford(TITLE_ITEM));
    }

    @Test
    void cannotAffordWhenBalanceBelowPrice() {
        assertFalse(freshAccount(49).canAfford(TITLE_ITEM));
    }

    @Test
    void purchaseTitleDebitsBalanceAndUnlocksTitle() {
        final UserStoreAccount before = freshAccount(200);

        final UserStoreAccount after = before.purchase(TITLE_ITEM);

        assertEquals(150, after.pokecoinBalance());
        assertTrue(after.unlockedTitles().contains("VIP"));
        assertEquals(0, before.unlockedTitles().size(), "original instance must stay unchanged");
        assertEquals(200, before.pokecoinBalance(), "original instance must stay unchanged");
    }

    @Test
    void purchaseAvatarDebitsBalanceAndUnlocksAvatar() {
        final UserStoreAccount after = freshAccount(200).purchase(AVATAR_ITEM);

        assertEquals(100, after.pokecoinBalance());
        assertTrue(after.unlockedAvatars().contains("Avatar Pikachu"));
    }

    @Test
    void purchasePackDebitsBalanceAndAddsToInventoryUnderItsImage() {
        final UserStoreAccount after = freshAccount(200).purchase(PACK_ITEM);

        assertEquals(170, after.pokecoinBalance());
        assertEquals(1, after.totalPacks());
        assertEquals(1, after.packsInventory().get("pack_special"));
    }

    @Test
    void purchasingSamePackTwiceAccumulatesInInventory() {
        final UserStoreAccount afterOne = freshAccount(200).purchase(PACK_ITEM);
        final UserStoreAccount afterTwo = afterOne.purchase(PACK_ITEM);

        assertEquals(2, afterTwo.totalPacks());
        assertEquals(2, afterTwo.packsInventory().get("pack_special"));
    }

    @Test
    void purchasePackWithoutImageFallsBackToDefaultPackType() {
        final UserStoreAccount after = freshAccount(200).purchase(PACK_ITEM_NO_IMAGE);

        assertEquals(1, after.packsInventory().get("pack_base"));
    }
}
