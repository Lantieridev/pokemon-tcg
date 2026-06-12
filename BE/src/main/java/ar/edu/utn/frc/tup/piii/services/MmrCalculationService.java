package ar.edu.utn.frc.tup.piii.services;

import org.springframework.stereotype.Service;

@Service
public class MmrCalculationService {

    private static final int BASE_MMR = 1000;
    private static final int PLACEMENT_MATCHES = 10;
    
    // K-Factors
    private static final int K_PLACEMENT = 60;
    private static final int K_BEGINNER = 32;
    private static final int K_INTERMEDIATE = 24;
    private static final int K_EXPERT = 16;
    
    private static final int THRESHOLD_INTERMEDIATE = 1500;
    private static final int THRESHOLD_EXPERT = 2000;

    /**
     * Calculates the new MMR using the Elo formula with dynamic K-Factor.
     *
     * @param currentMmr          The player's current MMR
     * @param opponentMmr         The opponent's current MMR
     * @param isWin               True if the player won, false if they lost
     * @param rankedMatchesPlayed Total ranked matches played by this player so far
     * @return The updated MMR (never drops below 0)
     */
    public int calculateNewMmr(int currentMmr, int opponentMmr, boolean isWin, int rankedMatchesPlayed) {
        double expectedScore = 1.0 / (1.0 + Math.pow(10.0, (opponentMmr - currentMmr) / 400.0));
        double actualScore = isWin ? 1.0 : 0.0;
        
        int kFactor = determineKFactor(currentMmr, rankedMatchesPlayed);
        
        int newMmr = (int) Math.round(currentMmr + kFactor * (actualScore - expectedScore));
        
        return Math.max(0, newMmr);
    }

    /**
     * Determines the appropriate K-factor based on placement status and current MMR.
     */
    private int determineKFactor(int currentMmr, int rankedMatchesPlayed) {
        if (rankedMatchesPlayed < PLACEMENT_MATCHES) {
            return K_PLACEMENT;
        }
        if (currentMmr < THRESHOLD_INTERMEDIATE) {
            return K_BEGINNER;
        }
        if (currentMmr < THRESHOLD_EXPERT) {
            return K_INTERMEDIATE;
        }
        return K_EXPERT;
    }

    /**
     * Calculates the soft-reset MMR at the end of a season.
     * Pulls the player halfway back to the BASE_MMR.
     */
    public int calculateSoftResetMmr(int currentMmr) {
        int newMmr = (int) Math.round((currentMmr - BASE_MMR) * 0.5 + BASE_MMR);
        return Math.max(0, newMmr); // Just in case
    }
}
