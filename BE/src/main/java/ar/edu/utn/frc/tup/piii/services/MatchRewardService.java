package ar.edu.utn.frc.tup.piii.services;

import ar.edu.utn.frc.tup.piii.engine.session.MatchBoard;
import ar.edu.utn.frc.tup.piii.engine.session.MatchSession;
import ar.edu.utn.frc.tup.piii.engine.session.MatchSessionState;
import ar.edu.utn.frc.tup.piii.persistence.entity.Tier;
import ar.edu.utn.frc.tup.piii.persistence.entity.UserEntity;
import ar.edu.utn.frc.tup.piii.persistence.repository.UserRepository;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.Objects;

/**
 * Resolves who won a finished match and applies the consequences that follow from it:
 * ranked MMR/tier updates and campaign node completion.
 *
 * <p>Extracted out of {@link MatchService} because "who won, and what does that unlock"
 * is a self-contained concern with its own collaborators (ranking, campaign progress),
 * independent of the session-locking and turn-phase orchestration that owns the rest of
 * {@link MatchService}.</p>
 */
@Service
public class MatchRewardService {

    private static final int RANKED_WIN_BATTLE_POINTS = 10;

    private final UserRepository userRepository;
    private final MmrCalculationService mmrCalculationService;
    private final CampaignService campaignService;

    public MatchRewardService(final UserRepository userRepository,
                               final MmrCalculationService mmrCalculationService,
                               @Lazy final CampaignService campaignService) {
        this.userRepository = Objects.requireNonNull(userRepository, "userRepository must not be null");
        this.mmrCalculationService = Objects.requireNonNull(mmrCalculationService, "mmrCalculationService must not be null");
        this.campaignService = Objects.requireNonNull(campaignService, "campaignService must not be null");
    }

    /**
     * Determines the winner of a finished match using the standard priority order: prize
     * cards taken, then Pokémon bankruptcy (no active and no bench), then deck-out.
     *
     * @param session the finished match session (never null)
     * @return the winning player's id
     */
    public String determineWinner(final MatchSession session) {
        final MatchBoard board = session.getBoard();
        final String playerA = session.getPlayerIdA();
        final String playerB = session.getPlayerIdB();

        // 1. Condición de Premios (Prize cards)
        if (board.getRemainingPrizes(0) == 0) {
            return playerA;
        }
        if (board.getRemainingPrizes(1) == 0) {
            return playerB;
        }

        // 2. Condición de Bancarrota de Pokémon (Active + Bench)
        final boolean hasActiveA = board.getActivePokemon(0) != null;
        final boolean hasBenchA = !board.getBenchedPokemon(0).isEmpty();
        final boolean hasActiveB = board.getActivePokemon(1) != null;
        final boolean hasBenchB = !board.getBenchedPokemon(1).isEmpty();

        if (!hasActiveA && !hasBenchA) {
            return playerB;
        }
        if (!hasActiveB && !hasBenchB) {
            return playerA;
        }

        // 3. Condición de Deck out
        if (board.getDeckSize(0) == 0) {
            return playerB;
        }
        if (board.getDeckSize(1) == 0) {
            return playerA;
        }

        // Fallback
        return playerA;
    }

    /**
     * Recalculates and persists MMR/tier for both players of a ranked match, and records
     * the transient MMR-change fields on the session for the response DTOs.
     *
     * @param session  the finished ranked match session (never null)
     * @param winnerId the winning player's id (never null)
     * @param loserId  the losing player's id (never null)
     */
    public void updateMmr(final MatchSession session, final String winnerId, final String loserId) {
        final UserEntity winner = userRepository.findFirstByUsername(winnerId).orElse(null);
        final UserEntity loser = userRepository.findFirstByUsername(loserId).orElse(null);

        if (winner != null && loser != null) {
            final int winnerMmr = winner.getMmr() != null ? winner.getMmr() : 1000;
            final int loserMmr = loser.getMmr() != null ? loser.getMmr() : 1000;

            final int winnerRankedMatches = winner.getRankedMatchesPlayed() != null ? winner.getRankedMatchesPlayed() : 0;
            final int loserRankedMatches = loser.getRankedMatchesPlayed() != null ? loser.getRankedMatchesPlayed() : 0;

            final Tier winnerTierBefore = Tier.fromMmrAndMatches(winnerMmr, winnerRankedMatches);
            final Tier loserTierBefore = Tier.fromMmrAndMatches(loserMmr, loserRankedMatches);

            final int newWinnerMmr = mmrCalculationService.calculateNewMmr(winnerMmr, loserMmr, true, winnerRankedMatches);
            final int newLoserMmr = mmrCalculationService.calculateNewMmr(loserMmr, winnerMmr, false, loserRankedMatches);

            winner.setMmr(newWinnerMmr);
            winner.setRankedMatchesPlayed(winnerRankedMatches + 1);
            winner.setBattlePoints((winner.getBattlePoints() != null ? winner.getBattlePoints() : 0) + RANKED_WIN_BATTLE_POINTS);
            loser.setMmr(newLoserMmr);
            loser.setRankedMatchesPlayed(loserRankedMatches + 1);

            userRepository.save(winner);
            userRepository.save(loser);

            final Tier winnerTierAfter = Tier.fromMmrAndMatches(newWinnerMmr, winnerRankedMatches + 1);
            final Tier loserTierAfter = Tier.fromMmrAndMatches(newLoserMmr, loserRankedMatches + 1);

            final boolean winnerRankedUp = winnerTierAfter.isHigherThan(winnerTierBefore);
            final boolean loserRankedUp = loserTierAfter.isHigherThan(loserTierBefore);

            if (winnerId.equals(session.getPlayerIdA())) {
                session.setMmrChangeA(newWinnerMmr - winnerMmr);
                session.setMmrChangeB(newLoserMmr - loserMmr);
                session.setCurrentMmrA(newWinnerMmr);
                session.setCurrentMmrB(newLoserMmr);
                session.setCurrentTierA(winnerTierAfter.getName());
                session.setCurrentTierB(loserTierAfter.getName());
                session.setRankUpTriggeredA(winnerRankedUp);
                session.setRankUpTriggeredB(loserRankedUp);
            } else {
                session.setMmrChangeB(newWinnerMmr - winnerMmr);
                session.setMmrChangeA(newLoserMmr - loserMmr);
                session.setCurrentMmrB(newWinnerMmr);
                session.setCurrentMmrA(newLoserMmr);
                session.setCurrentTierB(winnerTierAfter.getName());
                session.setCurrentTierA(loserTierAfter.getName());
                session.setRankUpTriggeredB(winnerRankedUp);
                session.setRankUpTriggeredA(loserRankedUp);
            }
        }
    }

    /**
     * Completes the human player's campaign node if this finished match was against a
     * campaign bot and the human player won.
     *
     * @param session the match session, finished or not (never null)
     */
    public void handleCampaignCompletion(final MatchSession session) {
        if (session.getState() != MatchSessionState.FINISHED) {
            return;
        }
        final String winnerId = session.getWinnerId();
        if (winnerId == null) {
            return;
        }

        final String playerA = session.getPlayerIdA();
        final String playerB = session.getPlayerIdB();

        for (final CampaignService.CampaignNodeInfo node : CampaignService.NODES) {
            if (node.botId().equals(playerA) || node.botId().equals(playerB)) {
                final String humanPlayer = node.botId().equals(playerA) ? playerB : playerA;
                if (winnerId.equals(humanPlayer)) {
                    campaignService.completeNode(humanPlayer, node.id(), session.getMatchId());
                }
                break;
            }
        }
    }
}
