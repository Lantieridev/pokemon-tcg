package ar.edu.utn.frc.tup.piii.store.adapter.in.web.dto;

import ar.edu.utn.frc.tup.piii.store.domain.StoreItem;
import ar.edu.utn.frc.tup.piii.store.domain.StoreItemType;

/**
 * Web-facing representation of a {@link StoreItem}. Field names/shape are unchanged from the
 * pre-refactor {@code StoreItemDTO} so the frontend contract on {@code GET /api/store/items} is
 * unaffected.
 */
public record StoreItemResponse(Long id, String name, String description, int price,
                                 StoreItemType itemType, String imageUrl) {

    public static StoreItemResponse from(final StoreItem item) {
        return new StoreItemResponse(item.id(), item.name(), item.description(), item.price(),
                item.type(), item.imageUrl());
    }
}
