package ar.edu.utn.frc.tup.piii.engine.model;

/**
 * The main phase of a player's turn — the player may play cards, attach energy, and retreat.
 * Tracks per-turn limits to enforce game rules. FR-001, FR-010.
 */
public final class MainPhase implements TurnPhase {

    /** Canonical phase name. */
    private static final String PHASE_NAME = "MAIN";

    /** Maximum energy cards that may be attached during a single main phase. */
    private static final int MAX_ENERGY_PER_TURN = 1;

    private int energyAttached;
    private boolean supporterPlayed;
    private boolean stadiumPlayed;
    private boolean retreatUsed;

    /** Creates a fresh MainPhase with all counters at zero. */
    public MainPhase() {
        // all fields default to 0 / false
    }

    @Override
    public String name() {
        return PHASE_NAME;
    }

    /**
     * Returns the number of energy cards attached this turn.
     *
     * @return energy attached count
     */
    public int getEnergyAttached() {
        return energyAttached;
    }

    /**
     * Returns whether a Supporter card has been played this turn.
     *
     * @return true if a Supporter was played
     */
    public boolean isSupporterPlayed() {
        return supporterPlayed;
    }

    /**
     * Returns whether a Stadium card has been played this turn.
     *
     * @return true if a Stadium was played
     */
    public boolean isStadiumPlayed() {
        return stadiumPlayed;
    }

    /**
     * Returns whether the retreat action has been used this turn.
     *
     * @return true if retreat was used
     */
    public boolean isRetreatUsed() {
        return retreatUsed;
    }

    /**
     * Records that one energy card was attached this turn.
     *
     * @throws IllegalStateException if energy has already been attached this turn
     */
    public void recordEnergyAttached() {
        if (energyAttached >= MAX_ENERGY_PER_TURN) {
            throw new IllegalStateException("Energy already attached this turn");
        }
        energyAttached++;
    }

    /**
     * Records that a Supporter card was played this turn.
     *
     * @throws IllegalStateException if a Supporter was already played this turn
     */
    public void recordSupporterPlayed() {
        if (supporterPlayed) {
            throw new IllegalStateException("Supporter already played this turn");
        }
        supporterPlayed = true;
    }

    /**
     * Records that a Stadium card was played this turn.
     *
     * @throws IllegalStateException if a Stadium was already played this turn
     */
    public void recordStadiumPlayed() {
        if (stadiumPlayed) {
            throw new IllegalStateException("Stadium already played this turn");
        }
        stadiumPlayed = true;
    }

    /**
     * Records that the retreat action was used this turn.
     *
     * @throws IllegalStateException if retreat was already used this turn
     */
    public void recordRetreatUsed() {
        if (retreatUsed) {
            throw new IllegalStateException("Retreat already used this turn");
        }
        retreatUsed = true;
    }
}
