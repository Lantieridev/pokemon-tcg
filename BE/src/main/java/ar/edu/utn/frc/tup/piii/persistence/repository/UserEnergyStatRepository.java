package ar.edu.utn.frc.tup.piii.persistence.repository;

import ar.edu.utn.frc.tup.piii.persistence.entity.UserEnergyStatEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserEnergyStatRepository extends JpaRepository<UserEnergyStatEntity, Long> {
    List<UserEnergyStatEntity> findByUserId(Long userId);
    UserEnergyStatEntity findByUserIdAndEnergyType(Long userId, String energyType);
}
