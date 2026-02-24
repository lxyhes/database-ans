-- 数据分析助手系统 - 基础表结构初始化

-- 数据源表
CREATE TABLE IF NOT EXISTS data_source (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL COMMENT '数据源名称',
    type VARCHAR(50) NOT NULL COMMENT '数据源类型: MYSQL/POSTGRESQL/ORACLE/SQLSERVER',
    host VARCHAR(255) NOT NULL COMMENT '主机地址',
    port INT NOT NULL COMMENT '端口',
    database_name VARCHAR(100) COMMENT '数据库名',
    username VARCHAR(100) COMMENT '用户名',
    password VARCHAR(255) COMMENT '密码(加密)',
    connection_params TEXT COMMENT '额外连接参数',
    is_active BOOLEAN DEFAULT TRUE COMMENT '是否启用',
    last_connected DATETIME COMMENT '最后连接时间',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    INDEX idx_type (type),
    INDEX idx_active (is_active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='数据源配置表';

-- 查询历史表
CREATE TABLE IF NOT EXISTS query_history (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    data_source_id BIGINT COMMENT '数据源ID',
    query_type VARCHAR(50) COMMENT '查询类型',
    query_text TEXT NOT NULL COMMENT '查询语句',
    execution_time BIGINT COMMENT '执行时间(毫秒)',
    row_count INT COMMENT '返回行数',
    status VARCHAR(20) COMMENT '状态: SUCCESS/FAILED',
    error_message TEXT COMMENT '错误信息',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    
    INDEX idx_datasource (data_source_id),
    INDEX idx_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='查询历史表';

-- 对话表
CREATE TABLE IF NOT EXISTS conversation (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    session_id VARCHAR(100) NOT NULL UNIQUE COMMENT '会话ID',
    data_source_id BIGINT COMMENT '数据源ID',
    provider VARCHAR(50) COMMENT 'AI提供商',
    title VARCHAR(200) COMMENT '对话标题',
    is_active BOOLEAN DEFAULT TRUE COMMENT '是否活跃',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    INDEX idx_session (session_id),
    INDEX idx_datasource (data_source_id),
    INDEX idx_active (is_active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI对话表';

-- 对话消息表
CREATE TABLE IF NOT EXISTS conversation_message (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    conversation_id BIGINT NOT NULL COMMENT '对话ID',
    message_type VARCHAR(20) NOT NULL COMMENT '消息类型: USER/ASSISTANT',
    content TEXT NOT NULL COMMENT '消息内容',
    generated_sql TEXT COMMENT '生成的SQL',
    query_result TEXT COMMENT '查询结果',
    sequence INT COMMENT '消息序号',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    
    INDEX idx_conversation (conversation_id),
    INDEX idx_sequence (sequence)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='对话消息表';

-- 指标表
CREATE TABLE IF NOT EXISTS metrics (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL COMMENT '指标名称',
    code VARCHAR(100) NOT NULL UNIQUE COMMENT '指标编码',
    description TEXT COMMENT '指标描述',
    category VARCHAR(50) COMMENT '指标分类',
    data_source_id BIGINT COMMENT '数据源ID',
    table_name VARCHAR(100) COMMENT '表名',
    column_name VARCHAR(100) COMMENT '字段名',
    aggregation_type VARCHAR(20) COMMENT '聚合类型: SUM/COUNT/AVG/MAX/MIN/DISTINCT_COUNT',
    filter_condition VARCHAR(500) COMMENT '过滤条件',
    unit VARCHAR(20) COMMENT '单位',
    is_active BOOLEAN DEFAULT TRUE COMMENT '是否启用',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    INDEX idx_code (code),
    INDEX idx_category (category),
    INDEX idx_datasource (data_source_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='指标定义表';

-- 数据血缘关系表
CREATE TABLE IF NOT EXISTS data_lineage (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    source_table VARCHAR(200) NOT NULL COMMENT '源表',
    source_column VARCHAR(200) COMMENT '源字段',
    target_table VARCHAR(200) NOT NULL COMMENT '目标表',
    target_column VARCHAR(200) COMMENT '目标字段',
    transformation TEXT COMMENT '转换逻辑',
    lineage_type VARCHAR(50) COMMENT '血缘类型',
    data_source_id BIGINT COMMENT '数据源ID',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    
    INDEX idx_source (source_table),
    INDEX idx_target (target_table),
    INDEX idx_datasource (data_source_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='数据血缘关系表';

-- 表关系表
CREATE TABLE IF NOT EXISTS table_relation (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    data_source_id BIGINT NOT NULL COMMENT '数据源ID',
    source_table VARCHAR(200) NOT NULL COMMENT '源表',
    target_table VARCHAR(200) NOT NULL COMMENT '目标表',
    relation_type VARCHAR(50) COMMENT '关系类型',
    join_condition TEXT COMMENT 'JOIN条件',
    confidence DECIMAL(5,2) COMMENT '置信度',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    
    INDEX idx_datasource (data_source_id),
    INDEX idx_source (source_table),
    INDEX idx_target (target_table)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='表关系表';

-- 敏感数据类型表
CREATE TABLE IF NOT EXISTS sensitive_data_type (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL COMMENT '类型名称',
    pattern VARCHAR(500) COMMENT '匹配模式(正则)',
    description TEXT COMMENT '描述',
    risk_level VARCHAR(20) COMMENT '风险等级: LOW/MEDIUM/HIGH/CRITICAL',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    
    UNIQUE INDEX idx_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='敏感数据类型表';

-- 敏感字段表
CREATE TABLE IF NOT EXISTS sensitive_column (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    data_source_id BIGINT NOT NULL COMMENT '数据源ID',
    table_name VARCHAR(200) NOT NULL COMMENT '表名',
    column_name VARCHAR(200) NOT NULL COMMENT '字段名',
    sensitive_type_id BIGINT COMMENT '敏感类型ID',
    is_masked BOOLEAN DEFAULT FALSE COMMENT '是否已脱敏',
    mask_strategy VARCHAR(50) COMMENT '脱敏策略',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    
    INDEX idx_datasource (data_source_id),
    INDEX idx_table (table_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='敏感字段表';

-- 数据质量报告表
CREATE TABLE IF NOT EXISTS data_quality_report (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    data_source_id BIGINT NOT NULL COMMENT '数据源ID',
    table_name VARCHAR(200) NOT NULL COMMENT '表名',
    total_rows BIGINT COMMENT '总行数',
    null_count BIGINT COMMENT '空值数',
    duplicate_count BIGINT COMMENT '重复数',
    quality_score DECIMAL(5,2) COMMENT '质量分数',
    issues TEXT COMMENT '问题列表JSON',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    
    INDEX idx_datasource (data_source_id),
    INDEX idx_table (table_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='数据质量报告表';

-- 报告模板表
CREATE TABLE IF NOT EXISTS report_template (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL COMMENT '模板名称',
    description TEXT COMMENT '模板描述',
    config TEXT COMMENT '模板配置JSON',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='报告模板表';

-- 报告实例表
CREATE TABLE IF NOT EXISTS report_instance (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    data_source_id BIGINT COMMENT '数据源ID',
    title VARCHAR(200) NOT NULL COMMENT '报告标题',
    table_names VARCHAR(500) COMMENT '表名列表',
    report_data TEXT COMMENT '报告数据JSON',
    health_score INT COMMENT '健康分数',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    
    INDEX idx_datasource (data_source_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='报告实例表';

-- 插入默认敏感数据类型
INSERT IGNORE INTO sensitive_data_type (name, pattern, description, risk_level) VALUES
('身份证号', '\\d{17}[\\dXx]', '中国居民身份证号码', 'HIGH'),
('手机号码', '1[3-9]\\d{9}', '中国大陆手机号码', 'MEDIUM'),
('银行卡号', '\\d{16,19}', '银行卡卡号', 'CRITICAL'),
('邮箱地址', '[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}', '电子邮箱地址', 'LOW'),
('统一社会信用代码', '[0-9A-HJ-NPQRTUWXY]{2}\\d{6}[0-9A-HJ-NPQRTUWXY]{10}', '企业统一社会信用代码', 'HIGH');
