package com.data.assistant.service.ai;

import lombok.Data;

/**
 * 代码生成结果
 */
@Data
public class CodeResult {
    
    /**
     * 生成的代码
     */
    private String code;
    
    /**
     * 是否成功
     */
    private boolean success;
    
    /**
     * 错误信息
     */
    private String errorMessage;
    
    /**
     * 编程语言
     */
    private String language;
    
    /**
     * 代码解释
     */
    private String explanation;
    
    public static CodeResult success(String code, String language) {
        CodeResult result = new CodeResult();
        result.setSuccess(true);
        result.setCode(code);
        result.setLanguage(language);
        return result;
    }
    
    public static CodeResult failure(String errorMessage) {
        CodeResult result = new CodeResult();
        result.setSuccess(false);
        result.setErrorMessage(errorMessage);
        return result;
    }
}
