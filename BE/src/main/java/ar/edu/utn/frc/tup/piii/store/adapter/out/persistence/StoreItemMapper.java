package ar.edu.utn.frc.tup.piii.store.adapter.out.persistence;

import ar.edu.utn.frc.tup.piii.persistence.entity.StoreItemEntity;
import ar.edu.utn.frc.tup.piii.store.domain.StoreItem;
import ar.edu.utn.frc.tup.piii.store.domain.StoreItemType;
import org.springframework.stereotype.Component;

/**
 * Translates between {@link StoreItemEntity} (JPA) and {@link StoreItem} (domain). This is the
 * only class in the store slice allowed to know about both sides — mirrors the existing
 * {@code persistence.mapper.CardMapper} convention in this codebase.
 */
@Component
public class StoreItemMapper {

    public StoreItem toDomain(final StoreItemEntity entity) {
        return new StoreItem(
                entity.getId(),
                entity.getName(),
                entity.getDescription(),
                entity.getPrice() != null ? entity.getPrice() : 0,
                toDomainType(entity.getItemType()),
                entity.getImageUrl(),
                Boolean.TRUE.equals(entity.getIsActive()));
    }

    private StoreItemType toDomainType(final ar.edu.utn.frc.tup.piii.persistence.entity.StoreItemType entityType) {
        return switch (entityType) {
            case AVATAR -> StoreItemType.AVATAR;
            case TITLE -> StoreItemType.TITLE;
            case PACK -> StoreItemType.PACK;
        };
    }
}
