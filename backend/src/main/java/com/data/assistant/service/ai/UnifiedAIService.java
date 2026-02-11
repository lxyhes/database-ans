package com.data.assistant.service.ai;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * 统一 AI 服务入口
 * 封装所有 AI 功能，对外提供统一的调用接口
 */
@Service
public class UnifiedAIService {
    
    private static final Logger logger = LoggerFactory.getLogger(UnifiedAIService.class);
    
    @Autowired
    private AIServiceFactory aiServiceFactory;
    
    /**
     * 自然语言转 SQL
     * @param provider AI 提供商（iflow, qwen, openai 等）
     * @param naturalLanguage 自然语言查询
     * @param schema 数据库 schema
     * @param context 上下文
     * @return NL2SQL 结果
     */
    public NL2SQLResult naturalLanguageToSQL(String provider, String naturalLanguage, String schema, Map<String, Object> context) {
        try {
            AIService service = getServiceOrDefault(provider);
            return service.naturalLanguageToSQL(naturalLanguage, schema, context);
        } catch (Exception e) {
            logger.error("NL2SQL failed for provider: {}", provider, e);
            return NL2SQLResult.failure("NL2SQL 服务异常: " + e.getMessage());
        }
    }
    
    /**
     * 数据分析
     */
    public AnalysisResult analyzeData(String provider, String data, String question) {
        try {
            AIService service = getServiceOrDefault(provider);
            return service.analyzeData(data, question);
        } catch (Exception e) {
            logger.error("Data analysis failed for provider: {}", provider, e);
            return AnalysisResult.failure("数据分析服务异常: " + e.getMessage());
        }
    }
    
    /**
     * 生成代码
     */
    public CodeResult generateCode(String provider, String description, String language) {
        try {
            AIService service = getServiceOrDefault(provider);
            return service.generateCode(description, language);
        } catch (Exception e) {
            logger.error("Code generation failed for provider: {}", provider, e);
            return CodeResult.failure("代码生成服务异常: " + e.getMessage());
        }
    }
    
    /**
     * 解释代码
     */
    public String explainCode(String provider, String code) {
        try {
            AIService service = getServiceOrDefault(provider);
            return service.explainCode(code);
        } catch (Exception e) {
            logger.error("Code explanation failed for provider: {}", provider, e);
            return "代码解释服务异常: " + e.getMessage();
        }
    }
    
    /**
     * 通用对话
     */
    public String chat(String provider, String prompt, Map<String, Object> context) {
        try {
            AIService service = getServiceOrDefault(provider);
            return service.chat(prompt, context);
        } catch (Exception e) {
            logger.error("Chat failed for provider: {}", provider, e);
            return "对话服务异常: " + e.getMessage();
        }
    }
    
    /**
     * 智能查询（自动选择最佳服务）
     */
    public NL2SQLResult smartQuery(String naturalLanguage, String schema, Map<String, Object> context) {
        // 优先使用 iFlow
        if (aiServiceFactory.isProviderAvailable("iflow")) {
            return naturalLanguageToSQL("iflow", naturalLanguage, schema, context);
        }
        
        // 回退到默认服务
        return naturalLanguageToSQL(null, naturalLanguage, schema, context);
    }
    
    /**
     * 获取服务或默认服务
     */
    private AIService getServiceOrDefault(String provider) {
        if (provider == null || provider.isEmpty()) {
            return aiServiceFactory.getDefaultService();
        }
        return aiServiceFactory.getService(provider);
    }
}
