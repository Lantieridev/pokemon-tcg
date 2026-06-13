package ar.edu.utn.frc.tup.piii.services;

import ar.edu.utn.frc.tup.piii.dtos.StoreItemDTO;

import java.util.List;

public interface StoreService {
    List<StoreItemDTO> getAvailableItems();
    void buyItem(String username, Long itemId);
}
