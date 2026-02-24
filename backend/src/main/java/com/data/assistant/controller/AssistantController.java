package com.data.assistant.controller;

import com.data.assistant.common.ApiResponse;
import com.data.assistant.service.AssistantService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/assistant")
@CrossOrigin(origins = "*")
public class AssistantController {
    
    @Autowired
    private AssistantService assistantService;
    
    @PostMapping("/chat")
    public ResponseEntity<Map<String, Object>> chat(@RequestBody Map<String, Object> params) {
        String message = (String) params.get("message");
        Long conversationId = params.get("conversationId") != null ? 
            Long.valueOf(params.get("conversationId").toString()) : null;
        Long dataSourceId = params.get("dataSourceId") != null ? 
            Long.valueOf(params.get("dataSourceId").toString()) : null;
        
        Map<String, Object> result = assistantService.processMessage(message, conversationId, dataSourceId);
        return ResponseEntity.ok(ApiResponse.success(result));
    }
    
    @GetMapping("/conversations")
    public ResponseEntity<Map<String, Object>> getConversations() {
        return ResponseEntity.ok(ApiResponse.success(assistantService.getConversations()));
    }
    
    @GetMapping("/conversations/{id}/messages")
    public ResponseEntity<Map<String, Object>> getConversationMessages(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(assistantService.getConversationMessages(id)));
    }
    
    @DeleteMapping("/conversations/{id}")
    public ResponseEntity<Map<String, Object>> deleteConversation(@PathVariable Long id) {
        assistantService.deleteConversation(id);
        return ResponseEntity.ok(ApiResponse.success("删除成功"));
    }
}
