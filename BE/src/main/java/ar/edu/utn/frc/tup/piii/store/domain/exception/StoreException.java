package ar.edu.utn.frc.tup.piii.store.domain.exception;

/**
 * Base type for business-rule violations raised by the store domain/use cases. Kept
 * framework-free (extends {@link RuntimeException} only) so the domain and application layers
 * never need to import a web or persistence type to signal failure.
 *
 * <p>{@code GlobalExceptionHandler} maps this hierarchy to HTTP responses, mirroring the existing
 * convention used for {@code InvalidActionException} and {@code InvalidDeckException}.</p>
 */
public abstract class StoreException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    protected StoreException(final String message) {
        super(message);
    }
}
