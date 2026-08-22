package ar.edu.utn.frc.tup.piii.store.domain;

/**
 * The kind of item sold in the store. Deliberately independent from
 * {@link ar.edu.utn.frc.tup.piii.persistence.entity.StoreItemType}, the JPA-facing enum with the
 * same name — the domain layer must not depend on persistence types. The two are reconciled by
 * {@code StoreItemMapper} at the persistence adapter boundary.
 */
public enum StoreItemType {
    AVATAR,
    TITLE,
    PACK
}
