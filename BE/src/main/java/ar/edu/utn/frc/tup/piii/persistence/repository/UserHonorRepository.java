package ar.edu.utn.frc.tup.piii.persistence.repository;

import ar.edu.utn.frc.tup.piii.persistence.entity.UserEntity;
import ar.edu.utn.frc.tup.piii.persistence.entity.UserHonorEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserHonorRepository extends JpaRepository<UserHonorEntity, Long> {
    List<UserHonorEntity> findByReceiver(UserEntity receiver);
    List<UserHonorEntity> findByReceiverUsername(String username);
}
