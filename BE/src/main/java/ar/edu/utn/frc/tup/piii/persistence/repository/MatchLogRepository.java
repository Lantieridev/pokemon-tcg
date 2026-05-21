package ar.edu.utn.frc.tup.piii.persistence.repository;

import ar.edu.utn.frc.tup.piii.persistence.entity.MatchLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MatchLogRepository extends JpaRepository<MatchLogEntity, Long> {
    List<MatchLogEntity> findByMatchIdOrderByCreatedAtAsc(Long matchId);
}
