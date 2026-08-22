package ar.edu.utn.frc.tup.piii.store.adapter.out.persistence;

import ar.edu.utn.frc.tup.piii.persistence.entity.UserEntity;
import ar.edu.utn.frc.tup.piii.persistence.repository.UserRepository;
import ar.edu.utn.frc.tup.piii.store.application.port.out.UserStoreAccountPort;
import ar.edu.utn.frc.tup.piii.store.domain.UserStoreAccount;
import ar.edu.utn.frc.tup.piii.store.domain.exception.StoreUserNotFoundException;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Outbound (driven) adapter: implements {@link UserStoreAccountPort} on top of the shared
 * {@link UserRepository}. Only touches the store-relevant fields of {@link UserEntity} — every
 * other field on that shared aggregate is left untouched.
 */
@Component
public class UserStoreAccountPersistenceAdapter implements UserStoreAccountPort {

    private final UserRepository userRepository;
    private final UserStoreAccountMapper userStoreAccountMapper;

    public UserStoreAccountPersistenceAdapter(final UserRepository userRepository,
                                               final UserStoreAccountMapper userStoreAccountMapper) {
        this.userRepository = userRepository;
        this.userStoreAccountMapper = userStoreAccountMapper;
    }

    @Override
    public Optional<UserStoreAccount> findByUsername(final String username) {
        return userRepository.findFirstByUsername(username).map(userStoreAccountMapper::toDomain);
    }

    @Override
    public void save(final UserStoreAccount account) {
        final UserEntity entity = userRepository.findFirstByUsername(account.username())
                .orElseThrow(StoreUserNotFoundException::new);
        userStoreAccountMapper.applyToEntity(account, entity);
        userRepository.save(entity);
    }
}
