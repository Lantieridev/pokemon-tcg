package ar.edu.utn.frc.tup.piii.store.domain;

import java.util.Objects;

/**
 * Immutable domain representation of a catalog item that can be purchased with pokecoins.
 * Framework-free: no JPA annotations, no Spring dependency. Persistence adapters translate
 * to/from {@link ar.edu.utn.frc.tup.piii.persistence.entity.StoreItemEntity}.
 */
public record StoreItem(Long id, String name, String description, int price, StoreItemType type,
                         String imageUrl, boolean active) {

    public StoreItem {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(type, "type must not be null");
        if (price < 0) {
            throw new IllegalArgumentException("price must not be negative");
        }
    }
}
