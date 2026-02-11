package com.data.assistant.controller;

import com.data.assistant.common.ApiResponse;
import com.data.assistant.model.Conversation;
import com.data.assistant.model.ConversationMessage;
import com.data.assistant.service.ConversationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/conversations")
@CrossOrigin(origins = "*")
public class ConversationController {

    @Autowired
    private ConversationService conversationService;

    @PostMapping
    public ResponseEntity<?> createConversation(@RequestBody Map<String, Object> request) {
        Long dataSourceId = Long.valueOf(request.get("dataSourceId").toString());
        String provider = (String) request.getOrDefault("provider", "mock");
        String title = (String) request.get("title");

        Conversation conversation = conversationService.createConversation(dataSourceId, provider, title);
        return ResponseEntity.ok(ApiResponse.success(conversation));
    }

    @GetMapping
    public ResponseEntity<?> getConversations(@RequestParam(required = false) Long dataSourceId) {
        List<Conversation> conversations = conversationService.getActiveConversations(dataSourceId);
        return ResponseEntity.ok(ApiResponse.success(conversations));
    }

    @GetMapping("/{sessionId}")
    public ResponseEntity<?> getConversation(@PathVariable String sessionId) {
        Map<String, Object> conversation = conversationService.getConversationDetail(sessionId);
        return ResponseEntity.ok(ApiResponse.success(conversation));
    }

    @GetMapping("/{sessionId}/messages")
    public ResponseEntity<?> getConversationMessages(@PathVariable String sessionId) {
        List<ConversationMessage> messages = conversationService.getConversationHistory(sessionId);
        return ResponseEntity.ok(ApiResponse.success(messages));
    }

    @PostMapping("/{sessionId}/messages")
    public ResponseEntity<?> addMessage(@PathVariable String sessionId, @RequestBody Map<String, Object> request) {
        String content = (String) request.get("content");
        conversationService.saveUserMessage(sessionId, content);
        return ResponseEntity.ok(ApiResponse.success());
    }

    @DeleteMapping("/{sessionId}")
    public ResponseEntity<?> deleteConversation(@PathVariable String sessionId) {
        conversationService.deactivateConversation(sessionId);
        return ResponseEntity.ok(ApiResponse.success());
    }
}
