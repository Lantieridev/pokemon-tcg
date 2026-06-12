package ar.edu.utn.frc.tup.piii.persistence.repository;

import ar.edu.utn.frc.tup.piii.persistence.entity.SeasonRecordEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SeasonRecordRepository extends JpaRepository<SeasonRecordEntity, Long> {
}
