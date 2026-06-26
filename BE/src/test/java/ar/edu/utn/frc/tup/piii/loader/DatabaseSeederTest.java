package ar.edu.utn.frc.tup.piii.loader;

import ar.edu.utn.frc.tup.piii.persistence.entity.UserEntity;
import ar.edu.utn.frc.tup.piii.persistence.repository.MatchRepository;
import ar.edu.utn.frc.tup.piii.persistence.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DatabaseSeederTest {

    @Test
    void shouldSeedDataWhenEmpty() throws Exception {
        UserRepository userRepository = mock(UserRepository.class);
        MatchRepository matchRepository = mock(MatchRepository.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);

        when(userRepository.findByUsername(any())).thenReturn(Optional.empty());
        when(userRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(matchRepository.count()).thenReturn(0L);
        when(passwordEncoder.encode(any())).thenReturn("encoded");

        DatabaseSeeder seeder = new DatabaseSeeder(userRepository, matchRepository, passwordEncoder);
        seeder.run();

        Mockito.verify(matchRepository, Mockito.times(4)).save(any());
    }

    @Test
    void shouldNotSeedMatchesWhenNotEmpty() throws Exception {
        UserRepository userRepository = mock(UserRepository.class);
        MatchRepository matchRepository = mock(MatchRepository.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);

        UserEntity fakeUser = UserEntity.builder().username("fake").build();
        when(userRepository.findByUsername(any())).thenReturn(Optional.of(fakeUser));
        when(matchRepository.count()).thenReturn(2L);

        DatabaseSeeder seeder = new DatabaseSeeder(userRepository, matchRepository, passwordEncoder);
        seeder.run();

        Mockito.verify(matchRepository, Mockito.never()).save(any());
    }
}
