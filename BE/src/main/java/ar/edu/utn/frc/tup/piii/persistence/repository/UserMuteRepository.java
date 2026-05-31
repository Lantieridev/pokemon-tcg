package ar.edu.utn.frc.tup.piii.persistence.repository;

import ar.edu.utn.frc.tup.piii.persistence.entity.UserEntity;
import ar.edu.utn.frc.tup.piii.persistence.entity.UserMuteEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserMuteRepository extends JpaRepository<UserMuteEntity, Long> {
    List<UserMuteEntity> findByUser(UserEntity user);
    List<UserMuteEntity> findByUserUsername(String username);
    Optional<UserMuteEntity> findByUserAndMutedUser(UserEntity user, UserEntity mutedUser);
    Optional<UserMuteEntity> findByUserUsernameAndMutedUserUsername(String username, String mutedUsername);
}
