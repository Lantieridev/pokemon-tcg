package ar.edu.utn.frc.tup.piii.controllers;

import ar.edu.utn.frc.tup.piii.dtos.HonorRequest;
import ar.edu.utn.frc.tup.piii.dtos.HonorType;
import ar.edu.utn.frc.tup.piii.dtos.UserStatusResponse;
import ar.edu.utn.frc.tup.piii.dtos.UserProfileResponseDTO;
import ar.edu.utn.frc.tup.piii.dtos.UpdateProfileRequestDTO;
import ar.edu.utn.frc.tup.piii.dtos.UpdateShowcaseRequestDTO;
import ar.edu.utn.frc.tup.piii.dtos.UpdateShowcaseDeckRequestDTO;
import ar.edu.utn.frc.tup.piii.services.HonorService;
import ar.edu.utn.frc.tup.piii.services.MuteService;
import ar.edu.utn.frc.tup.piii.services.PenaltyService;
import ar.edu.utn.frc.tup.piii.services.ProfileService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.time.LocalDateTime;
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
    private final ProfileService profileService;

    public UserController(final MuteService muteService,
                          final HonorService honorService,
                          final PenaltyService penaltyService,
                          final ProfileService profileService) {
        this.muteService = Objects.requireNonNull(muteService, "muteService must not be null");
        this.honorService = Objects.requireNonNull(honorService, "honorService must not be null");
        this.penaltyService = Objects.requireNonNull(penaltyService, "penaltyService must not be null");
        this.profileService = Objects.requireNonNull(profileService, "profileService must not be null");
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
        final boolean isPerma = penaltyService.isPermanentlyBanned(username);
        final boolean isPenalized = penaltyService.isPenalized(username);
        final String status = isPerma ? "PERMA_BANNED" : (isPenalized ? "PENALIZED" : "ACTIVE");
        final String penaltyType = penaltyService.getPenaltyType(username);
        final Integer matchesRemaining = penaltyService.getMatchesPenalizedRemaining(username);
        final LocalDateTime expiration = penaltyService.getPenaltyExpiration(username);

        final java.util.List<String> notifications = new java.util.ArrayList<>(penaltyService.getPendingNotifications(username));
        penaltyService.clearPendingNotifications(username);

        final boolean showWarning = penaltyService.shouldShowRecidivismWarning(username);
        if (showWarning) {
            penaltyService.clearRecidivismWarning(username);
        }

        return ResponseEntity.ok(UserStatusResponse.builder()
                .status(status)
                .penaltyType(penaltyType)
                .matchesPenalizedRemaining(matchesRemaining)
                .penaltyExpiration(expiration)
                .pendingNotifications(notifications)
                .showRecidivismWarning(showWarning)
                .build());
    }

    /**
     * Retrieves the consolidated user profile.
     *
     * @param username the username to fetch
     * @return the profile DTO
     */
    @GetMapping("/{username}/profile")
    public ResponseEntity<UserProfileResponseDTO> getUserProfile(@PathVariable final String username) {
        try {
            return ResponseEntity.ok(profileService.getProfile(username));
        } catch (final IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Updates the authenticated user's profile customization.
     *
     * @param request   the update details
     * @param principal the authenticated principal
     * @return 200 OK or 400 Bad Request
     */
    @PutMapping("/profile")
    public ResponseEntity<Void> updateProfile(@RequestBody final UpdateProfileRequestDTO request, final Principal principal) {
        if (principal == null || request == null) {
            return ResponseEntity.badRequest().build();
        }
        profileService.updateProfile(principal.getName(), request);
        return ResponseEntity.ok().build();
    }

    /**
     * Updates the authenticated user's showcase slots.
     *
     * @param request   the slots update details
     * @param principal the authenticated principal
     * @return 200 OK or 400 Bad Request
     */
    @PutMapping("/profile/showcase")
    public ResponseEntity<Void> updateShowcase(@RequestBody final UpdateShowcaseRequestDTO request, final Principal principal) {
        if (principal == null || request == null) {
            return ResponseEntity.badRequest().build();
        }
        profileService.updateShowcase(principal.getName(), request);
        return ResponseEntity.ok().build();
    }

    /**
     * Updates the authenticated user's showcased deck.
     *
     * @param request   the showcased deck update details
     * @param principal the authenticated principal
     * @return 200 OK or 400 Bad Request
     */
    @PutMapping("/profile/showcase/deck")
    public ResponseEntity<Void> updateShowcaseDeck(@RequestBody final UpdateShowcaseDeckRequestDTO request, final Principal principal) {
        if (principal == null || request == null) {
            return ResponseEntity.badRequest().build();
        }
        profileService.updateShowcaseDeck(principal.getName(), request.getDeckId());
        return ResponseEntity.ok().build();
    }
}
