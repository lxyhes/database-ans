-- 数据分析助手数据库表结构
-- 适用于 MySQL 8.0+

CREATE DATABASE IF NOT EXISTS analysis_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE analysis_db;

-- 数据源表
CREATE TABLE IF NOT EXISTS data_sources (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL COMMENT '数据源名称',
    type VARCHAR(50) NOT NULL COMMENT '数据源类型(mysql, postgresql, h2等)',
    host VARCHAR(255) NOT NULL COMMENT '主机地址',
    port INT NOT NULL COMMENT '端口',
    db_name VARCHAR(255) NOT NULL COMMENT '数据库名称',
    username VARCHAR(255) NOT NULL COMMENT '用户名',
    password VARCHAR(255) NOT NULL COMMENT '密码(加密存储)',
    connection_params TEXT COMMENT '连接参数(JSON格式)',
    description TEXT COMMENT '描述',
    active BOOLEAN DEFAULT TRUE COMMENT '是否激活',
    is_default BOOLEAN DEFAULT FALSE COMMENT '是否默认数据源',
    last_connected_at DATETIME COMMENT '最后连接时间',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='数据源配置表';

-- 表关系表
CREATE TABLE IF NOT EXISTS table_relations (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    data_source_id BIGINT NOT NULL COMMENT '数据源ID',
    source_table VARCHAR(255) NOT NULL COMMENT '源表名',
    source_column VARCHAR(255) NOT NULL COMMENT '源列名',
    target_table VARCHAR(255) NOT NULL COMMENT '目标表名',
    target_column VARCHAR(255) NOT NULL COMMENT '目标列名',
    relation_type VARCHAR(50) COMMENT '关系类型(FOREIGN_KEY, INFERRED, MANUAL)',
    confidence DOUBLE COMMENT '置信度',
    description TEXT COMMENT '描述',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_data_source_id (data_source_id),
    INDEX idx_source_table (source_table),
    INDEX idx_target_table (target_table)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='表关系表';

-- 查询历史表
CREATE TABLE IF NOT EXISTS query_history (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    query_text VARCHAR(1000) NOT NULL COMMENT '查询文本',
    sql_query TEXT COMMENT '生成的SQL',
    query_type VARCHAR(50) COMMENT '查询类型',
    is_favorite BOOLEAN DEFAULT FALSE COMMENT '是否收藏',
    tags VARCHAR(200) COMMENT '标签',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='查询历史表';

-- 对话表
CREATE TABLE IF NOT EXISTS conversations (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(255) COMMENT '对话标题',
    data_source_id BIGINT COMMENT '关联的数据源ID',
    status VARCHAR(50) DEFAULT 'active' COMMENT '状态',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_data_source_id (data_source_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='对话表';

-- 对话消息表
CREATE TABLE IF NOT EXISTS conversation_messages (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    conversation_id BIGINT NOT NULL COMMENT '对话ID',
    role VARCHAR(50) NOT NULL COMMENT '角色(user, assistant)',
    content TEXT NOT NULL COMMENT '消息内容',
    sql_query TEXT COMMENT '关联的SQL',
    query_result TEXT COMMENT '查询结果',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_conversation_id (conversation_id),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='对话消息表';

-- 敏感数据类型表
CREATE TABLE IF NOT EXISTS sensitive_data_types (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL COMMENT '类型名称',
    description TEXT COMMENT '描述',
    pattern VARCHAR(500) COMMENT '匹配模式(正则)',
    category VARCHAR(100) COMMENT '分类',
    risk_level VARCHAR(50) COMMENT '风险等级(LOW, MEDIUM, HIGH)',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE KEY uk_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='敏感数据类型表';

-- 敏感列表
CREATE TABLE IF NOT EXISTS sensitive_columns (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    data_source_id BIGINT NOT NULL COMMENT '数据源ID',
    table_name VARCHAR(255) NOT NULL COMMENT '表名',
    column_name VARCHAR(255) NOT NULL COMMENT '列名',
    data_type VARCHAR(100) COMMENT '数据类型',
    sensitive_type_id BIGINT COMMENT '敏感类型ID',
    confidence DOUBLE COMMENT '置信度',
    status VARCHAR(50) DEFAULT 'active' COMMENT '状态',
    notes TEXT COMMENT '备注',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_data_source_id (data_source_id),
    INDEX idx_table_name (table_name),
    INDEX idx_sensitive_type_id (sensitive_type_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='敏感列表';

-- 数据血缘表
CREATE TABLE IF NOT EXISTS data_lineage (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    source_data_source_id BIGINT COMMENT '源数据源ID',
    source_table VARCHAR(255) COMMENT '源表',
    source_column VARCHAR(255) COMMENT '源列',
    target_data_source_id BIGINT COMMENT '目标数据源ID',
    target_table VARCHAR(255) COMMENT '目标表',
    target_column VARCHAR(255) COMMENT '目标列',
    transformation_logic TEXT COMMENT '转换逻辑',
    lineage_type VARCHAR(50) COMMENT '血缘类型(DIRECT, TRANSFORMED, AGGREGATED)',
    confidence DOUBLE COMMENT '置信度',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_source (source_data_source_id, source_table),
    INDEX idx_target (target_data_source_id, target_table)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='数据血缘表';

-- 数据质量报告表
CREATE TABLE IF NOT EXISTS data_quality_reports (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    data_source_id BIGINT NOT NULL COMMENT '数据源ID',
    table_name VARCHAR(255) COMMENT '表名',
    column_name VARCHAR(255) COMMENT '列名',
    check_type VARCHAR(100) NOT NULL COMMENT '检查类型',
    check_result TEXT COMMENT '检查结果',
    score DOUBLE COMMENT '评分',
    status VARCHAR(50) COMMENT '状态(PASS, FAIL, WARNING)',
    details TEXT COMMENT '详细数据',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_data_source_id (data_source_id),
    INDEX idx_table_name (table_name),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='数据质量报告表';

-- 报表模板表
CREATE TABLE IF NOT EXISTS report_templates (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL COMMENT '模板名称',
    description TEXT COMMENT '描述',
    type VARCHAR(100) COMMENT '报表类型',
    config_json TEXT COMMENT '配置(JSON)',
    query_sql TEXT COMMENT '查询SQL',
    chart_type VARCHAR(50) COMMENT '图表类型',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='报表模板表';

-- 报表实例表
CREATE TABLE IF NOT EXISTS report_instances (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    template_id BIGINT NOT NULL COMMENT '模板ID',
    name VARCHAR(255) NOT NULL COMMENT '实例名称',
    parameters TEXT COMMENT '参数(JSON)',
    status VARCHAR(50) DEFAULT 'draft' COMMENT '状态',
    result_data TEXT COMMENT '结果数据',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_template_id (template_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='报表实例表';

-- 指标表
CREATE TABLE IF NOT EXISTS metrics (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL COMMENT '指标名称',
    description TEXT COMMENT '描述',
    category VARCHAR(100) COMMENT '分类',
    data_source_id BIGINT COMMENT '数据源ID',
    table_name VARCHAR(255) COMMENT '表名',
    calculation_logic TEXT COMMENT '计算逻辑',
    unit VARCHAR(50) COMMENT '单位',
    threshold_min DOUBLE COMMENT '阈值下限',
    threshold_max DOUBLE COMMENT '阈值上限',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_name (name),
    INDEX idx_category (category)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='指标表';

-- 插入默认敏感数据类型
INSERT INTO sensitive_data_types (name, description, pattern, category, risk_level) VALUES
('手机号', '中国大陆手机号码', '^1[3-9]\\d{9}$', '个人信息', 'HIGH'),
('身份证号', '中国大陆身份证号码', '^\\d{17}[\\dXx]$', '个人身份', 'HIGH'),
('邮箱', '电子邮箱地址', '^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$', '联系方式', 'MEDIUM'),
('银行卡号', '银行卡号码', '^\\d{16,19}$', '金融信息', 'HIGH'),
('姓名', '中文姓名', '^[\\u4e00-\\u9fa5]{2,8}$', '个人信息', 'LOW'),
('地址', '地址信息', '.*省.*市.*区.*', '位置信息', 'MEDIUM');

-- 添加外键约束(可选，根据需求决定是否启用)
-- ALTER TABLE table_relations ADD CONSTRAINT fk_tr_data_source FOREIGN KEY (data_source_id) REFERENCES data_sources(id);
-- ALTER TABLE conversation_messages ADD CONSTRAINT fk_msg_conversation FOREIGN KEY (conversation_id) REFERENCES conversations(id);
-- ALTER TABLE sensitive_columns ADD CONSTRAINT fk_sc_type FOREIGN KEY (sensitive_type_id) REFERENCES sensitive_data_types(id);
-- ALTER TABLE report_instances ADD CONSTRAINT fk_ri_template FOREIGN KEY (template_id) REFERENCES report_templates(id);
