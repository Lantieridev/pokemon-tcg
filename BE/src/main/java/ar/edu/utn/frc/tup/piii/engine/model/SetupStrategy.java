package ar.edu.utn.frc.tup.piii.engine.model;

import java.util.List;

/**
 * Decision interface that encapsulates the player's choices during the Setup Phase.
 * Implementations are provided by the service layer or by test doubles.
 */
public interface SetupStrategy {

    /**
     * Selects the card from the hand to place as the Active Pokémon.
     *
     * @param hand current hand contents (at least one Basic Pokémon present)
     * @return cardId of the chosen Basic Pokémon (must be present in hand)
     */
    String chooseActiveCardId(List<Card> hand);

    /**
     * Selects zero to five cards from the remaining hand to place on the Bench.
     * Called after the Active has been removed from the hand.
     *
     * @param hand hand contents after the Active card has been removed
     * @return list of cardIds to bench (may be empty; must all be Basic Pokémon in hand)
     */
    List<String> chooseBenchCardIds(List<Card> hand);

    /**
     * Decides whether to draw the bonus cards earned from the opponent's Mulligans.
     *
     * @param opponentMulliganCount how many Mulligans the opponent declared
     * @return true to draw {@code opponentMulliganCount} bonus cards, false to skip
     */
    boolean acceptBonusDraws(int opponentMulliganCount);
}
