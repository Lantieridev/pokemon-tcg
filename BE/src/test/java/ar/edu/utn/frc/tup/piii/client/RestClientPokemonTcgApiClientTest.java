package ar.edu.utn.frc.tup.piii.client;

import ar.edu.utn.frc.tup.piii.client.dto.PokemonTcgCardDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withResourceNotFound;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class RestClientPokemonTcgApiClientTest {

    private static final String BASE_URL = "https://api.pokemontcg.io/v2";

    private MockRestServiceServer server;
    private RestClientPokemonTcgApiClient client;

    @BeforeEach
    void setUp() {
        final RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        client = new RestClientPokemonTcgApiClient(builder, BASE_URL);
    }

    @Test
    void findById_returnsCardWhenApiResponds200() {
        server.expect(requestTo(BASE_URL + "/cards/xy1-1"))
                .andRespond(withSuccess("""
                        {
                          "data": {
                            "id": "xy1-1",
                            "name": "Venusaur-EX",
                            "supertype": "Pokémon",
                            "subtypes": ["Basic", "EX"],
                            "hp": "180",
                            "set": { "id": "xy1", "name": "XY" }
                          }
                        }
                        """, MediaType.APPLICATION_JSON));

        final Optional<PokemonTcgCardDTO> result = client.findById("xy1-1");

        assertTrue(result.isPresent());
        assertEquals("xy1-1", result.get().id());
        assertEquals("Venusaur-EX", result.get().name());
        assertEquals("Pokémon", result.get().supertype());
    }

    @Test
    void findById_returnsEmptyWhenApiResponds404() {
        server.expect(requestTo(BASE_URL + "/cards/nonexistent"))
                .andRespond(withResourceNotFound());

        final Optional<PokemonTcgCardDTO> result = client.findById("nonexistent");

        assertFalse(result.isPresent());
    }

    @Test
    void findById_returnsParsedSubtypes() {
        server.expect(requestTo(BASE_URL + "/cards/xy1-2"))
                .andRespond(withSuccess("""
                        {
                          "data": {
                            "id": "xy1-2",
                            "name": "Bulbasaur",
                            "supertype": "Pokémon",
                            "subtypes": ["Basic"],
                            "hp": "60",
                            "set": { "id": "xy1", "name": "XY" }
                          }
                        }
                        """, MediaType.APPLICATION_JSON));

        final Optional<PokemonTcgCardDTO> result = client.findById("xy1-2");

        assertTrue(result.isPresent());
        assertEquals(1, result.get().subtypes().size());
        assertEquals("Basic", result.get().subtypes().get(0));
    }
}
