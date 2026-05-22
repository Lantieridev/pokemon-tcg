package ar.edu.utn.frc.tup.piii.controllers;

import ar.edu.utn.frc.tup.piii.services.MuteService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.Objects;
import java.util.Set;

/**
 * Controller for user-specific actions like muting/unmuting oponents.
 */
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final MuteService muteService;

    public UserController(final MuteService muteService) {
        this.muteService = Objects.requireNonNull(muteService, "muteService must not be null");
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
}
