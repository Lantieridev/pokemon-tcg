package ar.edu.utn.frc.tup.piii.services;

import ar.edu.utn.frc.tup.piii.engine.model.Card;
import ar.edu.utn.frc.tup.piii.persistence.entity.CardEntity;
import ar.edu.utn.frc.tup.piii.persistence.entity.DeckCardEntity;
import ar.edu.utn.frc.tup.piii.persistence.entity.DeckEntity;
import ar.edu.utn.frc.tup.piii.persistence.mapper.CardMapper;
import ar.edu.utn.frc.tup.piii.persistence.repository.CardRepository;
import ar.edu.utn.frc.tup.piii.persistence.repository.DeckRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class JpaCardResolutionService implements CardResolutionService {

    private final DeckRepository deckRepository;
    private final CardRepository cardRepository;
    private final CardMapper cardMapper;

    public JpaCardResolutionService(DeckRepository deckRepository, CardRepository cardRepository, CardMapper cardMapper) {
        this.deckRepository = deckRepository;
        this.cardRepository = cardRepository;
        this.cardMapper = cardMapper;
    }

    @Override
    public List<Card> resolveCards(Long deckId) {
        Optional<DeckEntity> optDeck = deckRepository.findById(deckId);

        List<Card> deck = new ArrayList<>();
        if (optDeck.isPresent()) {
            DeckEntity deckEntity = optDeck.get();
            for (DeckCardEntity dce : deckEntity.getCards()) {
                for (int i = 0; i < dce.getQuantity(); i++) {
                    deck.add(cardMapper.map(dce.getCard()));
                }
            }
        } else {
            // Fallback deck for testing if deck does not exist in DB
            addFallbackCard(deck, "xy1-42", 4); // Pikachu
            addFallbackCard(deck, "xy1-43", 4); // Raichu
            addFallbackCard(deck, "xy1-11", 4); // Charizard-EX
            addFallbackCard(deck, "xy1-12", 4); // Chespin
            addFallbackCard(deck, "xy1-135", 14); // Lightning Energy
            addFallbackCard(deck, "xy1-133", 15); // Fire Energy
            addFallbackCard(deck, "xy1-132", 15); // Grass Energy
        }

        if (deck.isEmpty()) {
            throw new IllegalArgumentException("Deck is empty");
        }

        return deck;
    }
    
    private void addFallbackCard(List<Card> deck, String cardId, int quantity) {
        Optional<CardEntity> entity = cardRepository.findById(cardId);
        if (entity.isPresent()) {
            for (int i = 0; i < quantity; i++) {
                deck.add(cardMapper.map(entity.get()));
            }
        }
    }
}
