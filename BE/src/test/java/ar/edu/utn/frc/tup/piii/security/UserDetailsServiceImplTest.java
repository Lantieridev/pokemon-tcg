package ar.edu.utn.frc.tup.piii.security;

import ar.edu.utn.frc.tup.piii.persistence.entity.UserEntity;
import ar.edu.utn.frc.tup.piii.persistence.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class UserDetailsServiceImplTest {

    @Test
    void shouldLoadUserSuccessfully() {
        UserRepository userRepository = mock(UserRepository.class);
        UserEntity fakeEntity = UserEntity.builder()
                .username("lucas")
                .password("encoded_pass")
                .build();
        when(userRepository.findFirstByUsername("lucas")).thenReturn(Optional.of(fakeEntity));

        UserDetailsServiceImpl service = new UserDetailsServiceImpl(userRepository);
        UserDetails details = service.loadUserByUsername("lucas");

        assertThat(details).isNotNull();
        assertThat(details.getUsername()).isEqualTo("lucas");
        assertThat(details.getPassword()).isEqualTo("encoded_pass");
    }

    @Test
    void shouldThrowWhenUserNotFound() {
        UserRepository userRepository = mock(UserRepository.class);
        when(userRepository.findFirstByUsername("unknown")).thenReturn(Optional.empty());

        UserDetailsServiceImpl service = new UserDetailsServiceImpl(userRepository);
        assertThatThrownBy(() -> service.loadUserByUsername("unknown"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("User not found");
    }
}
