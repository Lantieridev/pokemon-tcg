package ar.edu.utn.frc.tup.piii.store.application.port.out;

import ar.edu.utn.frc.tup.piii.store.domain.UserStoreAccount;

import java.util.Optional;

/**
 * Outbound port: the narrow slice of user state the store needs, independent of JPA. The user
 * aggregate itself belongs to another bounded context (auth/profile) — this port only exposes
 * what a purchase reads and writes, deliberately not the whole {@code UserEntity}.
 */
public interface UserStoreAccountPort {

    Optional<UserStoreAccount> findByUsername(String username);

    /** Persists the account's store-relevant fields (balance, unlocked items, pack inventory). */
    void save(UserStoreAccount account);
}
