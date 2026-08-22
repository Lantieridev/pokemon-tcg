package ar.edu.utn.frc.tup.piii.store.domain.exception;

/**
 * Raised when the purchasing user cannot be resolved. Mapped to 401 by
 * {@code GlobalExceptionHandler}, preserving the behaviour of the previous
 * {@code IllegalArgumentException("Usuario no encontrado")} special-case.
 */
public final class StoreUserNotFoundException extends StoreException {

    private static final long serialVersionUID = 1L;

    public StoreUserNotFoundException() {
        super("Usuario no encontrado");
    }
}
