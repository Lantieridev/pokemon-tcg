package ar.edu.utn.frc.tup.piii.controllers;

import ar.edu.utn.frc.tup.piii.dtos.BuyRequestDTO;
import ar.edu.utn.frc.tup.piii.dtos.StoreItemDTO;
import ar.edu.utn.frc.tup.piii.services.StoreService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/store")
public class StoreController {

    private final StoreService storeService;

    public StoreController(StoreService storeService) {
        this.storeService = storeService;
    }

    @GetMapping("/items")
    public ResponseEntity<List<StoreItemDTO>> getAvailableItems() {
        return ResponseEntity.ok(storeService.getAvailableItems());
    }

    @PostMapping("/buy")
    public ResponseEntity<Void> buyItem(@RequestBody BuyRequestDTO request, Authentication authentication) {
        storeService.buyItem(authentication.getName(), request.getItemId());
        return ResponseEntity.ok().build();
    }
}
