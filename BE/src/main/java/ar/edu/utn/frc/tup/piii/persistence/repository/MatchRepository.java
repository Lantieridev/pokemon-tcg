package ar.edu.utn.frc.tup.piii.persistence.repository;

import ar.edu.utn.frc.tup.piii.dtos.RankingDto;
import ar.edu.utn.frc.tup.piii.persistence.entity.MatchEntity;
import ar.edu.utn.frc.tup.piii.persistence.entity.UserEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MatchRepository extends JpaRepository<MatchEntity, Long> {

    @Query("SELECT m FROM MatchEntity m WHERE m.player1.username = :username OR m.player2.username = :username ORDER BY m.createdAt DESC")
    List<MatchEntity> findMatchesByUsername(@Param("username") String username);

    @Modifying
    @Query("UPDATE MatchEntity m SET m.winner = :winner WHERE m.id = :id AND m.winner IS NULL")
    int updateWinnerIfNull(@Param("id") Long id, @Param("winner") UserEntity winner);

    @Query("SELECT new ar.edu.utn.frc.tup.piii.dtos.RankingDto(m.winner.username, COUNT(m)) " +
           "FROM MatchEntity m " +
           "WHERE m.winner IS NOT NULL AND m.status = 'FINISHED' " +
           "GROUP BY m.winner.username " +
           "ORDER BY COUNT(m) DESC")
    Slice<RankingDto> getGlobalRanking(Pageable pageable);
}


