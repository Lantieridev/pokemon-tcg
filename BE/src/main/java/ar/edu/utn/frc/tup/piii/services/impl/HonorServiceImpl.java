package ar.edu.utn.frc.tup.piii.services.impl;

import ar.edu.utn.frc.tup.piii.dtos.HonorType;
import ar.edu.utn.frc.tup.piii.services.HonorService;
import org.springframework.stereotype.Service;

import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory thread-safe implementation of HonorService.
 */
@Service
public class HonorServiceImpl implements HonorService {

    // Maps a username to their received honors mapping (HonorType -> Count)
    private final Map<String, Map<HonorType, Integer>> honorMap = new ConcurrentHashMap<>();

    @Override
    public void awardHonor(final String username, final String targetUsername, final HonorType honorType) {
        if (username == null || targetUsername == null || honorType == null) {
            return;
        }
        if (username.equalsIgnoreCase(targetUsername)) {
            // Can't honor yourself
            return;
        }

        honorMap.compute(targetUsername, (k, currentHonors) -> {
            final Map<HonorType, Integer> honors = currentHonors != null ? currentHonors : new ConcurrentHashMap<>();
            honors.merge(honorType, 1, Integer::sum);
            return honors;
        });
    }

    @Override
    public Map<HonorType, Integer> getHonors(final String username) {
        if (username == null) {
            return new EnumMap<>(HonorType.class);
        }
        final Map<HonorType, Integer> userHonors = honorMap.get(username);
        final Map<HonorType, Integer> result = new EnumMap<>(HonorType.class);

        // Pre-fill with all HonorTypes set to 0 for a consistent structure
        for (final HonorType type : HonorType.values()) {
            result.put(type, 0);
        }

        if (userHonors != null) {
            result.putAll(userHonors);
        }
        return result;
    }
}
