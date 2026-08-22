package ar.edu.utn.frc.tup.piii.store.application.service;

import ar.edu.utn.frc.tup.piii.store.application.port.out.StoreItemRepositoryPort;
import ar.edu.utn.frc.tup.piii.store.application.port.out.UserStoreAccountPort;
import ar.edu.utn.frc.tup.piii.store.domain.StoreItem;
import ar.edu.utn.frc.tup.piii.store.domain.StoreItemType;
import ar.edu.utn.frc.tup.piii.store.domain.UserStoreAccount;
import ar.edu.utn.frc.tup.piii.store.domain.exception.InsufficientPokecoinsException;
import ar.edu.utn.frc.tup.piii.store.domain.exception.StoreItemAlreadyOwnedException;
import ar.edu.utn.frc.tup.piii.store.domain.exception.StoreItemNotFoundException;
import ar.edu.utn.frc.tup.piii.store.domain.exception.StoreItemUnavailableException;
import ar.edu.utn.frc.tup.piii.store.domain.exception.StoreUserNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PurchaseStoreItemServiceTest {

    private static final Long ITEM_ID = 1L;
    private static final String USERNAME = "lucas";

    private StoreItemRepositoryPort storeItemRepositoryPort;
    private UserStoreAccountPort userStoreAccountPort;
    private PurchaseStoreItemService purchaseStoreItemService;

    @BeforeEach
    void setUp() {
        storeItemRepositoryPort = mock(StoreItemRepositoryPort.class);
        userStoreAccountPort = mock(UserStoreAccountPort.class);
        purchaseStoreItemService = new PurchaseStoreItemService(storeItemRepositoryPort, userStoreAccountPort);
    }

    private static UserStoreAccount account(final int balance) {
        return new UserStoreAccount(USERNAME, balance, Set.of(), Set.of(), "default_trainer", 0, Map.of());
    }

    @Test
    void purchasingAnAvatarDebitsAndUnlocksIt() {
        final StoreItem item = new StoreItem(ITEM_ID, "Avatar Pikachu", "desc", 100,
                StoreItemType.AVATAR, "pikachu.png", true);
        when(userStoreAccountPort.findByUsername(USERNAME)).thenReturn(Optional.of(account(200)));
        when(storeItemRepositoryPort.findById(ITEM_ID)).thenReturn(Optional.of(item));

        purchaseStoreItemService.purchase(USERNAME, ITEM_ID);

        final ArgumentCaptor<UserStoreAccount> captor = ArgumentCaptor.forClass(UserStoreAccount.class);
        verify(userStoreAccountPort).save(captor.capture());
        assertEquals(100, captor.getValue().pokecoinBalance());
        assertTrue(captor.getValue().unlockedAvatars().contains("Avatar Pikachu"));
    }

    @Test
    void purchasingAPackAccumulatesInventory() {
        final StoreItem item = new StoreItem(ITEM_ID, "Booster", "desc", 30,
                StoreItemType.PACK, "pack_special", true);
        when(userStoreAccountPort.findByUsername(USERNAME)).thenReturn(Optional.of(account(200)));
        when(storeItemRepositoryPort.findById(ITEM_ID)).thenReturn(Optional.of(item));

        purchaseStoreItemService.purchase(USERNAME, ITEM_ID);

        final ArgumentCaptor<UserStoreAccount> captor = ArgumentCaptor.forClass(UserStoreAccount.class);
        verify(userStoreAccountPort).save(captor.capture());
        assertEquals(1, captor.getValue().totalPacks());
        assertEquals(1, captor.getValue().packsInventory().get("pack_special"));
    }

    @Test
    void throwsWhenUserCannotBeResolved() {
        when(userStoreAccountPort.findByUsername(USERNAME)).thenReturn(Optional.empty());

        assertThrows(StoreUserNotFoundException.class,
                () -> purchaseStoreItemService.purchase(USERNAME, ITEM_ID));
        verify(storeItemRepositoryPort, never()).findById(any());
        verify(userStoreAccountPort, never()).save(any());
    }

    @Test
    void throwsWhenItemDoesNotExist() {
        when(userStoreAccountPort.findByUsername(USERNAME)).thenReturn(Optional.of(account(200)));
        when(storeItemRepositoryPort.findById(ITEM_ID)).thenReturn(Optional.empty());

        assertThrows(StoreItemNotFoundException.class,
                () -> purchaseStoreItemService.purchase(USERNAME, ITEM_ID));
        verify(userStoreAccountPort, never()).save(any());
    }

    @Test
    void throwsWhenItemIsInactive() {
        final StoreItem inactiveItem = new StoreItem(ITEM_ID, "Old Item", "desc", 10,
                StoreItemType.TITLE, null, false);
        when(userStoreAccountPort.findByUsername(USERNAME)).thenReturn(Optional.of(account(200)));
        when(storeItemRepositoryPort.findById(ITEM_ID)).thenReturn(Optional.of(inactiveItem));

        assertThrows(StoreItemUnavailableException.class,
                () -> purchaseStoreItemService.purchase(USERNAME, ITEM_ID));
        verify(userStoreAccountPort, never()).save(any());
    }

    @Test
    void throwsWhenBalanceIsInsufficient() {
        final StoreItem item = new StoreItem(ITEM_ID, "VIP", "desc", 100, StoreItemType.TITLE, null, true);
        when(userStoreAccountPort.findByUsername(USERNAME)).thenReturn(Optional.of(account(50)));
        when(storeItemRepositoryPort.findById(ITEM_ID)).thenReturn(Optional.of(item));

        assertThrows(InsufficientPokecoinsException.class,
                () -> purchaseStoreItemService.purchase(USERNAME, ITEM_ID));
        verify(userStoreAccountPort, never()).save(any());
    }

    @Test
    void throwsWhenTitleIsAlreadyOwned() {
        final StoreItem item = new StoreItem(ITEM_ID, "VIP", "desc", 100, StoreItemType.TITLE, null, true);
        final UserStoreAccount owner =
                new UserStoreAccount(USERNAME, 200, Set.of("VIP"), Set.of(), "default_trainer", 0, Map.of());
        when(userStoreAccountPort.findByUsername(USERNAME)).thenReturn(Optional.of(owner));
        when(storeItemRepositoryPort.findById(ITEM_ID)).thenReturn(Optional.of(item));

        assertThrows(StoreItemAlreadyOwnedException.class,
                () -> purchaseStoreItemService.purchase(USERNAME, ITEM_ID));
        verify(userStoreAccountPort, never()).save(any());
    }

    @Test
    void throwsWhenAvatarIsAlreadyEquipped() {
        final StoreItem item = new StoreItem(ITEM_ID, "Avatar Pikachu", "desc", 100,
                StoreItemType.AVATAR, "pikachu.png", true);
        final UserStoreAccount owner =
                new UserStoreAccount(USERNAME, 200, Set.of(), Set.of(), "pikachu.png", 0, Map.of());
        when(userStoreAccountPort.findByUsername(USERNAME)).thenReturn(Optional.of(owner));
        when(storeItemRepositoryPort.findById(ITEM_ID)).thenReturn(Optional.of(item));

        assertThrows(StoreItemAlreadyOwnedException.class,
                () -> purchaseStoreItemService.purchase(USERNAME, ITEM_ID));
    }

    @Test
    void savesUnderTheSameUsernameThatWasQueried() {
        final StoreItem item = new StoreItem(ITEM_ID, "VIP", "desc", 10, StoreItemType.TITLE, null, true);
        when(userStoreAccountPort.findByUsername(USERNAME)).thenReturn(Optional.of(account(200)));
        when(storeItemRepositoryPort.findById(ITEM_ID)).thenReturn(Optional.of(item));

        purchaseStoreItemService.purchase(USERNAME, ITEM_ID);

        verify(userStoreAccountPort).save(argThat(a -> a.username().equals(USERNAME)));
        verify(storeItemRepositoryPort).findById(eq(ITEM_ID));
    }
}
