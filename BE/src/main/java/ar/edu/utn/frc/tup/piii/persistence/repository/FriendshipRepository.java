package ar.edu.utn.frc.tup.piii.persistence.repository;

import ar.edu.utn.frc.tup.piii.persistence.entity.FriendshipEntity;
import ar.edu.utn.frc.tup.piii.persistence.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface FriendshipRepository extends JpaRepository<FriendshipEntity, Long> {

    @Query("SELECT f FROM FriendshipEntity f WHERE (f.user1 = :user1 AND f.user2 = :user2) OR (f.user1 = :user2 AND f.user2 = :user1)")
    Optional<FriendshipEntity> findByUsers(@Param("user1") UserEntity user1, @Param("user2") UserEntity user2);

    @Query("SELECT f FROM FriendshipEntity f JOIN FETCH f.user1 JOIN FETCH f.user2 WHERE (f.user1 = :user OR f.user2 = :user) AND f.status = :status")
    List<FriendshipEntity> findByUserAndStatus(@Param("user") UserEntity user, @Param("status") String status);
    
    @Query("SELECT f FROM FriendshipEntity f JOIN FETCH f.user1 JOIN FETCH f.user2 WHERE f.user2 = :user AND f.status = 'PENDING'")
    List<FriendshipEntity> findPendingRequestsForUser(@Param("user") UserEntity user);
}
