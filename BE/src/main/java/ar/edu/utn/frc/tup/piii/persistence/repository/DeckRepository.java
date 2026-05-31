package ar.edu.utn.frc.tup.piii.persistence.repository;

import ar.edu.utn.frc.tup.piii.persistence.entity.DeckEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DeckRepository extends JpaRepository<DeckEntity, Long> {

    @EntityGraph(attributePaths = {"cards", "cards.card"})
    Optional<DeckEntity> findById(Long id);

    @Query("SELECT COUNT(dc) > 0 FROM DeckCardEntity dc WHERE dc.deck.user.id = :userId AND dc.id.cardId = :cardId")
    boolean existsByUserIdAndCardId(@Param("userId") Long userId, @Param("cardId") String cardId);

    @Query("SELECT COUNT(DISTINCT dc.id.cardId) FROM DeckCardEntity dc WHERE dc.deck.user.id = :userId")
    int countUniqueCardsByUserId(@Param("userId") Long userId);
}
