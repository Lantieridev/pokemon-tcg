package ar.edu.utn.frc.tup.piii.store.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class StoreItemTest {

    @Test
    void buildsWithAllFields() {
        final StoreItem item = new StoreItem(1L, "Avatar Pikachu", "desc", 100,
                StoreItemType.AVATAR, "pikachu.png", true);

        assertEquals(1L, item.id());
        assertEquals("Avatar Pikachu", item.name());
        assertEquals("desc", item.description());
        assertEquals(100, item.price());
        assertEquals(StoreItemType.AVATAR, item.type());
        assertEquals("pikachu.png", item.imageUrl());
        assertEquals(true, item.active());
    }

    @Test
    void rejectsNullId() {
        assertThrows(NullPointerException.class,
                () -> new StoreItem(null, "name", "desc", 10, StoreItemType.TITLE, null, true));
    }

    @Test
    void rejectsNullName() {
        assertThrows(NullPointerException.class,
                () -> new StoreItem(1L, null, "desc", 10, StoreItemType.TITLE, null, true));
    }

    @Test
    void rejectsNullType() {
        assertThrows(NullPointerException.class,
                () -> new StoreItem(1L, "name", "desc", 10, null, null, true));
    }

    @Test
    void rejectsNegativePrice() {
        assertThrows(IllegalArgumentException.class,
                () -> new StoreItem(1L, "name", "desc", -1, StoreItemType.TITLE, null, true));
    }
}
