package ar.edu.utn.frc.tup.piii.store.domain.exception;

/** Raised when a store item exists but has been deactivated. Mapped to 400 by {@code GlobalExceptionHandler}. */
public final class StoreItemUnavailableException extends StoreException {

    private static final long serialVersionUID = 1L;

    public StoreItemUnavailableException() {
        super("El artículo ya no está disponible");
    }
}
