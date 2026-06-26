package ar.edu.utn.frc.tup.piii.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

class JwtUtilTest {

    private JwtUtil jwtUtil;
    private final String secret = "MySuperSecretKeyForPokemonTcgGame1234567890!";
    private final long expiration = 36000000;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil(secret, expiration);
    }

    @Test
    void shouldGenerateAndExtractToken() {
        String username = "lucas";
        String token = jwtUtil.generateToken(username);

        assertThat(token).isNotNull();
        assertThat(jwtUtil.getUsernameFromToken(token)).isEqualTo(username);
        assertThat(jwtUtil.isValidToken(token)).isTrue();
    }

    @Test
    void shouldValidateToken() {
        String username = "lucas";
        String token = jwtUtil.generateToken(username);
        UserDetails userDetails = new User(username, "password", Collections.emptyList());

        assertThat(jwtUtil.validateToken(token, userDetails)).isTrue();
    }

    @Test
    void shouldFailValidationForWrongUsername() {
        String username = "lucas";
        String token = jwtUtil.generateToken(username);
        UserDetails userDetails = new User("wrong_user", "password", Collections.emptyList());

        assertThat(jwtUtil.validateToken(token, userDetails)).isFalse();
    }

    @Test
    void shouldBeInvalidForBadToken() {
        assertThat(jwtUtil.isValidToken("bad-token")).isFalse();
    }
}
