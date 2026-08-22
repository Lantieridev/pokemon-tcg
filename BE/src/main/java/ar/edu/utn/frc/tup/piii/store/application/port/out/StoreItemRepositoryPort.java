package ar.edu.utn.frc.tup.piii.store.application.port.out;

import ar.edu.utn.frc.tup.piii.store.domain.StoreItem;

import java.util.List;
import java.util.Optional;

/**
 * Outbound port: how the store application layer reads catalog items, independent of JPA.
 * Implemented by an adapter in {@code store.adapter.out.persistence}.
 */
public interface StoreItemRepositoryPort {

    List<StoreItem> findAllActive();

    Optional<StoreItem> findById(Long itemId);
}
