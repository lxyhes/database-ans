package com.data.assistant.model;

public enum SensitiveDataType {
    PHONE("手机号", "^1[3-9]\\d{9}$", "138****8888"),
    ID_CARD("身份证号", "^\\d{17}[\\dXx]$", "110101********1234"),
    BANK_CARD("银行卡号", "^\\d{16,19}$", "6222************1234"),
    EMAIL("邮箱", "^[\\w.-]+@[\\w.-]+\\.\\w+$", "a***@example.com"),
    NAME("姓名", null, "*某某"),
    ADDRESS("地址", null, "北京市****"),
    PASSWORD("密码", null, "********");

    private final String displayName;
    private final String pattern;
    private final String maskExample;

    SensitiveDataType(String displayName, String pattern, String maskExample) {
        this.displayName = displayName;
        this.pattern = pattern;
        this.maskExample = maskExample;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getPattern() {
        return pattern;
    }

    public String getMaskExample() {
        return maskExample;
    }
}
