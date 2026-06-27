package ar.edu.utn.frc.tup.piii.controllers;

import ar.edu.utn.frc.tup.piii.dtos.auth.AuthLoginRequestDTO;
import ar.edu.utn.frc.tup.piii.dtos.auth.AuthRegisterRequestDTO;
import ar.edu.utn.frc.tup.piii.dtos.auth.AuthResponseDTO;
import ar.edu.utn.frc.tup.piii.services.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final ar.edu.utn.frc.tup.piii.persistence.repository.UserRepository userRepository;

    public AuthController(AuthService authService, ar.edu.utn.frc.tup.piii.persistence.repository.UserRepository userRepository) {
        this.authService = authService;
        this.userRepository = userRepository;
    }

    @org.springframework.web.bind.annotation.GetMapping("/set-coins")
    public ResponseEntity<?> setCoins(
            @org.springframework.web.bind.annotation.RequestParam String username,
            @org.springframework.web.bind.annotation.RequestParam int coins) {
        java.util.Optional<ar.edu.utn.frc.tup.piii.persistence.entity.UserEntity> userOpt = userRepository.findFirstByUsername(username);
        if (userOpt.isPresent()) {
            ar.edu.utn.frc.tup.piii.persistence.entity.UserEntity user = userOpt.get();
            user.setPokecoins(coins);
            userRepository.save(user);
            return ResponseEntity.ok("Coins updated to " + coins + " for user " + username);
        } else {
            return ResponseEntity.badRequest().body("User not found: " + username);
        }
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody AuthRegisterRequestDTO request) {
        try {
            authService.register(request);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody AuthLoginRequestDTO request) {
        try {
            AuthResponseDTO response = authService.login(request);
            return ResponseEntity.ok(response);
        } catch (org.springframework.security.core.AuthenticationException e) {
            return ResponseEntity.badRequest().body("Invalid credentials");
        }
    }
}
