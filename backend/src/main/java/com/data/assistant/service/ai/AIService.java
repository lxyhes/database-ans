package com.data.assistant.service.ai;

import java.util.Map;

/**
 * 统一 AI 服务接口
 * 支持多种 AI 提供商：iFlow、Qwen、OpenAI 等
 */
public interface AIService {
    
    /**
     * 自然语言转 SQL
     * @param naturalLanguage 自然语言查询
     * @param schema 数据库 schema 信息
     * @param context 上下文信息（多轮对话历史等）
     * @return SQL 生成结果
     */
    NL2SQLResult naturalLanguageToSQL(String naturalLanguage, String schema, Map<String, Object> context);
    
    /**
     * 数据分析
     * @param data 数据内容
     * @param question 分析问题
     * @return 分析结果
     */
    AnalysisResult analyzeData(String data, String question);
    
    /**
     * 生成代码
     * @param description 代码描述
     * @param language 编程语言
     * @return 生成的代码
     */
    CodeResult generateCode(String description, String language);
    
    /**
     * 解释代码
     * @param code 代码内容
     * @return 代码解释
     */
    String explainCode(String code);
    
    /**
     * 通用对话查询
     * @param prompt 提示词
     * @param context 上下文
     * @return AI 响应
     */
    String chat(String prompt, Map<String, Object> context);
    
    /**
     * 获取服务提供商名称
     */
    String getProviderName();
    
    /**
     * 检查服务是否可用
     */
    boolean isAvailable();
}
