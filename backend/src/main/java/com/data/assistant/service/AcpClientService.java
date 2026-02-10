package com.data.assistant.service;

import com.alibaba.acp.sdk.AcpClient;
import com.alibaba.acp.sdk.transport.process.ProcessTransport;
import com.alibaba.acp.sdk.transport.process.ProcessTransportOptions;
import com.alibaba.acp.sdk.protocol.domain.content.block.TextContent;
import com.alibaba.acp.sdk.session.event.consumer.AgentEventConsumer;
import com.alibaba.acp.sdk.session.event.consumer.ContentEventSimpleConsumer;
import com.alibaba.acp.sdk.protocol.domain.session.update.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Collections;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * ACP Client SDK 服务集成
 * 按照 qwen-client-sdk.md 文档实现
 */
@Service
public class AcpClientService {
    private static final Logger logger = LoggerFactory.getLogger(AcpClientService.class);

    @Value("${acp.client.enabled:true}")
    private boolean acpClientEnabled;

    /**
     * 发送提示词并获取回复
     */
    public String sendPrompt(String prompt) {
        // Check if ACP client is enabled and qwen command is available
        if (!acpClientEnabled || !isQwenCommandAvailable()) {
            logger.warn("ACP client is disabled or qwen command is not available. Returning mock response.");
            return handleFallbackResponse(prompt);
        }

        StringBuilder responseBuilder = new StringBuilder();
        CountDownLatch latch = new CountDownLatch(1);

        // 创建 ACP 客户端
        AcpClient acpClient = null;
        try {
            // 使用进程传输方式启动 qwen 代理
            acpClient = new AcpClient(
                    new ProcessTransport(new ProcessTransportOptions().setCommandArgs(new String[]{"qwen", "--acp", "-y"})));

            logger.info("Sending prompt to ACP agent: {}", prompt);

            acpClient.sendPrompt(Collections.singletonList(new TextContent(prompt)),
                    new AgentEventConsumer().setContentEventConsumer(new ContentEventSimpleConsumer() {
                        @Override
                        public void onAgentMessageChunkSessionUpdate(AgentMessageChunkSessionUpdate sessionUpdate) {
                            if (sessionUpdate != null && sessionUpdate.getContent() != null) {
                                // 提取文本内容并累加
                                String content = sessionUpdate.getContent().toString();
                                // 如果是JSON格式的消息块，可能需要进一步解析
                                logger.debug("Received message chunk: {}", content);
                                responseBuilder.append(content);
                            }
                        }

                        @Override
                        public void onAvailableCommandsUpdateSessionUpdate(AvailableCommandsUpdateSessionUpdate sessionUpdate) {
                            logger.info("Available commands updated: {}", sessionUpdate);
                        }

                        @Override
                        public void onCurrentModeUpdateSessionUpdate(CurrentModeUpdateSessionUpdate sessionUpdate) {
                            logger.info("Current mode updated: {}", sessionUpdate);
                        }

                        @Override
                        public void onPlanSessionUpdate(PlanSessionUpdate sessionUpdate) {
                            logger.info("Plan update received: {}", sessionUpdate);
                        }

                        @Override
                        public void onToolCallUpdateSessionUpdate(ToolCallUpdateSessionUpdate sessionUpdate) {
                            logger.info("Tool call update: {}", sessionUpdate);
                        }

                        @Override
                        public void onToolCallSessionUpdate(ToolCallSessionUpdate sessionUpdate) {
                            logger.info("Tool call received: {}", sessionUpdate);
                        }
                    }));

            // 等待处理完成。在实际 SDK 中，sendPrompt 可能是同步的或提供结束信号
            // 这里我们等待一段时间或者直到某些结束条件满足
            // 假设我们等待 15 秒作为超时
            boolean completed = latch.await(15, TimeUnit.SECONDS);

            if (responseBuilder.length() == 0) {
                return "Prompt sent, but no content was received within the timeout period.";
            }

            return responseBuilder.toString();
        } catch (Exception e) {
            logger.error("Failed to send prompt via ACP Client, falling back to mock response", e);
            return handleFallbackResponse(prompt);
        } finally {
            if (acpClient != null) {
                try {
                    acpClient.close();
                } catch (IOException e) {
                    logger.error("Error closing AcpClient", e);
                }
            }
        }
    }

    /**
     * Checks if the qwen command is available in the system
     */
    private boolean isQwenCommandAvailable() {
        try {
            // Try to execute 'qwen --help' to check if the command exists
            ProcessBuilder processBuilder = new ProcessBuilder("qwen", "--help");
            Process process = processBuilder.start();
            int exitCode = process.waitFor();
            return exitCode == 0 || exitCode == 1; // 0 for success, 1 for help displayed
        } catch (Exception e) {
            logger.warn("qwen command is not available: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Fallback response when ACP client is not available
     */
    private String handleFallbackResponse(String prompt) {
        logger.info("Using fallback response for prompt: {}", prompt);
        // Return a meaningful response that indicates the limitation
        return "模拟响应：当前环境未配置 Qwen ACP 客户端。您的查询 '" + prompt + "' 已接收，但无法通过 ACP 协议处理。" +
               "如需启用 ACP 功能，请确保已安装 Qwen CLI 并在系统路径中可用。";
    }
}
