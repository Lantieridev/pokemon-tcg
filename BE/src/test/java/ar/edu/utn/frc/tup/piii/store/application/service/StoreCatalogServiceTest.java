package ar.edu.utn.frc.tup.piii.store.application.service;

import ar.edu.utn.frc.tup.piii.store.application.port.out.StoreItemRepositoryPort;
import ar.edu.utn.frc.tup.piii.store.domain.StoreItem;
import ar.edu.utn.frc.tup.piii.store.domain.StoreItemType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class StoreCatalogServiceTest {

    private StoreItemRepositoryPort storeItemRepositoryPort;
    private StoreCatalogService storeCatalogService;

    @BeforeEach
    void setUp() {
        storeItemRepositoryPort = mock(StoreItemRepositoryPort.class);
        storeCatalogService = new StoreCatalogService(storeItemRepositoryPort);
    }

    @Test
    void delegatesToTheOutboundPortAndReturnsWhatItProvides() {
        final StoreItem item = new StoreItem(1L, "Avatar Pikachu", "desc", 100,
                StoreItemType.AVATAR, "pikachu.png", true);
        when(storeItemRepositoryPort.findAllActive()).thenReturn(List.of(item));

        final List<StoreItem> result = storeCatalogService.listAvailableItems();

        assertEquals(1, result.size());
        assertEquals(item, result.get(0));
    }
}
