package ar.edu.utn.frc.tup.piii.store.application.port.in;

import ar.edu.utn.frc.tup.piii.store.domain.StoreItem;

import java.util.List;

/** Inbound port: read the catalog of items currently on sale. */
@FunctionalInterface
public interface ListAvailableStoreItemsUseCase {

    List<StoreItem> listAvailableItems();
}
