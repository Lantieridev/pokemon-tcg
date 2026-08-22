package ar.edu.utn.frc.tup.piii.store.adapter.in.web;

import ar.edu.utn.frc.tup.piii.store.adapter.in.web.dto.PurchaseItemRequest;
import ar.edu.utn.frc.tup.piii.store.adapter.in.web.dto.StoreItemResponse;
import ar.edu.utn.frc.tup.piii.store.application.port.in.ListAvailableStoreItemsUseCase;
import ar.edu.utn.frc.tup.piii.store.application.port.in.PurchaseStoreItemUseCase;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Inbound (driving) adapter for the store slice. Depends only on the use case ports —
 * never on a persistence type or a domain service implementation directly.
 */
@RestController
@RequestMapping("/api/store")
public class StoreController {

    private final ListAvailableStoreItemsUseCase listAvailableStoreItemsUseCase;
    private final PurchaseStoreItemUseCase purchaseStoreItemUseCase;

    public StoreController(final ListAvailableStoreItemsUseCase listAvailableStoreItemsUseCase,
                            final PurchaseStoreItemUseCase purchaseStoreItemUseCase) {
        this.listAvailableStoreItemsUseCase = listAvailableStoreItemsUseCase;
        this.purchaseStoreItemUseCase = purchaseStoreItemUseCase;
    }

    @GetMapping("/items")
    public ResponseEntity<List<StoreItemResponse>> getAvailableItems() {
        final List<StoreItemResponse> items = listAvailableStoreItemsUseCase.listAvailableItems().stream()
                .map(StoreItemResponse::from)
                .toList();
        return ResponseEntity.ok(items);
    }

    @PostMapping("/buy")
    public ResponseEntity<Void> buyItem(@RequestBody final PurchaseItemRequest request,
                                         final Authentication authentication) {
        purchaseStoreItemUseCase.purchase(authentication.getName(), request.itemId());
        return ResponseEntity.ok().build();
    }
}
