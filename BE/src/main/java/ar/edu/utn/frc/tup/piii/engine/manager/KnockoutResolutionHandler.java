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
 * <p>The attacker is derived from which side owns the knocked-out Pokémon (the other side), so no
 * turn-index lookup is needed. Pure POJO — zero Spring imports.</p>
 */
public final class KnockoutResolutionHandler implements KnockoutHandler {

    private final List<PlayerRuntime> playerRuntimes;
    private final KnockoutHandler downstream;

    /**
     * @param playerRuntimes live runtime state for both players (never null, size must be 2)
     * @param downstream     handler to invoke after prize transfer (typically VictoryConditionChecker)
     */
    public KnockoutResolutionHandler(final List<PlayerRuntime> playerRuntimes,
                                      final KnockoutHandler downstream) {
        this.playerRuntimes = Objects.requireNonNull(playerRuntimes, "playerRuntimes must not be null");
        this.downstream = Objects.requireNonNull(downstream, "downstream must not be null");
    }

    /**
     * Resolves the knockout: discards the Pokémon's card, transfers prizes to the attacker's hand,
     * then notifies the downstream handler for victory-condition evaluation.
     *
     * @param knocked      the Pokémon that was knocked out
     * @param prizesToTake number of prize cards the attacker should take
     */
    @Override
    public void onKnockout(final BattlePokemonState knocked, final int prizesToTake) {
        final int ownerIndex = findOwnerIndex(knocked);
        if (ownerIndex == -1) {
            return; // Safety guard: Pokémon not found on either side
        }

        final int opponentIndex = 1 - ownerIndex;
        final PlayerRuntime owner = playerRuntimes.get(ownerIndex);
        owner.setKnockedOutLastTurn(true);
        final PlayerRuntime opponentPlayer = playerRuntimes.get(opponentIndex);

        discardKnockedCards(owner, knocked);
        removeFromField(owner, knocked);

        // Award prizes to the opponent (taken from their prize pile into hand)
        opponentPlayer.takePrizes(prizesToTake);

        recordKnockoutStats(owner, opponentPlayer, knocked);

        // Remove from turnsInPlay tracking — this Pokémon is no longer in play
        owner.removePokemonFromPlay(knocked);

        // Notify downstream handler (VictoryConditionChecker)
        downstream.onKnockout(knocked, prizesToTake);
    }

    private int findOwnerIndex(final BattlePokemonState knocked) {
        if (isOwnedBy(playerRuntimes.get(0), knocked)) {
            return 0;
        }
        if (isOwnedBy(playerRuntimes.get(1), knocked)) {
            return 1;
        }
        return -1;
    }

    private boolean isOwnedBy(final PlayerRuntime runtime, final BattlePokemonState knocked) {
        return Objects.equals(knocked, runtime.getActivePokemon()) || runtime.getBench().getAll().contains(knocked);
    }

    // Discard all cards associated with the knocked Pokémon
    private void discardKnockedCards(final PlayerRuntime owner, final BattlePokemonState knocked) {
        owner.getDiscardPile().add(knocked.getBaseCard());
        knocked.getUnderlyingCards().forEach(owner.getDiscardPile()::add);
        knocked.getAttachedEnergyCards().forEach(owner.getDiscardPile()::add);
        knocked.getAttachedTool().ifPresent(tool -> {
            owner.getDiscardPile().add(tool);
            knocked.detachTool();
        });
    }

    private void removeFromField(final PlayerRuntime owner, final BattlePokemonState knocked) {
        if (knocked.equals(owner.getActivePokemon())) {
            // Active slot is left empty — the player must promote a benched Pokémon
            // via a subsequent replacement action from the client
            owner.clearActivePokemon();
        } else {
            removeFromBench(owner, knocked);
        }
    }

    private void recordKnockoutStats(final PlayerRuntime owner, final PlayerRuntime opponentPlayer, final BattlePokemonState knocked) {
        if (owner.getStatisticsTracker() != null) {
            owner.getStatisticsTracker().incrementKOsSuffered(knocked.getCardId());
        }
        if (opponentPlayer.getStatisticsTracker() != null && opponentPlayer.getActivePokemon() != null) {
            opponentPlayer.getStatisticsTracker().incrementKOsMade(opponentPlayer.getActivePokemon().getCardId());
        }
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
