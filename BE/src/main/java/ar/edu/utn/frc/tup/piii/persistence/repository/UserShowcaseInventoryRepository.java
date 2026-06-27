package ar.edu.utn.frc.tup.piii.persistence.repository;

import ar.edu.utn.frc.tup.piii.persistence.entity.UserShowcaseInventoryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserShowcaseInventoryRepository extends JpaRepository<UserShowcaseInventoryEntity, Long> {
    List<UserShowcaseInventoryEntity> findByUserId(Long userId);
}
