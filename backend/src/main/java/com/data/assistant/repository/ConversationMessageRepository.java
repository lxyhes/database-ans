package com.data.assistant.repository;

import com.data.assistant.model.ConversationMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ConversationMessageRepository extends JpaRepository<ConversationMessage, Long> {

    List<ConversationMessage> findByConversationIdOrderBySequenceAsc(Long conversationId);

    Integer countByConversationId(Long conversationId);
}
