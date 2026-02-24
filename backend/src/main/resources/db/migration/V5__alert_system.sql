-- 智能异常检测与告警系统表结构

-- 告警规则表
CREATE TABLE IF NOT EXISTS alert_rule (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL COMMENT '规则名称',
    description TEXT COMMENT '规则描述',
    data_source_id BIGINT NOT NULL COMMENT '数据源ID',
    table_name VARCHAR(100) NOT NULL COMMENT '监控表名',
    column_name VARCHAR(100) COMMENT '监控字段名',
    rule_type VARCHAR(50) NOT NULL COMMENT '规则类型: THRESHOLD/ANOMALY/TREND/MISSING',
    
    -- 阈值规则配置
    operator VARCHAR(20) COMMENT '操作符: >/</>=/<=/==/!=/between',
    threshold_value DECIMAL(20,4) COMMENT '阈值',
    threshold_value2 DECIMAL(20,4) COMMENT '第二阈值(between时使用)',
    
    -- 异常检测配置
    detection_method VARCHAR(50) COMMENT '检测方法: ZSCORE/IQR/MOVING_AVERAGE',
    sensitivity DECIMAL(5,2) DEFAULT 3.00 COMMENT '敏感度(标准差倍数)',
    baseline_period INT DEFAULT 7 COMMENT '基线周期(天)',
    
    -- 执行配置
    check_interval INT DEFAULT 5 COMMENT '检查间隔(分钟)',
    check_sql TEXT COMMENT '自定义检查SQL',
    
    -- 告警配置
    alert_level VARCHAR(20) DEFAULT 'WARNING' COMMENT '告警级别: INFO/WARNING/CRITICAL',
    alert_channels VARCHAR(500) COMMENT '告警渠道: EMAIL,SMS,WECHAT,DINGTALK',
    alert_receivers VARCHAR(1000) COMMENT '告警接收人(逗号分隔)',
    cooldown_minutes INT DEFAULT 30 COMMENT '冷却时间(分钟)',
    
    -- 状态
    is_active BOOLEAN DEFAULT TRUE COMMENT '是否启用',
    last_check_time DATETIME COMMENT '上次检查时间',
    last_alert_time DATETIME COMMENT '上次告警时间',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    INDEX idx_datasource (data_source_id),
    INDEX idx_table (table_name),
    INDEX idx_active (is_active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='告警规则表';

-- 告警记录表
CREATE TABLE IF NOT EXISTS alert_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    rule_id BIGINT NOT NULL COMMENT '规则ID',
    rule_name VARCHAR(100) COMMENT '规则名称(冗余)',
    data_source_id BIGINT COMMENT '数据源ID',
    table_name VARCHAR(100) COMMENT '表名',
    
    alert_level VARCHAR(20) NOT NULL COMMENT '告警级别',
    alert_type VARCHAR(50) COMMENT '告警类型',
    
    -- 告警内容
    title VARCHAR(200) NOT NULL COMMENT '告警标题',
    message TEXT NOT NULL COMMENT '告警消息',
    detail JSON COMMENT '告警详情',
    
    -- 检测数据
    actual_value DECIMAL(20,4) COMMENT '实际值',
    expected_value DECIMAL(20,4) COMMENT '预期值',
    deviation_rate DECIMAL(10,4) COMMENT '偏差率',
    
    -- 状态
    status VARCHAR(20) DEFAULT 'PENDING' COMMENT '状态: PENDING/CONFIRMED/RESOLVED/IGNORED',
    confirmed_by VARCHAR(100) COMMENT '确认人',
    confirmed_at DATETIME COMMENT '确认时间',
    resolved_by VARCHAR(100) COMMENT '解决人',
    resolved_at DATETIME COMMENT '解决时间',
    resolve_note TEXT COMMENT '解决说明',
    
    -- 通知状态
    notify_status VARCHAR(500) COMMENT '通知状态JSON',
    
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    
    INDEX idx_rule (rule_id),
    INDEX idx_status (status),
    INDEX idx_level (alert_level),
    INDEX idx_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='告警记录表';

-- 告警统计表(按天聚合)
CREATE TABLE IF NOT EXISTS alert_statistics (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    stat_date DATE NOT NULL COMMENT '统计日期',
    data_source_id BIGINT COMMENT '数据源ID',
    
    total_alerts INT DEFAULT 0 COMMENT '总告警数',
    critical_count INT DEFAULT 0 COMMENT '严重告警数',
    warning_count INT DEFAULT 0 COMMENT '警告数',
    info_count INT DEFAULT 0 COMMENT '信息数',
    
    resolved_count INT DEFAULT 0 COMMENT '已解决数',
    pending_count INT DEFAULT 0 COMMENT '待处理数',
    
    avg_resolve_time DECIMAL(10,2) COMMENT '平均解决时间(分钟)',
    
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    
    UNIQUE INDEX idx_date_ds (stat_date, data_source_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='告警统计表';

-- 数据基线表(用于异常检测)
CREATE TABLE IF NOT EXISTS data_baseline (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    data_source_id BIGINT NOT NULL COMMENT '数据源ID',
    table_name VARCHAR(100) NOT NULL COMMENT '表名',
    column_name VARCHAR(100) NOT NULL COMMENT '字段名',
    
    baseline_date DATE NOT NULL COMMENT '基线日期',
    
    -- 统计值
    min_value DECIMAL(20,4) COMMENT '最小值',
    max_value DECIMAL(20,4) COMMENT '最大值',
    avg_value DECIMAL(20,4) COMMENT '平均值',
    std_value DECIMAL(20,4) COMMENT '标准差',
    median_value DECIMAL(20,4) COMMENT '中位数',
    
    -- 分布
    q1_value DECIMAL(20,4) COMMENT '第一四分位数',
    q3_value DECIMAL(20,4) COMMENT '第三四分位数',
    
    -- 计数
    total_count BIGINT COMMENT '总数',
    null_count BIGINT COMMENT '空值数',
    distinct_count BIGINT COMMENT '去重数',
    
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    
    UNIQUE INDEX idx_ds_table_col_date (data_source_id, table_name, column_name, baseline_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='数据基线表';
