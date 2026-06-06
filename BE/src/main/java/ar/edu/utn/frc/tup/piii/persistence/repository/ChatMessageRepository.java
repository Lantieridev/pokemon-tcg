package ar.edu.utn.frc.tup.piii.persistence.repository;

import ar.edu.utn.frc.tup.piii.persistence.entity.ChatMessageEntity;
import ar.edu.utn.frc.tup.piii.persistence.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessageEntity, Long> {

    @Query("SELECT c FROM ChatMessageEntity c WHERE (c.sender = :user1 AND c.receiver = :user2) OR (c.sender = :user2 AND c.receiver = :user1) ORDER BY c.createdAt ASC")
    List<ChatMessageEntity> findChatHistory(@Param("user1") UserEntity user1, @Param("user2") UserEntity user2);
}
