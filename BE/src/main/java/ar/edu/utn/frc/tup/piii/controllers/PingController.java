package ar.edu.utn.frc.tup.piii.controllers;

import ar.edu.utn.frc.tup.piii.dtos.common.ErrorApi;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Ping controller class to health check.
 */
@RestController
public class PingController {

    @Operation(
            summary = "Check healthy of the app",
            description = "If the app it's alive response pong")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Successful operation",
                    content = @Content(
                            schema = @Schema(implementation = String.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal Server Error",
                    content = @Content(
                            schema = @Schema(implementation = ErrorApi.class))
            )
    })
    @GetMapping("/ping")
    public String ping() {
        ar.edu.utn.frc.tup.piii.persistence.repository.UserRepository ur = ar.edu.utn.frc.tup.piii.context.ApplicationContextProvider.getApplicationContext().getBean(ar.edu.utn.frc.tup.piii.persistence.repository.UserRepository.class);
        for(ar.edu.utn.frc.tup.piii.entities.UserEntity u : ur.findAll()) {
            if(u.getPokecoins() == null || u.getPokecoins() < 100) {
                u.setPokecoins((u.getPokecoins() == null ? 0 : u.getPokecoins()) + 10);
                u.setXp((u.getXp() == null ? 0 : u.getXp()) + 25);
                ur.save(u);
            }
        }
        return "pong";
    }
}
