package ar.edu.utn.frc.tup.piii.engine;

import ar.edu.utn.frc.tup.piii.engine.listener.HandStateProvider;
import ar.edu.utn.frc.tup.piii.engine.model.Card;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class FakeHandStateProvider implements HandStateProvider {

    private final Map<Integer, Map<String, Card>> hands = new HashMap<>();

    public void addCard(int playerIndex, Card card) {
        hands.computeIfAbsent(playerIndex, k -> new HashMap<>()).put(card.getCardId(), card);
    }

    @Override
    public Optional<Card> getCardInHand(int playerIndex, String cardId) {
        final Map<String, Card> playerHand = hands.get(playerIndex);
        if (playerHand == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(playerHand.get(cardId));
    }

    @Override
    public java.util.List<Card> getHandCards(int playerIndex) {
        final Map<String, Card> playerHand = hands.get(playerIndex);
        if (playerHand == null) {
            return java.util.List.of();
        }
        return new java.util.ArrayList<>(playerHand.values());
    }
}
