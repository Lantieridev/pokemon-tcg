package ar.edu.utn.frc.tup.piii.services.impl;

import ar.edu.utn.frc.tup.piii.services.MuteService;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory thread-safe implementation of MuteService.
 */
@Service
public class MuteServiceImpl implements MuteService {

    // Maps a username to the set of usernames they have muted
    private final Map<String, Set<String>> mutedMap = new ConcurrentHashMap<>();

    @Override
    public void muteUser(final String username, final String targetUsername) {
        if (username == null || targetUsername == null) {
            return;
        }
        mutedMap.computeIfAbsent(username, k -> Collections.synchronizedSet(new HashSet<>())).add(targetUsername);
    }

    @Override
    public void unmuteUser(final String username, final String targetUsername) {
        if (username == null || targetUsername == null) {
            return;
        }
        final Set<String> muted = mutedMap.get(username);
        if (muted != null) {
            muted.remove(targetUsername);
        }
    }

    @Override
    public Set<String> getMutedUsers(final String username) {
        if (username == null) {
            return Collections.emptySet();
        }
        final Set<String> muted = mutedMap.get(username);
        return muted != null ? new HashSet<>(muted) : Collections.emptySet();
    }

    @Override
    public boolean isMuted(final String username, final String targetUsername) {
        if (username == null || targetUsername == null) {
            return false;
        }
        final Set<String> muted = mutedMap.get(username);
        return muted != null && muted.contains(targetUsername);
    }
}
