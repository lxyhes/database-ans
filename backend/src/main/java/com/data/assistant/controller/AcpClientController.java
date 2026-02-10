package com.data.assistant.controller;

import com.data.assistant.model.QueryRequest;
import com.data.assistant.model.QueryResponse;
import com.data.assistant.service.AcpClientService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * ACP Client SDK 控制器
 * 允许通过 ACP 协议与 AI 代理交互
 */
@RestController
@RequestMapping("/api/acp")
@CrossOrigin(origins = "*")
public class AcpClientController {

    @Autowired
    private AcpClientService acpClientService;

    /**
     * 发送提示词到 ACP 代理
     */
    @PostMapping("/query")
    public QueryResponse query(@RequestBody QueryRequest request) {
        String result = acpClientService.sendPrompt(request.getNaturalLanguageQuery());
        return new QueryResponse(true, result);
    }
}
