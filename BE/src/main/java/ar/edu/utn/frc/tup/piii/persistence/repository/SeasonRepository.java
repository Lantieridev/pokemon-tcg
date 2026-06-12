package ar.edu.utn.frc.tup.piii.persistence.repository;

import ar.edu.utn.frc.tup.piii.persistence.entity.SeasonEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SeasonRepository extends JpaRepository<SeasonEntity, Long> {

    @Query("SELECT s FROM SeasonEntity s WHERE s.status = 'ACTIVE'")
    Optional<SeasonEntity> findActiveSeason();
}
