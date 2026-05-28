package ar.edu.utn.frc.tup.piii.services.impl;

import ar.edu.utn.frc.tup.piii.dtos.HonorType;
import ar.edu.utn.frc.tup.piii.persistence.entity.UserEntity;
import ar.edu.utn.frc.tup.piii.persistence.entity.UserHonorEntity;
import ar.edu.utn.frc.tup.piii.persistence.repository.UserHonorRepository;
import ar.edu.utn.frc.tup.piii.persistence.repository.UserRepository;
import ar.edu.utn.frc.tup.piii.services.HonorService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Database-backed implementation of HonorService.
 */
@Service
@Transactional
public class HonorServiceImpl implements HonorService {

    private final UserHonorRepository userHonorRepository;
    private final UserRepository userRepository;

    public HonorServiceImpl(final UserHonorRepository userHonorRepository, final UserRepository userRepository) {
        this.userHonorRepository = Objects.requireNonNull(userHonorRepository, "userHonorRepository must not be null");
        this.userRepository = Objects.requireNonNull(userRepository, "userRepository must not be null");
    }

    @Override
    public void awardHonor(final String username, final String targetUsername, final HonorType honorType) {
        if (username == null || targetUsername == null || honorType == null) {
            return;
        }
        if (username.equalsIgnoreCase(targetUsername)) {
            // Can't honor yourself
            return;
        }

        final Optional<UserEntity> giverOpt = userRepository.findByUsername(username);
        final Optional<UserEntity> receiverOpt = userRepository.findByUsername(targetUsername);

        if (giverOpt.isPresent() && receiverOpt.isPresent()) {
            userHonorRepository.save(UserHonorEntity.builder()
                    .giver(giverOpt.get())
                    .receiver(receiverOpt.get())
                    .honorType(honorType)
                    .build());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Map<HonorType, Integer> getHonors(final String username) {
        final Map<HonorType, Integer> result = new EnumMap<>(HonorType.class);

        // Pre-fill with all HonorTypes set to 0 for a consistent structure
        for (final HonorType type : HonorType.values()) {
            result.put(type, 0);
        }

        if (username == null) {
            return result;
        }

        final List<UserHonorEntity> received = userHonorRepository.findByReceiverUsername(username);
        for (final UserHonorEntity honor : received) {
            if (honor.getHonorType() != null) {
                result.merge(honor.getHonorType(), 1, Integer::sum);
            }
        }

        return result;
    }
}
