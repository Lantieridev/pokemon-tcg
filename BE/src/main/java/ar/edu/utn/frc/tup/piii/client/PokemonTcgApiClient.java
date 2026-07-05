package ar.edu.utn.frc.tup.piii.client;

import ar.edu.utn.frc.tup.piii.client.dto.PokemonTcgCardDTO;

import java.util.List;
import java.util.Optional;

public interface PokemonTcgApiClient {

    Optional<PokemonTcgCardDTO> findById(String cardId);

    List<PokemonTcgCardDTO> findBySetIds(List<String> setIds);
}
