package com.data.assistant.service.ai;

import lombok.Data;

/**
 * 数据分析结果
 */
@Data
public class AnalysisResult {
    
    /**
     * 分析内容
     */
    private String analysis;
    
    /**
     * 是否成功
     */
    private boolean success;
    
    /**
     * 错误信息
     */
    private String errorMessage;
    
    /**
     * 发现的异常点
     */
    private String anomalies;
    
    /**
     * 趋势分析
     */
    private String trends;
    
    /**
     * 建议
     */
    private String recommendations;
    
    public static AnalysisResult success(String analysis) {
        AnalysisResult result = new AnalysisResult();
        result.setSuccess(true);
        result.setAnalysis(analysis);
        return result;
    }
    
    public static AnalysisResult failure(String errorMessage) {
        AnalysisResult result = new AnalysisResult();
        result.setSuccess(false);
        result.setErrorMessage(errorMessage);
        return result;
    }
}
