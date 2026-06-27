package ar.edu.utn.frc.tup.piii.persistence.repository;

import ar.edu.utn.frc.tup.piii.persistence.entity.BattlePassLevelEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BattlePassLevelRepository extends JpaRepository<BattlePassLevelEntity, Integer> {
}
