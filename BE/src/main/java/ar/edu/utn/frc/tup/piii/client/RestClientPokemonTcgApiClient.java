package ar.edu.utn.frc.tup.piii.client;

import ar.edu.utn.frc.tup.piii.client.dto.PokemonTcgCardDTO;
import ar.edu.utn.frc.tup.piii.client.dto.PokemonTcgSingleResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.util.Optional;

@Component
public final class RestClientPokemonTcgApiClient implements PokemonTcgApiClient {

    private final RestClient restClient;

    public RestClientPokemonTcgApiClient(
            final RestClient.Builder builder,
            @Value("${pokemontcg.api.base-url}") final String baseUrl) {
        this.restClient = builder.baseUrl(baseUrl).build();
    }

    @Override
    public Optional<PokemonTcgCardDTO> findById(final String cardId) {
        try {
            final PokemonTcgSingleResponse response = restClient.get()
                    .uri("/cards/{id}", cardId)
                    .retrieve()
                    .body(PokemonTcgSingleResponse.class);
            return Optional.ofNullable(response).map(PokemonTcgSingleResponse::data);
        } catch (HttpClientErrorException.NotFound e) {
            return Optional.empty();
        }
    }
}
