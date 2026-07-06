package ar.edu.utn.frc.tup.piii.persistence.entity;

import ar.edu.utn.frc.tup.piii.persistence.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Proves the @Version field on UserEntity actually rejects a stale concurrent write,
 * reproducing the double-spend race two simultaneous purchase requests could otherwise cause:
 * both read the same balance, both pass their "can afford this" check, and without locking
 * both writes would succeed even though only one purchase should have gone through.
 */
@SpringBootTest
class UserEntityOptimisticLockingTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    void shouldRejectStaleWriteWhenTwoConcurrentRequestsLoadTheSameUser() {
        final UserEntity saved = userRepository.save(UserEntity.builder()
                .username("optimistic-lock-test-user")
                .password("irrelevant")
                .email("optimistic-lock@test.com")
                .pokecoins(1000)
                .build());

        // Simulate two concurrent requests both reading the balance before either writes.
        final UserEntity requestA = userRepository.findById(saved.getId()).orElseThrow();
        final UserEntity requestB = userRepository.findById(saved.getId()).orElseThrow();

        requestA.setPokecoins(requestA.getPokecoins() - 900);
        userRepository.saveAndFlush(requestA);

        requestB.setPokecoins(requestB.getPokecoins() - 900);
        assertThrows(ObjectOptimisticLockingFailureException.class,
                () -> userRepository.saveAndFlush(requestB));

        assertEquals(100, userRepository.findById(saved.getId()).orElseThrow().getPokecoins());
    }
}
