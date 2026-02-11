package com.data.assistant.service;

import com.data.assistant.model.Conversation;
import com.data.assistant.model.ConversationMessage;
import com.data.assistant.repository.ConversationMessageRepository;
import com.data.assistant.repository.ConversationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class ConversationService {

    @Autowired
    private ConversationRepository conversationRepository;

    @Autowired
    private ConversationMessageRepository messageRepository;

    @Transactional
    public Conversation createConversation(Long dataSourceId, String provider, String title) {
        Conversation conversation = new Conversation();
        conversation.setSessionId(UUID.randomUUID().toString());
        conversation.setDataSourceId(dataSourceId);
        conversation.setProvider(provider);
        conversation.setTitle(title != null ? title : "新对话");
        conversation.setIsActive(true);
        return conversationRepository.save(conversation);
    }

    @Transactional(readOnly = true)
    public Optional<Conversation> getConversation(String sessionId) {
        return conversationRepository.findBySessionId(sessionId);
    }

    @Transactional
    public Conversation getOrCreateConversation(String sessionId, Long dataSourceId, String provider) {
        if (sessionId != null) {
            Optional<Conversation> existing = conversationRepository.findBySessionId(sessionId);
            if (existing.isPresent()) {
                return existing.get();
            }
        }
        return createConversation(dataSourceId, provider, null);
    }

    @Transactional(readOnly = true)
    public List<Conversation> getActiveConversations(Long dataSourceId) {
        if (dataSourceId != null) {
            return conversationRepository.findByDataSourceIdAndIsActiveTrueOrderByUpdatedAtDesc(dataSourceId);
        }
        return conversationRepository.findByIsActiveTrueOrderByUpdatedAtDesc();
    }

    @Transactional(readOnly = true)
    public List<ConversationMessage> getConversationHistory(String sessionId) {
        Optional<Conversation> conversation = conversationRepository.findBySessionId(sessionId);
        if (conversation.isPresent()) {
            return messageRepository.findByConversationIdOrderBySequenceAsc(conversation.get().getId());
        }
        return new ArrayList<>();
    }

    @Transactional
    public ConversationMessage saveUserMessage(String sessionId, String content) {
        Conversation conversation = conversationRepository.findBySessionId(sessionId)
                .orElseThrow(() -> new RuntimeException("Conversation not found"));

        Integer nextSequence = messageRepository.countByConversationId(conversation.getId()) + 1;

        ConversationMessage message = new ConversationMessage();
        message.setConversation(conversation);
        message.setMessageType(ConversationMessage.MessageType.USER);
        message.setContent(content);
        message.setSequence(nextSequence);

        return messageRepository.save(message);
    }

    @Transactional
    public ConversationMessage saveAssistantMessage(String sessionId, String content, String generatedSql, String queryResult) {
        Conversation conversation = conversationRepository.findBySessionId(sessionId)
                .orElseThrow(() -> new RuntimeException("Conversation not found"));

        Integer nextSequence = messageRepository.countByConversationId(conversation.getId()) + 1;

        ConversationMessage message = new ConversationMessage();
        message.setConversation(conversation);
        message.setMessageType(ConversationMessage.MessageType.ASSISTANT);
        message.setContent(content);
        message.setGeneratedSql(generatedSql);
        message.setQueryResult(queryResult);
        message.setSequence(nextSequence);

        return messageRepository.save(message);
    }

    @Transactional
    public void deactivateConversation(String sessionId) {
        conversationRepository.deactivateBySessionId(sessionId);
    }

    @Transactional(readOnly = true)
    public String buildConversationContext(String sessionId, int maxHistory) {
        List<ConversationMessage> history = getConversationHistory(sessionId);

        StringBuilder context = new StringBuilder();
        context.append("以下是对话历史上下文：\n\n");

        int startIndex = Math.max(0, history.size() - maxHistory);
        for (int i = startIndex; i < history.size(); i++) {
            ConversationMessage msg = history.get(i);
            if (msg.getMessageType() == ConversationMessage.MessageType.USER) {
                context.append("用户: ").append(msg.getContent()).append("\n");
            } else {
                context.append("AI: ").append(msg.getContent()).append("\n");
                if (msg.getGeneratedSql() != null) {
                    context.append("生成的SQL: ").append(msg.getGeneratedSql()).append("\n");
                }
            }
        }

        context.append("\n请基于以上上下文回答用户的下一个问题。");
        return context.toString();
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getConversationDetail(String sessionId) {
        Map<String, Object> result = new HashMap<>();

        Conversation conversation = conversationRepository.findBySessionId(sessionId)
                .orElseThrow(() -> new RuntimeException("Conversation not found"));

        List<ConversationMessage> messages = messageRepository.findByConversationIdOrderBySequenceAsc(conversation.getId());

        result.put("sessionId", conversation.getSessionId());
        result.put("dataSourceId", conversation.getDataSourceId());
        result.put("title", conversation.getTitle());
        result.put("provider", conversation.getProvider());
        result.put("createdAt", conversation.getCreatedAt());
        result.put("updatedAt", conversation.getUpdatedAt());
        result.put("messages", messages);

        return result;
    }
}
