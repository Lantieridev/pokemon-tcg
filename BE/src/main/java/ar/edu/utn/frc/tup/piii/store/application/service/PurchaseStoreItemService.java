package ar.edu.utn.frc.tup.piii.store.application.service;

import ar.edu.utn.frc.tup.piii.store.application.port.in.PurchaseStoreItemUseCase;
import ar.edu.utn.frc.tup.piii.store.application.port.out.StoreItemRepositoryPort;
import ar.edu.utn.frc.tup.piii.store.application.port.out.UserStoreAccountPort;
import ar.edu.utn.frc.tup.piii.store.domain.StoreItem;
import ar.edu.utn.frc.tup.piii.store.domain.UserStoreAccount;
import ar.edu.utn.frc.tup.piii.store.domain.exception.InsufficientPokecoinsException;
import ar.edu.utn.frc.tup.piii.store.domain.exception.StoreItemAlreadyOwnedException;
import ar.edu.utn.frc.tup.piii.store.domain.exception.StoreItemNotFoundException;
import ar.edu.utn.frc.tup.piii.store.domain.exception.StoreItemUnavailableException;
import ar.edu.utn.frc.tup.piii.store.domain.exception.StoreUserNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Application service backing {@link PurchaseStoreItemUseCase}. Orchestrates the purchase by
 * loading state through the outbound ports, delegating the actual business rules (affordability,
 * ownership, granting) to {@link UserStoreAccount}, and persisting the result — no JPA type is
 * referenced here.
 *
 * <p>{@code @Transactional} is kept at this boundary rather than pushed into the persistence
 * adapter: it mirrors this codebase's existing convention (see the former
 * {@code StoreServiceImpl}) of annotating the use-case-shaped method that must be atomic, and
 * Spring's proxy-based transactions require the annotation on a Spring-managed bean's public
 * method — the application service, not the domain, is the right place for it.</p>
 */
@Service
public class PurchaseStoreItemService implements PurchaseStoreItemUseCase {

    private final StoreItemRepositoryPort storeItemRepositoryPort;
    private final UserStoreAccountPort userStoreAccountPort;

    public PurchaseStoreItemService(final StoreItemRepositoryPort storeItemRepositoryPort,
                                     final UserStoreAccountPort userStoreAccountPort) {
        this.storeItemRepositoryPort = storeItemRepositoryPort;
        this.userStoreAccountPort = userStoreAccountPort;
    }

    @Override
    @Transactional
    public void purchase(final String username, final Long itemId) {
        final UserStoreAccount account = userStoreAccountPort.findByUsername(username)
                .orElseThrow(StoreUserNotFoundException::new);
        final StoreItem item = storeItemRepositoryPort.findById(itemId)
                .orElseThrow(StoreItemNotFoundException::new);

        if (!item.active()) {
            throw new StoreItemUnavailableException();
        }
        if (!account.canAfford(item)) {
            throw new InsufficientPokecoinsException();
        }
        if (account.alreadyOwns(item)) {
            throw new StoreItemAlreadyOwnedException(item.type());
        }

        userStoreAccountPort.save(account.purchase(item));
    }
}
