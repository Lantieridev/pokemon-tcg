package ar.edu.utn.frc.tup.piii.store.domain.exception;

import ar.edu.utn.frc.tup.piii.store.domain.StoreItemType;

/**
 * Raised when the user already owns a non-repeatable item (a title or an avatar). Mapped to 400
 * by {@code GlobalExceptionHandler}. The message mirrors the item type, as the previous
 * implementation did.
 */
public final class StoreItemAlreadyOwnedException extends StoreException {

    private static final long serialVersionUID = 1L;

    public StoreItemAlreadyOwnedException(final StoreItemType itemType) {
        super(messageFor(itemType));
    }

    private static String messageFor(final StoreItemType itemType) {
        return switch (itemType) {
            case TITLE -> "Ya posees este título";
            case AVATAR -> "Ya posees este avatar";
            case PACK -> "Ya posees este artículo";
        };
    }
}
