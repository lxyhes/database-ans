package com.data.assistant.service;

import com.data.assistant.model.Conversation;
import com.data.assistant.model.ConversationMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class AssistantService {
    
    private static final Logger logger = LoggerFactory.getLogger(AssistantService.class);
    
    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    @Autowired
    private DynamicDataSourceService dynamicDataSourceService;
    
    @Autowired
    private NaturalLanguageProcessor nlProcessor;
    
    @Autowired
    private ConversationService conversationService;
    
    public Map<String, Object> processMessage(String message, Long conversationId, Long dataSourceId) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            if (dataSourceId != null) {
                dynamicDataSourceService.switchDataSource(dataSourceId);
            }
            
            Conversation conversation = null;
            if (conversationId != null) {
                conversation = conversationService.getConversation(String.valueOf(conversationId))
                    .orElse(null);
            }
            
            if (conversation == null) {
                conversation = conversationService.createConversation(dataSourceId, "assistant", "智能助手对话");
            }
            
            conversationService.saveUserMessage(conversation.getSessionId(), message);
            
            String sqlQuery = nlProcessor.parseNaturalLanguageToSQL(message);
            result.put("sqlQuery", sqlQuery);
            
            List<Map<String, Object>> data = executeQuery(sqlQuery);
            result.put("data", data);
            
            String insight = generateInsight(message, data);
            result.put("insight", insight);
            
            Map<String, Object> chart = generateChart(message, data);
            result.put("chart", chart);
            
            String response = generateResponse(message, data, insight);
            result.put("message", response);
            
            result.put("conversationId", conversation.getId());
            result.put("sessionId", conversation.getSessionId());
            
            String queryResultJson = data.isEmpty() ? "[]" : data.toString();
            conversationService.saveAssistantMessage(
                conversation.getSessionId(), 
                response, 
                sqlQuery, 
                queryResultJson
            );
            
            result.put("success", true);
            
        } catch (Exception e) {
            logger.error("处理消息失败: {}", e.getMessage());
            result.put("success", false);
            result.put("message", "抱歉，处理您的问题时出现错误：" + e.getMessage());
        }
        
        return result;
    }
    
    private List<Map<String, Object>> executeQuery(String sql) {
        try {
            return jdbcTemplate.queryForList(sql);
        } catch (Exception e) {
            logger.warn("执行查询失败: {}", e.getMessage());
            return new ArrayList<>();
        }
    }
    
    private String generateInsight(String question, List<Map<String, Object>> data) {
        if (data == null || data.isEmpty()) {
            return "查询未返回数据，请检查查询条件。";
        }
        
        StringBuilder insight = new StringBuilder();
        
        insight.append("查询返回了 ").append(data.size()).append(" 条记录。");
        
        if (data.size() > 0) {
            Map<String, Object> firstRow = data.get(0);
            insight.append(" 包含字段：");
            boolean first = true;
            for (String key : firstRow.keySet()) {
                if (!first) insight.append("、");
                insight.append(key);
                first = false;
            }
            insight.append("。");
        }
        
        if (question.contains("最高") || question.contains("最大")) {
            insight.append(" 已按相关指标排序，首条记录为最大值。");
        } else if (question.contains("最低") || question.contains("最小")) {
            insight.append(" 已按相关指标排序，首条记录为最小值。");
        } else if (question.contains("趋势") || question.contains("变化")) {
            insight.append(" 数据呈现一定趋势，建议查看图表分析。");
        }
        
        return insight.toString();
    }
    
    private Map<String, Object> generateChart(String question, List<Map<String, Object>> data) {
        Map<String, Object> chart = new HashMap<>();
        
        if (data == null || data.isEmpty()) {
            return chart;
        }
        
        Map<String, Object> firstRow = data.get(0);
        List<String> keys = new ArrayList<>(firstRow.keySet());
        
        if (keys.size() < 2) {
            return chart;
        }
        
        String xField = keys.get(0);
        String yField = keys.get(1);
        
        List<String> xData = new ArrayList<>();
        List<Object> yData = new ArrayList<>();
        
        for (Map<String, Object> row : data.subList(0, Math.min(10, data.size()))) {
            xData.add(String.valueOf(row.get(xField)));
            yData.add(row.get(yField));
        }
        
        String chartType = "bar";
        if (question.contains("趋势") || question.contains("变化") || question.contains("时间")) {
            chartType = "line";
        } else if (question.contains("占比") || question.contains("分布") || question.contains("比例")) {
            chartType = "pie";
        }
        
        chart.put("tooltip", Map.of("trigger", "axis"));
        chart.put("legend", Map.of("data", Arrays.asList(yField)));
        chart.put("xAxis", Map.of(
            "type", "category",
            "data", xData
        ));
        chart.put("yAxis", Map.of("type", "value"));
        
        Map<String, Object> series = new HashMap<>();
        series.put("name", yField);
        series.put("type", chartType);
        series.put("data", yData);
        series.put("itemStyle", Map.of("color", "#165dff"));
        
        chart.put("series", Arrays.asList(series));
        
        return chart;
    }
    
    private String generateResponse(String question, List<Map<String, Object>> data, String insight) {
        StringBuilder response = new StringBuilder();
        
        if (data == null || data.isEmpty()) {
            return "查询未返回数据。请检查查询条件或尝试其他问题。";
        }
        
        response.append("根据您的问题\"").append(question).append("\"，我为您查询了数据。\n\n");
        response.append(insight).append("\n\n");
        
        if (data.size() > 0) {
            response.append("以下是前几条数据：\n");
            int count = Math.min(3, data.size());
            for (int i = 0; i < count; i++) {
                response.append(i + 1).append(". ");
                Map<String, Object> row = data.get(i);
                boolean first = true;
                for (Map.Entry<String, Object> entry : row.entrySet()) {
                    if (!first) response.append(", ");
                    response.append(entry.getKey()).append(": ").append(entry.getValue());
                    first = false;
                }
                response.append("\n");
            }
        }
        
        return response.toString();
    }
    
    public List<Map<String, Object>> getConversations() {
        List<Map<String, Object>> result = new ArrayList<>();
        try {
            List<Conversation> conversations = conversationService.getActiveConversations(null);
            for (Conversation conv : conversations) {
                Map<String, Object> convMap = new HashMap<>();
                convMap.put("id", conv.getId());
                convMap.put("sessionId", conv.getSessionId());
                convMap.put("title", conv.getTitle());
                convMap.put("dataSourceId", conv.getDataSourceId());
                convMap.put("provider", conv.getProvider());
                convMap.put("createdAt", conv.getCreatedAt());
                convMap.put("updatedAt", conv.getUpdatedAt());
                result.add(convMap);
            }
        } catch (Exception e) {
            logger.error("获取对话列表失败: {}", e.getMessage());
        }
        return result;
    }
    
    public List<Map<String, Object>> getConversationMessages(Long conversationId) {
        List<Map<String, Object>> result = new ArrayList<>();
        try {
            if (conversationId == null) {
                return result;
            }
            List<ConversationMessage> messages = conversationService.getConversationHistory(String.valueOf(conversationId));
            for (ConversationMessage msg : messages) {
                Map<String, Object> msgMap = new HashMap<>();
                msgMap.put("id", msg.getId());
                msgMap.put("type", msg.getMessageType().name());
                msgMap.put("content", msg.getContent());
                msgMap.put("generatedSql", msg.getGeneratedSql());
                msgMap.put("queryResult", msg.getQueryResult());
                msgMap.put("sequence", msg.getSequence());
                msgMap.put("createdAt", msg.getCreatedAt());
                result.add(msgMap);
            }
        } catch (Exception e) {
            logger.error("获取对话消息失败: {}", e.getMessage());
        }
        return result;
    }
    
    public void deleteConversation(Long conversationId) {
        try {
            if (conversationId != null) {
                conversationService.deactivateConversation(String.valueOf(conversationId));
            }
        } catch (Exception e) {
            logger.error("删除对话失败: {}", e.getMessage());
        }
    }
}
