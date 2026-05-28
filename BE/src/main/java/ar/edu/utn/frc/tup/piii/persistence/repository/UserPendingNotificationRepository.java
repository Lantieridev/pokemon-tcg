package ar.edu.utn.frc.tup.piii.persistence.repository;

import ar.edu.utn.frc.tup.piii.persistence.entity.UserEntity;
import ar.edu.utn.frc.tup.piii.persistence.entity.UserPendingNotificationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserPendingNotificationRepository extends JpaRepository<UserPendingNotificationEntity, Long> {
    List<UserPendingNotificationEntity> findByUserUsername(String username);
    List<UserPendingNotificationEntity> findByUser(UserEntity user);
}
