package ar.edu.utn.frc.tup.piii.services.impl;

import ar.edu.utn.frc.tup.piii.persistence.entity.UserEntity;
import ar.edu.utn.frc.tup.piii.persistence.entity.UserMuteEntity;
import ar.edu.utn.frc.tup.piii.persistence.repository.UserMuteRepository;
import ar.edu.utn.frc.tup.piii.persistence.repository.UserRepository;
import ar.edu.utn.frc.tup.piii.services.MuteService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Database-backed implementation of MuteService.
 */
@Service
@Transactional
public class MuteServiceImpl implements MuteService {

    private final UserMuteRepository userMuteRepository;
    private final UserRepository userRepository;

    public MuteServiceImpl(final UserMuteRepository userMuteRepository, final UserRepository userRepository) {
        this.userMuteRepository = Objects.requireNonNull(userMuteRepository, "userMuteRepository must not be null");
        this.userRepository = Objects.requireNonNull(userRepository, "userRepository must not be null");
    }

    @Override
    public void muteUser(final String username, final String targetUsername) {
        if (username == null || targetUsername == null || username.equals(targetUsername)) {
            return;
        }
        final Optional<UserEntity> userOpt = userRepository.findFirstByUsername(username);
        final Optional<UserEntity> targetOpt = userRepository.findFirstByUsername(targetUsername);

        if (userOpt.isPresent() && targetOpt.isPresent()) {
            final UserEntity user = userOpt.get();
            final UserEntity target = targetOpt.get();

            final boolean alreadyMuted = userMuteRepository.findByUserAndMutedUser(user, target).isPresent();
            if (!alreadyMuted) {
                userMuteRepository.save(UserMuteEntity.builder()
                        .user(user)
                        .mutedUser(target)
                        .build());
            }
        }
    }

    @Override
    public void unmuteUser(final String username, final String targetUsername) {
        if (username == null || targetUsername == null) {
            return;
        }
        userMuteRepository.findByUserUsernameAndMutedUserUsername(username, targetUsername)
                .ifPresent(userMuteRepository::delete);
    }

    @Override
    @Transactional(readOnly = true)
    public Set<String> getMutedUsers(final String username) {
        if (username == null) {
            return Collections.emptySet();
        }
        return userMuteRepository.findByUserUsername(username).stream()
                .map(mute -> mute.getMutedUser().getUsername())
                .collect(Collectors.toSet());
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isMuted(final String username, final String targetUsername) {
        if (username == null || targetUsername == null) {
            return false;
        }
        return userMuteRepository.findByUserUsernameAndMutedUserUsername(username, targetUsername).isPresent();
    }
}
