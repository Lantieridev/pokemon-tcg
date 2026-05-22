package ar.edu.utn.frc.tup.piii.controllers;

import ar.edu.utn.frc.tup.piii.dtos.HonorRequest;
import ar.edu.utn.frc.tup.piii.dtos.HonorType;
import ar.edu.utn.frc.tup.piii.dtos.UserStatusResponse;
import ar.edu.utn.frc.tup.piii.services.HonorService;
import ar.edu.utn.frc.tup.piii.services.MuteService;
import ar.edu.utn.frc.tup.piii.services.PenaltyService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Controller for user-specific actions like muting/unmuting oponents and awarding honors.
 */
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final MuteService muteService;
    private final HonorService honorService;
    private final PenaltyService penaltyService;

    public UserController(final MuteService muteService, final HonorService honorService, final PenaltyService penaltyService) {
        this.muteService = Objects.requireNonNull(muteService, "muteService must not be null");
        this.honorService = Objects.requireNonNull(honorService, "honorService must not be null");
        this.penaltyService = Objects.requireNonNull(penaltyService, "penaltyService must not be null");
    }

    /**
     * Mutes a target user for the authenticated user.
     *
     * @param targetUsername the username to mute
     * @param principal      the authenticated principal
     * @return 200 OK
     */
    @PostMapping("/mute/{targetUsername}")
    public ResponseEntity<Void> muteUser(@PathVariable final String targetUsername, final Principal principal) {
        if (principal != null) {
            muteService.muteUser(principal.getName(), targetUsername);
        }
        return ResponseEntity.ok().build();
    }

    /**
     * Unmutes a target user for the authenticated user.
     *
     * @param targetUsername the username to unmute
     * @param principal      the authenticated principal
     * @return 200 OK
     */
    @DeleteMapping("/mute/{targetUsername}")
    public ResponseEntity<Void> unmuteUser(@PathVariable final String targetUsername, final Principal principal) {
        if (principal != null) {
            muteService.unmuteUser(principal.getName(), targetUsername);
        }
        return ResponseEntity.ok().build();
    }

    /**
     * Retrieves the set of muted users for the authenticated user.
     *
     * @param principal the authenticated principal
     * @return list of muted usernames
     */
    @GetMapping("/mute")
    public ResponseEntity<Set<String>> getMutedUsers(final Principal principal) {
        if (principal == null) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(muteService.getMutedUsers(principal.getName()));
    }

    /**
     * Awards honor to a player.
     *
     * @param username     the player receiving the honor
     * @param request      the honor details
     * @param principal    the authenticated player giving the honor
     * @return 200 OK
     */
    @PostMapping("/{username}/honor")
    public ResponseEntity<Void> awardHonor(@PathVariable final String username,
                                           @RequestBody final HonorRequest request,
                                           final Principal principal) {
        if (principal != null && request != null && request.getHonorType() != null) {
            honorService.awardHonor(principal.getName(), username, request.getHonorType());
        }
        return ResponseEntity.ok().build();
    }

    /**
     * Gets the count of honors for a user.
     *
     * @param username the user to inspect
     * @return the honor tally map
     */
    @GetMapping("/{username}/honor")
    public ResponseEntity<Map<HonorType, Integer>> getHonors(@PathVariable final String username) {
        return ResponseEntity.ok(honorService.getHonors(username));
    }

    /**
     * Gets the moderation and penalty status of a user.
     *
     * @param username the user to inspect
     * @return the user status details
     */
    @GetMapping("/{username}/status")
    public ResponseEntity<UserStatusResponse> getUserStatus(@PathVariable final String username) {
        if (penaltyService.isPenalized(username)) {
            return ResponseEntity.ok(UserStatusResponse.builder()
                    .status("PENALIZED")
                    .penaltyExpiration(penaltyService.getPenaltyExpiration(username))
                    .build());
        }
        return ResponseEntity.ok(UserStatusResponse.builder()
                .status("ACTIVE")
                .build());
    }
}
