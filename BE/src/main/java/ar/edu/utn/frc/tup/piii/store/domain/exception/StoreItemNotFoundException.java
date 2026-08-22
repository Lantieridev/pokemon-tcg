package ar.edu.utn.frc.tup.piii.store.domain.exception;

/** Raised when the requested store item does not exist. Mapped to 400 by {@code GlobalExceptionHandler}. */
public final class StoreItemNotFoundException extends StoreException {

    private static final long serialVersionUID = 1L;

    public StoreItemNotFoundException() {
        super("Artículo no encontrado");
    }
}
