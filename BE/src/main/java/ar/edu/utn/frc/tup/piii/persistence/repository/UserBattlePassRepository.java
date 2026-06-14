package ar.edu.utn.frc.tup.piii.persistence.repository;

import ar.edu.utn.frc.tup.piii.persistence.entity.UserBattlePassEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserBattlePassRepository extends JpaRepository<UserBattlePassEntity, Long> {
    Optional<UserBattlePassEntity> findByUserId(Long userId);
}
