package ar.edu.utn.frc.tup.piii.persistence.repository;

import ar.edu.utn.frc.tup.piii.dtos.MatchHistoryProjectionDto;
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

    @Query("SELECT new ar.edu.utn.frc.tup.piii.dtos.MatchHistoryProjectionDto(m.id, m.status, m.player1.username, m.player2.username, m.winner.username, m.createdAt, m.player1StatsJson, m.player2StatsJson) " +
           "FROM MatchEntity m WHERE m.player1.username = :username OR m.player2.username = :username ORDER BY m.createdAt DESC")
     List<MatchHistoryProjectionDto> findMatchesByUsername(@Param("username") String username);
 
 
 
     @Query("SELECT new ar.edu.utn.frc.tup.piii.dtos.RankingDto(m.winner.username, COUNT(m)) " +
            "FROM MatchEntity m " +
            "WHERE m.winner IS NOT NULL AND m.status = 'FINISHED' " +
            "GROUP BY m.winner.username " +
            "ORDER BY COUNT(m) DESC")
     Slice<RankingDto> getGlobalRanking(Pageable pageable);
 
     @Query("SELECT new ar.edu.utn.frc.tup.piii.dtos.MatchHistoryProjectionDto(" +
            "m.id, m.status, p1.username, p2.username, w.username, m.createdAt, m.player1StatsJson, m.player2StatsJson) " +
            "FROM MatchEntity m " +
            "LEFT JOIN m.player1 p1 " +
            "LEFT JOIN m.player2 p2 " +
            "LEFT JOIN m.winner w " +
            "WHERE p1.username = :username OR p2.username = :username " +
            "ORDER BY m.createdAt DESC")
     Slice<MatchHistoryProjectionDto> findUserMatchHistory(@Param("username") String username, Pageable pageable);
}


