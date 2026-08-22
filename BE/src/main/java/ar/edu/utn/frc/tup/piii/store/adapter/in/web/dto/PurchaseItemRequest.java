package ar.edu.utn.frc.tup.piii.store.adapter.in.web.dto;

/** Request body for {@code POST /api/store/buy}. Replaces the pre-refactor {@code BuyRequestDTO}. */
public record PurchaseItemRequest(Long itemId) {
}
