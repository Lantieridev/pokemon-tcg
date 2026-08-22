package ar.edu.utn.frc.tup.piii.store.application.port.in;

/** Inbound port: spend a user's pokecoins on a store item. */
public interface PurchaseStoreItemUseCase {

    /**
     * @param username the buyer, as resolved from the authenticated principal
     * @param itemId   the catalog item being purchased
     */
    void purchase(String username, Long itemId);
}
