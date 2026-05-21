package ar.edu.utn.frc.tup.piii.persistence.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Entity
@Table(name = "deck_cards")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeckCardEntity {

    @EmbeddedId
    private DeckCardId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("deckId")
    @JoinColumn(name = "deck_id")
    private DeckEntity deck;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("cardId")
    @JoinColumn(name = "card_id")
    private CardEntity card;

    private Integer quantity;

    @Embeddable
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DeckCardId implements Serializable {
        private Long deckId;
        private String cardId;
    }
}
