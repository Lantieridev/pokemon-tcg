package ar.edu.utn.frc.tup.piii.persistence.repository;

import ar.edu.utn.frc.tup.piii.persistence.entity.UserCardStatEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserCardStatRepository extends JpaRepository<UserCardStatEntity, Long> {
    List<UserCardStatEntity> findByUserId(Long userId);
    List<UserCardStatEntity> findByUserIdAndCardId(Long userId, String cardId);
}
