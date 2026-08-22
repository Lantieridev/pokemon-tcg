package ar.edu.utn.frc.tup.piii.store.domain.exception;

import ar.edu.utn.frc.tup.piii.store.domain.StoreItemType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

/**
 * Locks in the exact messages the previous {@code StoreServiceImpl} used to throw as
 * {@code IllegalArgumentException}, since {@code GlobalExceptionHandler} now dispatches on type
 * instead of message string, and the HTTP response body text must stay stable for the frontend.
 */
class StoreExceptionsTest {

    @Test
    void userNotFoundKeepsOriginalMessageAndIsAStoreException() {
        final StoreUserNotFoundException ex = new StoreUserNotFoundException();
        assertEquals("Usuario no encontrado", ex.getMessage());
        assertInstanceOf(StoreException.class, ex);
    }

    @Test
    void itemNotFoundKeepsOriginalMessage() {
        assertEquals("Artículo no encontrado", new StoreItemNotFoundException().getMessage());
    }

    @Test
    void itemUnavailableKeepsOriginalMessage() {
        assertEquals("El artículo ya no está disponible", new StoreItemUnavailableException().getMessage());
    }

    @Test
    void insufficientPokecoinsKeepsOriginalMessage() {
        assertEquals("No tienes suficientes pokecoins para comprar este artículo",
                new InsufficientPokecoinsException().getMessage());
    }

    @Test
    void alreadyOwnedTitleMessageMentionsTitle() {
        assertEquals("Ya posees este título",
                new StoreItemAlreadyOwnedException(StoreItemType.TITLE).getMessage());
    }

    @Test
    void alreadyOwnedAvatarMessageMentionsAvatar() {
        assertEquals("Ya posees este avatar",
                new StoreItemAlreadyOwnedException(StoreItemType.AVATAR).getMessage());
    }

    @Test
    void alreadyOwnedPackHasAGenericMessage() {
        assertEquals("Ya posees este artículo",
                new StoreItemAlreadyOwnedException(StoreItemType.PACK).getMessage());
    }
}
