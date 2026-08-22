package ar.edu.utn.frc.tup.piii.store.domain.exception;

/** Raised when the user's pokecoin balance does not cover the item's price. Mapped to 400. */
public final class InsufficientPokecoinsException extends StoreException {

    private static final long serialVersionUID = 1L;

    public InsufficientPokecoinsException() {
        super("No tienes suficientes pokecoins para comprar este artículo");
    }
}
