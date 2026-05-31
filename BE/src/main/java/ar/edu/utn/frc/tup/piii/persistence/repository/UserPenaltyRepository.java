package ar.edu.utn.frc.tup.piii.persistence.repository;

import ar.edu.utn.frc.tup.piii.persistence.entity.UserEntity;
import ar.edu.utn.frc.tup.piii.persistence.entity.UserPenaltyEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserPenaltyRepository extends JpaRepository<UserPenaltyEntity, Long> {
    List<UserPenaltyEntity> findByUserUsernameAndIsActiveTrue(String username);
    Optional<UserPenaltyEntity> findByUserUsernameAndPenaltyTypeAndIsActiveTrue(String username, String penaltyType);
    List<UserPenaltyEntity> findByUserAndIsActiveTrue(UserEntity user);
    List<UserPenaltyEntity> findByUser(UserEntity user);
}
