package ar.edu.utn.frc.tup.piii.store.adapter.out.persistence;

import ar.edu.utn.frc.tup.piii.persistence.entity.StoreItemEntity;
import ar.edu.utn.frc.tup.piii.persistence.repository.StoreItemRepository;
import ar.edu.utn.frc.tup.piii.store.domain.StoreItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class StoreItemPersistenceAdapterTest {

    private StoreItemRepository storeItemRepository;
    private StoreItemPersistenceAdapter adapter;

    @BeforeEach
    void setUp() {
        storeItemRepository = mock(StoreItemRepository.class);
        adapter = new StoreItemPersistenceAdapter(storeItemRepository, new StoreItemMapper());
    }

    private static StoreItemEntity entity(final long id) {
        return StoreItemEntity.builder().id(id).name("VIP").price(10)
                .itemType(ar.edu.utn.frc.tup.piii.persistence.entity.StoreItemType.TITLE)
                .isActive(true)
                .build();
    }

    @Test
    void findAllActiveMapsEveryEntityReturnedByTheRepository() {
        when(storeItemRepository.findAllByIsActiveTrue()).thenReturn(List.of(entity(1L), entity(2L)));

        final List<StoreItem> items = adapter.findAllActive();

        assertEquals(2, items.size());
        assertEquals(1L, items.get(0).id());
        assertEquals(2L, items.get(1).id());
    }

    @Test
    void findByIdMapsThePresentEntity() {
        when(storeItemRepository.findById(1L)).thenReturn(Optional.of(entity(1L)));

        final Optional<StoreItem> result = adapter.findById(1L);

        assertTrue(result.isPresent());
        assertEquals(1L, result.get().id());
    }

    @Test
    void findByIdReturnsEmptyWhenTheRepositoryFindsNothing() {
        when(storeItemRepository.findById(99L)).thenReturn(Optional.empty());

        assertTrue(adapter.findById(99L).isEmpty());
    }
}
