package ar.edu.utn.frc.tup.piii.store.adapter.out.persistence;

import ar.edu.utn.frc.tup.piii.persistence.repository.StoreItemRepository;
import ar.edu.utn.frc.tup.piii.store.application.port.out.StoreItemRepositoryPort;
import ar.edu.utn.frc.tup.piii.store.domain.StoreItem;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/** Outbound (driven) adapter: implements {@link StoreItemRepositoryPort} on top of Spring Data JPA. */
@Component
public class StoreItemPersistenceAdapter implements StoreItemRepositoryPort {

    private final StoreItemRepository storeItemRepository;
    private final StoreItemMapper storeItemMapper;

    public StoreItemPersistenceAdapter(final StoreItemRepository storeItemRepository,
                                        final StoreItemMapper storeItemMapper) {
        this.storeItemRepository = storeItemRepository;
        this.storeItemMapper = storeItemMapper;
    }

    @Override
    public List<StoreItem> findAllActive() {
        return storeItemRepository.findAllByIsActiveTrue().stream()
                .map(storeItemMapper::toDomain)
                .toList();
    }

    @Override
    public Optional<StoreItem> findById(final Long itemId) {
        return storeItemRepository.findById(itemId).map(storeItemMapper::toDomain);
    }
}
