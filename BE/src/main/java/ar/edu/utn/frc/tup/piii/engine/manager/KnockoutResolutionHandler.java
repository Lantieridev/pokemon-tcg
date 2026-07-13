package ar.edu.utn.frc.tup.piii.engine.manager;

import ar.edu.utn.frc.tup.piii.engine.listener.KnockoutHandler;
import ar.edu.utn.frc.tup.piii.engine.model.BattlePokemonState;
import ar.edu.utn.frc.tup.piii.engine.session.PlayerRuntime;

import java.util.List;
import java.util.Objects;

/**
 * Resolves the full consequences of a Pokémon being knocked out (FR-008 through FR-011):
 * <ol>
 *   <li>Identifies which player owns the knocked-out Pokémon (defender).</li>
 *   <li>Removes it from the owner's active slot or bench and discards its card.</li>
 *   <li>Awards {@code prizesToTake} prize cards from the attacker's prize pile to their hand.</li>
 *   <li>Delegates to the downstream {@link KnockoutHandler} (typically
 *       {@link VictoryConditionChecker}) to check end-game conditions.</li>
 * </ol>
 *
 * <p>Uses the {@link TurnManager} to determine the current attacker index at the moment
 * the knockout fires, avoiding any mutable attacker-index field. Pure POJO — zero Spring imports.</p>
 */
public final class KnockoutResolutionHandler implements KnockoutHandler {

    private final List<PlayerRuntime> playerRuntimes;
    private final TurnManager turnManager;
    private final KnockoutHandler downstream;

    /**
     * @param playerRuntimes live runtime state for both players (never null, size must be 2)
     * @param turnManager    provides the currently active player index (never null)
     * @param downstream     handler to invoke after prize transfer (typically VictoryConditionChecker)
     */
    public KnockoutResolutionHandler(final List<PlayerRuntime> playerRuntimes,
                                      final TurnManager turnManager,
                                      final KnockoutHandler downstream) {
        this.playerRuntimes = Objects.requireNonNull(playerRuntimes, "playerRuntimes must not be null");
        this.turnManager = Objects.requireNonNull(turnManager, "turnManager must not be null");
        this.downstream = Objects.requireNonNull(downstream, "downstream must not be null");
    }

    /**
     * Resolves the knockout: discards the Pokémon's card, transfers prizes to the attacker's hand,
     * then notifies the downstream handler for victory-condition evaluation.
     *
     * @param knocked      the Pokémon that was knocked out
     * @param prizesToTake number of prize cards the attacker should take
     */
    public void onKnockout(final BattlePokemonState knocked, final int prizesToTake) {
        // Find which player owns `knocked`
        int ownerIndex = -1;
        if (Objects.equals(knocked, playerRuntimes.get(0).getActivePokemon()) || playerRuntimes.get(0).getBench().getAll().contains(knocked)) {
            ownerIndex = 0;
        } else if (Objects.equals(knocked, playerRuntimes.get(1).getActivePokemon()) || playerRuntimes.get(1).getBench().getAll().contains(knocked)) {
            ownerIndex = 1;
        }

        if (ownerIndex == -1) {
            return; // Safety guard: Pokémon not found on either side
        }
        
        final int opponentIndex = 1 - ownerIndex;
        final PlayerRuntime owner = playerRuntimes.get(ownerIndex);
        owner.setKnockedOutLastTurn(true);
        final PlayerRuntime opponentPlayer = playerRuntimes.get(opponentIndex);

        // Discard all cards associated with the knocked Pokémon
        owner.getDiscardPile().add(knocked.getBaseCard());
        knocked.getUnderlyingCards().forEach(owner.getDiscardPile()::add);
        knocked.getAttachedEnergyCards().forEach(owner.getDiscardPile()::add);
        knocked.getAttachedTool().ifPresent(tool -> {
            owner.getDiscardPile().add(tool);
            knocked.detachTool();
        });

        // Remove from field: active slot or bench
        if (knocked.equals(owner.getActivePokemon())) {
            // Active slot is left empty — the player must promote a benched Pokémon
            // via a subsequent replacement action from the client
            owner.clearActivePokemon();
        } else {
            removeFromBench(owner, knocked);
        }

        // Award prizes to the opponent (taken from their prize pile into hand)
        opponentPlayer.takePrizes(prizesToTake);

        // Record KO in statistics trackers
        if (owner.getStatisticsTracker() != null && knocked != null) {
            owner.getStatisticsTracker().incrementKOsSuffered(knocked.getCardId());
        }
        if (opponentPlayer.getStatisticsTracker() != null && opponentPlayer.getActivePokemon() != null) {
            opponentPlayer.getStatisticsTracker().incrementKOsMade(opponentPlayer.getActivePokemon().getCardId());
        }

        // Remove from turnsInPlay tracking — this Pokémon is no longer in play
        owner.removePokemonFromPlay(knocked);

        // Notify downstream handler (VictoryConditionChecker)
        downstream.onKnockout(knocked, prizesToTake);
    }

    /**
     * Searches and removes the knocked-out Pokémon from the defender's bench.
     * No-op if not found (defensive guard for status-damage KOs during between-turns).
     *
     * @param defender the player whose bench to search
     * @param knocked  the Pokémon to remove
     */
    private void removeFromBench(final PlayerRuntime defender, final BattlePokemonState knocked) {
        final List<BattlePokemonState> benched = defender.getBench().getAll();
        for (int i = 0; i < benched.size(); i++) {
            if (benched.get(i).equals(knocked)) {
                defender.getBench().remove(i);
                return;
            }
        }
    }
}
