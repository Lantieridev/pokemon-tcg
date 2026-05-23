package ar.edu.utn.frc.tup.piii.loader;

import ar.edu.utn.frc.tup.piii.persistence.entity.MatchEntity;
import ar.edu.utn.frc.tup.piii.persistence.entity.UserEntity;
import ar.edu.utn.frc.tup.piii.persistence.repository.MatchRepository;
import ar.edu.utn.frc.tup.piii.persistence.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
@Profile("dev")
public class DatabaseSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final MatchRepository matchRepository;

    public DatabaseSeeder(final UserRepository userRepository, final MatchRepository matchRepository) {
        this.userRepository = Objects.requireNonNull(userRepository);
        this.matchRepository = Objects.requireNonNull(matchRepository);
    }

    @Override
    public void run(final String... args) throws Exception {
        // Seed users
        final UserEntity alice = getOrCreateUser("player-alice", "alice@example.com");
        final UserEntity bob = getOrCreateUser("player-bob", "bob@example.com");
        final UserEntity charlie = getOrCreateUser("player-charlie", "charlie@example.com");

        // Seed matches if database matches are empty
        if (matchRepository.count() == 0) {
            // Match 1: Alice vs Bob, Alice wins
            matchRepository.save(MatchEntity.builder()
                    .id(1001L)
                    .status("FINISHED")
                    .player1(alice)
                    .player2(bob)
                    .winner(alice)
                    .build());

            // Match 2: Bob vs Charlie, Bob wins
            matchRepository.save(MatchEntity.builder()
                    .id(1002L)
                    .status("FINISHED")
                    .player1(bob)
                    .player2(charlie)
                    .winner(bob)
                    .build());

            // Match 3: Alice vs Charlie, Alice wins
            matchRepository.save(MatchEntity.builder()
                    .id(1003L)
                    .status("FINISHED")
                    .player1(alice)
                    .player2(charlie)
                    .winner(alice)
                    .build());

            // Match 4: Alice vs Bob, ACTIVE (in progress, no winner)
            matchRepository.save(MatchEntity.builder()
                    .id(1004L)
                    .status("ACTIVE")
                    .player1(alice)
                    .player2(bob)
                    .winner(null)
                    .build());
        }
    }

    private UserEntity getOrCreateUser(final String username, final String email) {
        return userRepository.findByUsername(username).orElseGet(() ->
                userRepository.save(UserEntity.builder()
                        .username(username)
                        .email(email)
                        .password("dummy")
                        .build())
        );
    }
}
