package ar.edu.utn.frc.tup.piii.services;

import ar.edu.utn.frc.tup.piii.persistence.entity.UserEntity;
import ar.edu.utn.frc.tup.piii.persistence.entity.UserMuteEntity;
import ar.edu.utn.frc.tup.piii.persistence.repository.UserMuteRepository;
import ar.edu.utn.frc.tup.piii.persistence.repository.UserRepository;
import ar.edu.utn.frc.tup.piii.services.impl.MuteServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MuteServiceTest {

    private UserMuteRepository userMuteRepository;
    private UserRepository userRepository;
    private MuteService muteService;

    @BeforeEach
    void setUp() {
        userMuteRepository = mock(UserMuteRepository.class);
        userRepository = mock(UserRepository.class);
        muteService = new MuteServiceImpl(userMuteRepository, userRepository);
    }

    @Test
    void shouldMuteAndVerifyUser() {
        final String username = "ash";
        final String target = "gary";

        final UserEntity user = UserEntity.builder().id(1L).username(username).build();
        final UserEntity targetUser = UserEntity.builder().id(2L).username(target).build();

        when(userRepository.findByUsername(username)).thenReturn(Optional.of(user));
        when(userRepository.findByUsername(target)).thenReturn(Optional.of(targetUser));
        when(userMuteRepository.findByUserAndMutedUser(user, targetUser)).thenReturn(Optional.empty());

        // Al consultar silenciado, inicialmente no
        when(userMuteRepository.findByUserUsernameAndMutedUserUsername(username, target)).thenReturn(Optional.empty());
        assertFalse(muteService.isMuted(username, target));

        // Muteamos
        muteService.muteUser(username, target);
        verify(userMuteRepository, times(1)).save(any(UserMuteEntity.class));

        // Ahora simulamos que está mutado
        final UserMuteEntity mute = UserMuteEntity.builder().user(user).mutedUser(targetUser).build();
        when(userMuteRepository.findByUserUsernameAndMutedUserUsername(username, target)).thenReturn(Optional.of(mute));
        assertTrue(muteService.isMuted(username, target));

        final List<UserMuteEntity> muteList = List.of(mute);
        when(userMuteRepository.findByUserUsername(username)).thenReturn(muteList);

        final Set<String> muted = muteService.getMutedUsers(username);
        assertEquals(1, muted.size());
        assertTrue(muted.contains(target));
    }

    @Test
    void shouldUnmuteUser() {
        final String username = "ash";
        final String target = "gary";

        final UserMuteEntity mute = UserMuteEntity.builder().build();
        when(userMuteRepository.findByUserUsernameAndMutedUserUsername(username, target)).thenReturn(Optional.of(mute));

        muteService.unmuteUser(username, target);
        verify(userMuteRepository, times(1)).delete(mute);
    }

    @Test
    void shouldHandleNullInputsGracefully() {
        muteService.muteUser(null, "gary");
        muteService.muteUser("ash", null);

        verify(userRepository, never()).findByUsername(anyString());
    }
}
