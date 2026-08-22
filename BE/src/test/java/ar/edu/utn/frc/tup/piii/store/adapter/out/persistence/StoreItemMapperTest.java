package ar.edu.utn.frc.tup.piii.store.adapter.out.persistence;

import ar.edu.utn.frc.tup.piii.persistence.entity.StoreItemEntity;
import ar.edu.utn.frc.tup.piii.store.domain.StoreItem;
import ar.edu.utn.frc.tup.piii.store.domain.StoreItemType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class StoreItemMapperTest {

    private final StoreItemMapper storeItemMapper = new StoreItemMapper();

    @Test
    void mapsAllFieldsAndEachItemType() {
        final StoreItemEntity entity = StoreItemEntity.builder()
                .id(5L)
                .name("Avatar Pikachu")
                .description("desc")
                .price(100)
                .itemType(ar.edu.utn.frc.tup.piii.persistence.entity.StoreItemType.AVATAR)
                .imageUrl("pikachu.png")
                .isActive(true)
                .build();

        final StoreItem item = storeItemMapper.toDomain(entity);

        assertEquals(5L, item.id());
        assertEquals("Avatar Pikachu", item.name());
        assertEquals("desc", item.description());
        assertEquals(100, item.price());
        assertEquals(StoreItemType.AVATAR, item.type());
        assertEquals("pikachu.png", item.imageUrl());
        assertEquals(true, item.active());
    }

    @Test
    void mapsTitleAndPackTypes() {
        final StoreItemEntity title = StoreItemEntity.builder().id(1L).name("VIP").price(1)
                .itemType(ar.edu.utn.frc.tup.piii.persistence.entity.StoreItemType.TITLE).isActive(true).build();
        final StoreItemEntity pack = StoreItemEntity.builder().id(2L).name("Booster").price(1)
                .itemType(ar.edu.utn.frc.tup.piii.persistence.entity.StoreItemType.PACK).isActive(true).build();

        assertEquals(StoreItemType.TITLE, storeItemMapper.toDomain(title).type());
        assertEquals(StoreItemType.PACK, storeItemMapper.toDomain(pack).type());
    }

    @Test
    void treatsNullIsActiveAsInactive() {
        final StoreItemEntity entity = StoreItemEntity.builder().id(1L).name("VIP").price(1)
                .itemType(ar.edu.utn.frc.tup.piii.persistence.entity.StoreItemType.TITLE)
                .isActive(null)
                .build();

        assertFalse(storeItemMapper.toDomain(entity).active());
    }

    @Test
    void treatsNullPriceAsZero() {
        final StoreItemEntity entity = StoreItemEntity.builder().id(1L).name("VIP").price(null)
                .itemType(ar.edu.utn.frc.tup.piii.persistence.entity.StoreItemType.TITLE)
                .isActive(true)
                .build();

        assertEquals(0, storeItemMapper.toDomain(entity).price());
    }
}
