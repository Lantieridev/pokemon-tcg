package ar.edu.utn.frc.tup.piii.controllers;

import ar.edu.utn.frc.tup.piii.dtos.friends.FriendshipDTO;
import ar.edu.utn.frc.tup.piii.dtos.friends.FriendshipRequestDTO;
import ar.edu.utn.frc.tup.piii.services.FriendshipService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/friends")
@RequiredArgsConstructor
public class FriendshipController {

    private final FriendshipService friendshipService;

    @PostMapping("/request")
    public ResponseEntity<?> sendFriendRequest(Principal principal, @RequestBody FriendshipRequestDTO request) {
        try {
            friendshipService.sendFriendRequest(principal.getName(), request.getTargetUsername());
            return ResponseEntity.ok(Map.of("message", "Friend request sent successfully"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("/list")
    public ResponseEntity<List<FriendshipDTO>> getActiveFriends(Principal principal) {
        return ResponseEntity.ok(friendshipService.getActiveFriends(principal.getName()));
    }

    @GetMapping("/requests")
    public ResponseEntity<List<FriendshipDTO>> getPendingRequests(Principal principal) {
        return ResponseEntity.ok(friendshipService.getPendingRequests(principal.getName()));
    }

    @PutMapping("/accept/{id}")
    public ResponseEntity<?> acceptFriendRequest(Principal principal, @PathVariable Long id) {
        try {
            friendshipService.acceptFriendRequest(principal.getName(), id);
            return ResponseEntity.ok(Map.of("message", "Friend request accepted"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PutMapping("/reject/{id}")
    public ResponseEntity<?> rejectFriendRequest(Principal principal, @PathVariable Long id) {
        try {
            friendshipService.rejectFriendRequest(principal.getName(), id);
            return ResponseEntity.ok(Map.of("message", "Friend request rejected"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @DeleteMapping("/remove/{id}")
    public ResponseEntity<?> removeFriend(Principal principal, @PathVariable Long id) {
        try {
            friendshipService.removeFriend(principal.getName(), id);
            return ResponseEntity.ok(Map.of("message", "Friend removed successfully"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }
}
