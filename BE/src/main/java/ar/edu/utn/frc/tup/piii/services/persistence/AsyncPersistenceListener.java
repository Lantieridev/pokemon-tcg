package ar.edu.utn.frc.tup.piii.services.persistence;

import ar.edu.utn.frc.tup.piii.engine.session.MatchSession;
import ar.edu.utn.frc.tup.piii.engine.session.MatchStatisticsTracker;
import ar.edu.utn.frc.tup.piii.persistence.entity.MatchEntity;
import ar.edu.utn.frc.tup.piii.persistence.entity.MatchLogEntity;
import ar.edu.utn.frc.tup.piii.persistence.entity.UserCardStatEntity;
import ar.edu.utn.frc.tup.piii.persistence.entity.UserEnergyStatEntity;
import ar.edu.utn.frc.tup.piii.persistence.entity.UserEntity;
import ar.edu.utn.frc.tup.piii.persistence.repository.MatchLogRepository;
import ar.edu.utn.frc.tup.piii.persistence.repository.MatchRepository;
import ar.edu.utn.frc.tup.piii.persistence.repository.UserCardStatRepository;
import ar.edu.utn.frc.tup.piii.persistence.repository.UserEnergyStatRepository;
import ar.edu.utn.frc.tup.piii.persistence.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Component
public class AsyncPersistenceListener {

    private static final Logger log = LoggerFactory.getLogger(AsyncPersistenceListener.class);

    private final MatchRepository matchRepository;
    private final MatchLogRepository matchLogRepository;
    private final UserRepository userRepository;
    private final UserCardStatRepository userCardStatRepository;
    private final UserEnergyStatRepository userEnergyStatRepository;
    private final ObjectMapper objectMapper;

    public AsyncPersistenceListener(final MatchRepository matchRepository,
                                    final MatchLogRepository matchLogRepository,
                                    final UserRepository userRepository,
                                    final UserCardStatRepository userCardStatRepository,
                                    final UserEnergyStatRepository userEnergyStatRepository,
                                    final ObjectMapper objectMapper) {
        this.matchRepository = Objects.requireNonNull(matchRepository);
        this.matchLogRepository = Objects.requireNonNull(matchLogRepository);
        this.userRepository = Objects.requireNonNull(userRepository);
        this.userCardStatRepository = Objects.requireNonNull(userCardStatRepository);
        this.userEnergyStatRepository = Objects.requireNonNull(userEnergyStatRepository);
        this.objectMapper = Objects.requireNonNull(objectMapper);
    }

    @Async
    @EventListener
    @Transactional
    public void onSaveMatch(final SaveMatchEvent event) {
        final MatchSession session = event.session();
        final Long matchIdNumeric = parseOrHashId(session.getMatchId());

        final UserEntity player1 = getOrCreateUser(session.getPlayerIdA());
        final UserEntity player2 = getOrCreateUser(session.getPlayerIdB());
        final UserEntity winner = session.getWinnerId() != null ? getOrCreateUser(session.getWinnerId()) : null;

        final MatchEntity entity = matchRepository.findById(matchIdNumeric)
                .orElseGet(() -> MatchEntity.builder().id(matchIdNumeric).build());

        entity.setStatus(session.getState().name());
        entity.setPlayer1(player1);
        entity.setPlayer2(player2);
        entity.setWinner(winner);
        entity.setCurrentState(session);

        // Serialize match-specific statistics
        if (session.hasPlayerRuntimes()) {
            final MatchStatisticsTracker tracker1 = session.getPlayerRuntime(0).getStatisticsTracker();
            try {
                entity.setPlayer1StatsJson(objectMapper.writeValueAsString(tracker1));
            } catch (Exception e) {
                log.error("Failed to serialize player 1 statistics", e);
            }
            // Update global statistics for player 1
            updateGlobalStats(player1, tracker1);

            final MatchStatisticsTracker tracker2 = session.getPlayerRuntime(1).getStatisticsTracker();
            try {
                entity.setPlayer2StatsJson(objectMapper.writeValueAsString(tracker2));
            } catch (Exception e) {
                log.error("Failed to serialize player 2 statistics", e);
            }
            // Update global statistics for player 2
            updateGlobalStats(player2, tracker2);
        }

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

    private void updateGlobalStats(final UserEntity user, final MatchStatisticsTracker tracker) {
        if (user == null || tracker == null) {
            return;
        }

        // 1. Update Card Stats
        java.util.Set<String> cardIds = new java.util.HashSet<>();
        cardIds.addAll(tracker.getPokemonPlayedCounts().keySet());
        cardIds.addAll(tracker.getPokemonDamageDealt().keySet());
        cardIds.addAll(tracker.getPokemonDamageReceived().keySet());
        cardIds.addAll(tracker.getPokemonKOsMade().keySet());
        cardIds.addAll(tracker.getPokemonKOsSuffered().keySet());

        for (String cardId : cardIds) {
            if (cardId == null) continue;
            UserCardStatEntity stat = userCardStatRepository.findByUserIdAndCardId(user.getId(), cardId);
            if (stat == null) {
                stat = UserCardStatEntity.builder()
                        .user(user)
                        .cardId(cardId)
                        .timesPlayed(0)
                        .damageDealt(0)
                        .damageReceived(0)
                        .kosMade(0)
                        .kosSuffered(0)
                        .build();
            }
            stat.setTimesPlayed(stat.getTimesPlayed() + tracker.getPokemonPlayedCounts().getOrDefault(cardId, 0));
            stat.setDamageDealt(stat.getDamageDealt() + tracker.getPokemonDamageDealt().getOrDefault(cardId, 0));
            stat.setDamageReceived(stat.getDamageReceived() + tracker.getPokemonDamageReceived().getOrDefault(cardId, 0));
            stat.setKosMade(stat.getKosMade() + tracker.getPokemonKOsMade().getOrDefault(cardId, 0));
            stat.setKosSuffered(stat.getKosSuffered() + tracker.getPokemonKOsSuffered().getOrDefault(cardId, 0));
            userCardStatRepository.save(stat);
        }

        // 2. Update Energy Stats
        for (java.util.Map.Entry<ar.edu.utn.frc.tup.piii.engine.model.PokemonType, Integer> entry : tracker.getEnergyAttachedCounts().entrySet()) {
            ar.edu.utn.frc.tup.piii.engine.model.PokemonType type = entry.getKey();
            int count = entry.getValue();
            if (type == null || count <= 0) continue;
            String typeName = type.name();
            UserEnergyStatEntity stat = userEnergyStatRepository.findByUserIdAndEnergyType(user.getId(), typeName);
            if (stat == null) {
                stat = UserEnergyStatEntity.builder()
                        .user(user)
                        .energyType(typeName)
                        .timesPlayed(0)
                        .build();
            }
            stat.setTimesPlayed(stat.getTimesPlayed() + count);
            userEnergyStatRepository.save(stat);
        }
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
