package ar.edu.utn.frc.tup.piii.persistence.entity;

public enum Tier {
    UNRANKED("Unranked", 0),
    IRON("Iron", 1),
    BRONZE("Bronze", 2),
    SILVER("Silver", 3),
    GOLD("Gold", 4),
    PLATINUM("Platinum", 5),
    DIAMOND("Diamond", 6),
    MASTER("Master", 7),
    GRANDMASTER("Grandmaster", 8);

    private static final int MIN_RANKED_MATCHES = 10;
    private static final int IRON_MAX_MMR = 1200;
    private static final int BRONZE_MAX_MMR = 1400;
    private static final int SILVER_MAX_MMR = 1600;
    private static final int GOLD_MAX_MMR = 1800;
    private static final int PLATINUM_MAX_MMR = 2000;
    private static final int DIAMOND_MAX_MMR = 2200;
    private static final int MASTER_MAX_MMR = 2400;

    private final String name;
    private final int rank;

    Tier(final String name, final int rank) {
        this.name = name;
        this.rank = rank;
    }

    public String getName() {
        return name;
    }

    public int getRank() {
        return rank;
    }

    public boolean isHigherThan(final Tier other) {
        return this.rank > other.rank;
    }

    public static Tier fromMmrAndMatches(final Integer mmr, final Integer matchesPlayed) {
        if (mmr == null || matchesPlayed == null || matchesPlayed < MIN_RANKED_MATCHES) {
            return UNRANKED;
        }
        if (mmr < IRON_MAX_MMR) {
            return IRON;
        }
        if (mmr < BRONZE_MAX_MMR) {
            return BRONZE;
        }
        if (mmr < SILVER_MAX_MMR) {
            return SILVER;
        }
        if (mmr < GOLD_MAX_MMR) {
            return GOLD;
        }
        if (mmr < PLATINUM_MAX_MMR) {
            return PLATINUM;
        }
        if (mmr < DIAMOND_MAX_MMR) {
            return DIAMOND;
        }
        if (mmr < MASTER_MAX_MMR) {
            return MASTER;
        }
        return GRANDMASTER;
    }
}
