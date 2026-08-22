package ar.edu.utn.frc.tup.piii.store.application.service;

import ar.edu.utn.frc.tup.piii.store.application.port.in.ListAvailableStoreItemsUseCase;
import ar.edu.utn.frc.tup.piii.store.application.port.out.StoreItemRepositoryPort;
import ar.edu.utn.frc.tup.piii.store.domain.StoreItem;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Application service backing {@link ListAvailableStoreItemsUseCase}. Depends only on the
 * outbound port, never on JPA or a web type.
 */
@Service
public class StoreCatalogService implements ListAvailableStoreItemsUseCase {

    private final StoreItemRepositoryPort storeItemRepositoryPort;

    public StoreCatalogService(final StoreItemRepositoryPort storeItemRepositoryPort) {
        this.storeItemRepositoryPort = storeItemRepositoryPort;
    }

    @Override
    public List<StoreItem> listAvailableItems() {
        return storeItemRepositoryPort.findAllActive();
    }
}
