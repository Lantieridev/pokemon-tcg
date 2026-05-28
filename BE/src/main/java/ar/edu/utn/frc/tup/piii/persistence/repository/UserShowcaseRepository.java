package ar.edu.utn.frc.tup.piii.persistence.repository;

import ar.edu.utn.frc.tup.piii.persistence.entity.UserEntity;
import ar.edu.utn.frc.tup.piii.persistence.entity.UserShowcaseEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserShowcaseRepository extends JpaRepository<UserShowcaseEntity, Long> {
    List<UserShowcaseEntity> findByUserUsernameOrderBySlotPositionAsc(String username);
    Optional<UserShowcaseEntity> findByUserAndSlotPosition(UserEntity user, Integer slotPosition);
    Optional<UserShowcaseEntity> findByUserUsernameAndSlotPosition(String username, Integer slotPosition);
}
