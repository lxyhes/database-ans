package com.data.assistant.repository;

import com.data.assistant.model.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ConversationRepository extends JpaRepository<Conversation, Long> {

    Optional<Conversation> findBySessionId(String sessionId);

    List<Conversation> findByDataSourceIdAndIsActiveTrueOrderByUpdatedAtDesc(Long dataSourceId);

    List<Conversation> findByIsActiveTrueOrderByUpdatedAtDesc();

    @Modifying
    @Query("UPDATE Conversation c SET c.isActive = false WHERE c.sessionId = ?1")
    void deactivateBySessionId(String sessionId);
}
