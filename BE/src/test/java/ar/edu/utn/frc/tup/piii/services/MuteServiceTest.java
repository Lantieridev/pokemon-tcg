package ar.edu.utn.frc.tup.piii.services;

import ar.edu.utn.frc.tup.piii.services.impl.MuteServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class MuteServiceTest {

    private MuteService muteService;

    @BeforeEach
    void setUp() {
        muteService = new MuteServiceImpl();
    }

    @Test
    void shouldMuteAndVerifyUser() {
        final String username = "ash";
        final String target = "gary";

        assertFalse(muteService.isMuted(username, target));

        muteService.muteUser(username, target);
        assertTrue(muteService.isMuted(username, target));

        final Set<String> muted = muteService.getMutedUsers(username);
        assertEquals(1, muted.size());
        assertTrue(muted.contains(target));
    }

    @Test
    void shouldUnmuteUser() {
        final String username = "ash";
        final String target = "gary";

        muteService.muteUser(username, target);
        assertTrue(muteService.isMuted(username, target));

        muteService.unmuteUser(username, target);
        assertFalse(muteService.isMuted(username, target));
        assertTrue(muteService.getMutedUsers(username).isEmpty());
    }

    @Test
    void shouldHandleNullInputsGracefully() {
        muteService.muteUser(null, "gary");
        muteService.muteUser("ash", null);
        assertFalse(muteService.isMuted("ash", "gary"));
        assertTrue(muteService.getMutedUsers(null).isEmpty());
    }
}
