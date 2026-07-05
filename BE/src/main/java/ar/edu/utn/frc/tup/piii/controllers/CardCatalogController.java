package ar.edu.utn.frc.tup.piii.controllers;

import ar.edu.utn.frc.tup.piii.client.PokemonTcgApiClient;
import ar.edu.utn.frc.tup.piii.client.dto.PokemonTcgCardDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Objects;

/**
 * Exposes the Pokemon TCG card catalog through our own backend so the
 * frontend never has to call the external Pokemon TCG API directly.
 */
@RestController
@RequestMapping("/api/cards")
public final class CardCatalogController {

    private final PokemonTcgApiClient pokemonTcgApiClient;

    public CardCatalogController(final PokemonTcgApiClient pokemonTcgApiClient) {
        this.pokemonTcgApiClient = Objects.requireNonNull(pokemonTcgApiClient);
    }

    @GetMapping("/catalog")
    public ResponseEntity<List<PokemonTcgCardDTO>> getCatalog(@RequestParam final List<String> setIds) {
        return ResponseEntity.ok(pokemonTcgApiClient.findBySetIds(setIds));
    }
}
