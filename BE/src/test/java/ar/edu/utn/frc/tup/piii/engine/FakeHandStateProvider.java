package ar.edu.utn.frc.tup.piii.engine;

import ar.edu.utn.frc.tup.piii.engine.listener.HandStateProvider;
import ar.edu.utn.frc.tup.piii.engine.model.Card;

import java.util.HashMap;
import java.util.Map;

public class FakeHandStateProvider implements HandStateProvider {

    private final Map<Integer, Map<String, Card>> hands = new HashMap<>();

    public void addCard(int playerIndex, Card card) {
        hands.computeIfAbsent(playerIndex, k -> new HashMap<>()).put(card.getCardId(), card);
    }

    @Override
    public Card getCardInHand(int playerIndex, String cardId) {
        Map<String, Card> playerHand = hands.get(playerIndex);
        if (playerHand != null) {
            return playerHand.get(cardId);
        }
        return null;
    }
}
