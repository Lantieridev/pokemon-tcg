package ar.edu.utn.frc.tup.piii.client;

import ar.edu.utn.frc.tup.piii.client.dto.PokemonTcgCardDTO;
import ar.edu.utn.frc.tup.piii.client.dto.PokemonTcgListResponse;
import ar.edu.utn.frc.tup.piii.client.dto.PokemonTcgSingleResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

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

    @Override
    public List<PokemonTcgCardDTO> findBySetIds(final List<String> setIds) {
        if (setIds == null || setIds.isEmpty()) {
            return Collections.emptyList();
        }
        final String query = setIds.stream()
                .map(setId -> "set.id:" + setId)
                .collect(Collectors.joining(" OR ", "(", ")"));

        final PokemonTcgListResponse response = restClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/cards")
                        .queryParam("q", query)
                        .queryParam("pageSize", 350)
                        .build())
                .retrieve()
                .body(PokemonTcgListResponse.class);

        return Optional.ofNullable(response)
                .map(PokemonTcgListResponse::data)
                .orElse(Collections.emptyList());
    }
}
