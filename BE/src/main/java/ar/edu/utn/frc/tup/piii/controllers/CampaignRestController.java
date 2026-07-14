package ar.edu.utn.frc.tup.piii.controllers;

import ar.edu.utn.frc.tup.piii.dtos.CampaignProgressResponseDTO;
import ar.edu.utn.frc.tup.piii.services.CampaignService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;
import java.util.Map;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/api/campaign")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class CampaignRestController {

    private final CampaignService campaignService;

    /**
     * GET /api/campaign/progress
     * Obtiene el progreso del usuario logueado en la campaña PvE.
     */
    @GetMapping("/progress")
    public ResponseEntity<CampaignProgressResponseDTO> getProgress(Principal principal) {
        if (principal == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuario no autenticado");
        }
        return ResponseEntity.ok(campaignService.getCampaignProgress(principal.getName()));
    }

    /**
     * POST /api/campaign/challenge/{nodeId}
     * Inicia un desafío de campaña contra el bot del gimnasio especificado.
     */
    @PostMapping("/challenge/{nodeId}")
    public ResponseEntity<Map<String, String>> challengeNode(
            @PathVariable final int nodeId,
            @RequestParam final Long deckId,
            Principal principal) {
        
        if (principal == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuario no autenticado");
        }

        try {
            String matchId = campaignService.iniciarDesafioPvE(principal.getName(), nodeId, deckId);
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("matchId", matchId));
        } catch (NoSuchElementException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage(), e);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage(), e);
        }
    }
}
