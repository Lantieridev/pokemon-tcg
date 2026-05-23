package ar.edu.utn.frc.tup.piii.services.persistence;

import ar.edu.utn.frc.tup.piii.engine.session.MatchSession;
import ar.edu.utn.frc.tup.piii.persistence.entity.MatchEntity;
import ar.edu.utn.frc.tup.piii.persistence.entity.MatchLogEntity;
import ar.edu.utn.frc.tup.piii.persistence.entity.UserEntity;
import ar.edu.utn.frc.tup.piii.persistence.repository.MatchLogRepository;
import ar.edu.utn.frc.tup.piii.persistence.repository.MatchRepository;
import ar.edu.utn.frc.tup.piii.persistence.repository.UserRepository;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Component
public class AsyncPersistenceListener {

    private final MatchRepository matchRepository;
    private final MatchLogRepository matchLogRepository;
    private final UserRepository userRepository;

    public AsyncPersistenceListener(final MatchRepository matchRepository,
                                    final MatchLogRepository matchLogRepository,
                                    final UserRepository userRepository) {
        this.matchRepository = Objects.requireNonNull(matchRepository);
        this.matchLogRepository = Objects.requireNonNull(matchLogRepository);
        this.userRepository = Objects.requireNonNull(userRepository);
    }

    @Async
    @EventListener
    @Transactional
    public void onSaveMatch(final SaveMatchEvent event) {
        final MatchSession session = event.session();
        final Long matchIdNumeric = parseOrHashId(session.getMatchId());

        final UserEntity player1 = getOrCreateUser(session.getPlayerIdA());
        final UserEntity player2 = getOrCreateUser(session.getPlayerIdB());

        final MatchEntity entity = matchRepository.findById(matchIdNumeric)
                .orElseGet(() -> MatchEntity.builder().id(matchIdNumeric).build());

        entity.setStatus(session.getState().name());
        entity.setPlayer1(player1);
        entity.setPlayer2(player2);
        entity.setCurrentState(session);

        matchRepository.save(entity);
    }

    @Async
    @EventListener
    @Transactional
    public void onLogAction(final LogActionEvent event) {
        final Long matchIdNumeric = parseOrHashId(event.matchId());
        final UserEntity player = getOrCreateUser(event.playerId());

        final MatchEntity match = matchRepository.findById(matchIdNumeric)
                .orElseGet(() -> {
                    final MatchEntity dummy = MatchEntity.builder()
                            .id(matchIdNumeric)
                            .status("ACTIVE")
                            .player1(player != null ? player : getOrCreateUser("system"))
                            .build();
                    return matchRepository.save(dummy);
                });

        final MatchLogEntity logEntity = MatchLogEntity.builder()
                .match(match)
                .turnNumber(event.turnNumber())
                .player(player)
                .actionType(event.actionType())
                .result(event.result())
                .build();

        matchLogRepository.save(logEntity);
    }

    private Long parseOrHashId(final String id) {
        if (id == null) return null;
        try {
            return Long.parseLong(id);
        } catch (NumberFormatException e) {
            return (long) Math.abs(id.hashCode());
        }
    }

    private UserEntity getOrCreateUser(final String username) {
        if (username == null) return null;
        return userRepository.findByUsername(username).orElseGet(() -> {
            final UserEntity newUser = UserEntity.builder()
                    .username(username)
                    .email(username + "@example.com")
                    .password("dummy")
                    .build();
            return userRepository.save(newUser);
        });
    }
}
