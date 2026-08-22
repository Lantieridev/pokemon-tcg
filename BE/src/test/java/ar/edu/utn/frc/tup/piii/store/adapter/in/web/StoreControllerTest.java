package ar.edu.utn.frc.tup.piii.store.adapter.in.web;

import ar.edu.utn.frc.tup.piii.controllers.GlobalExceptionHandler;
import ar.edu.utn.frc.tup.piii.store.application.port.in.ListAvailableStoreItemsUseCase;
import ar.edu.utn.frc.tup.piii.store.application.port.in.PurchaseStoreItemUseCase;
import ar.edu.utn.frc.tup.piii.store.domain.StoreItem;
import ar.edu.utn.frc.tup.piii.store.domain.StoreItemType;
import ar.edu.utn.frc.tup.piii.store.domain.exception.InsufficientPokecoinsException;
import ar.edu.utn.frc.tup.piii.store.domain.exception.StoreUserNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class StoreControllerTest {

    private static final Authentication LUCAS = new UsernamePasswordAuthenticationToken("lucas", null);

    private MockMvc mockMvc;
    private ListAvailableStoreItemsUseCase listAvailableStoreItemsUseCase;
    private PurchaseStoreItemUseCase purchaseStoreItemUseCase;

    @BeforeEach
    void setUp() {
        listAvailableStoreItemsUseCase = mock(ListAvailableStoreItemsUseCase.class);
        purchaseStoreItemUseCase = mock(PurchaseStoreItemUseCase.class);

        mockMvc = MockMvcBuilders
                .standaloneSetup(new StoreController(listAvailableStoreItemsUseCase, purchaseStoreItemUseCase))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void getAvailableItemsReturnsTheCatalogAsJson() throws Exception {
        final StoreItem item = new StoreItem(1L, "Avatar Pikachu", "desc", 100,
                StoreItemType.AVATAR, "pikachu.png", true);
        when(listAvailableStoreItemsUseCase.listAvailableItems()).thenReturn(List.of(item));

        mockMvc.perform(get("/api/store/items"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].name").value("Avatar Pikachu"))
                .andExpect(jsonPath("$[0].itemType").value("AVATAR"))
                .andExpect(jsonPath("$[0].imageUrl").value("pikachu.png"));
    }

    @Test
    void buyItemDelegatesToTheUseCaseWithTheAuthenticatedUsername() throws Exception {
        mockMvc.perform(post("/api/store/buy")
                        .principal(LUCAS)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"itemId\":7}"))
                .andExpect(status().isOk());

        verify(purchaseStoreItemUseCase).purchase(eq("lucas"), eq(7L));
    }

    @Test
    void buyItemMapsInsufficientFundsToBadRequest() throws Exception {
        doThrow(new InsufficientPokecoinsException())
                .when(purchaseStoreItemUseCase).purchase("lucas", 7L);

        mockMvc.perform(post("/api/store/buy")
                        .principal(LUCAS)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"itemId\":7}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void buyItemMapsUnresolvedUserToUnauthorized() throws Exception {
        doThrow(new StoreUserNotFoundException())
                .when(purchaseStoreItemUseCase).purchase("lucas", 7L);

        mockMvc.perform(post("/api/store/buy")
                        .principal(LUCAS)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"itemId\":7}"))
                .andExpect(status().isUnauthorized());
    }
}
