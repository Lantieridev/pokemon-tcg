package ar.edu.utn.frc.tup.piii.engine.listener;

/**
 * Provides prize-card state for a player.
 * A return value of {@code 0} from {@link #getRemainingPrizes} means the player has won by prizes.
 * FR-008.
 */
public interface PrizeStateProvider {

    /**
     * Returns the number of prize cards still face-down for the given player.
     *
     * @param playerIndex zero-based player index
     * @return remaining prizes (>= 0)
     */
    int getRemainingPrizes(int playerIndex);
}
