package ar.edu.utn.frc.tup.piii.persistence.repository;

import ar.edu.utn.frc.tup.piii.persistence.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.Query;

@Repository
public interface UserRepository extends JpaRepository<UserEntity, Long> {
    Optional<UserEntity> findByUsername(String username);

    @Query("SELECT new ar.edu.utn.frc.tup.piii.dtos.RankingDto(u.username, u.mmr, '', u.rankedMatchesPlayed) " +
           "FROM UserEntity u " +
           "WHERE u.mmr IS NOT NULL " +
           "ORDER BY u.mmr DESC")
    Slice<ar.edu.utn.frc.tup.piii.dtos.RankingDto> getGlobalRanking(Pageable pageable);
}
