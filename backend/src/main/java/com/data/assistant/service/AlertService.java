package com.data.assistant.service;

import com.data.assistant.model.AlertRule;
import com.data.assistant.model.AlertRecord;
import com.data.assistant.repository.AlertRuleRepository;
import com.data.assistant.repository.AlertRecordRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class AlertService {
    
    private static final Logger logger = LoggerFactory.getLogger(AlertService.class);
    
    @Autowired
    private AlertRuleRepository ruleRepository;
    
    @Autowired
    private AlertRecordRepository recordRepository;
    
    @Autowired
    private JdbcTemplate jdbcTemplate;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Autowired
    private DynamicDataSourceService dynamicDataSourceService;
    
    public List<AlertRule> getAllRules() {
        return ruleRepository.findAll();
    }
    
    public List<AlertRule> getActiveRules() {
        return ruleRepository.findByIsActiveTrue();
    }
    
    public Optional<AlertRule> getRuleById(Long id) {
        return ruleRepository.findById(id);
    }
    
    @Transactional
    public AlertRule createRule(AlertRule rule) {
        validateRule(rule);
        return ruleRepository.save(rule);
    }
    
    @Transactional
    public AlertRule updateRule(Long id, AlertRule rule) {
        AlertRule existing = ruleRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("规则不存在: " + id));
        
        rule.setId(id);
        rule.setCreatedAt(existing.getCreatedAt());
        validateRule(rule);
        return ruleRepository.save(rule);
    }
    
    @Transactional
    public void deleteRule(Long id) {
        ruleRepository.deleteById(id);
    }
    
    @Transactional
    public void toggleRule(Long id, boolean active) {
        AlertRule rule = ruleRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("规则不存在: " + id));
        rule.setIsActive(active);
        ruleRepository.save(rule);
    }
    
    private void validateRule(AlertRule rule) {
        if (rule.getRuleType() == null) {
            throw new IllegalArgumentException("规则类型不能为空");
        }
        
        switch (rule.getRuleType()) {
            case "THRESHOLD":
                if (rule.getOperator() == null || rule.getThresholdValue() == null) {
                    throw new IllegalArgumentException("阈值规则需要指定操作符和阈值");
                }
                break;
            case "ANOMALY":
                if (rule.getDetectionMethod() == null) {
                    throw new IllegalArgumentException("异常检测规则需要指定检测方法");
                }
                break;
        }
    }
    
    @Scheduled(fixedRate = 60000)
    @Transactional
    public void checkAllRules() {
        logger.debug("开始检查所有告警规则...");
        
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(5);
        List<AlertRule> rulesToCheck = ruleRepository.findRulesNeedingCheck(threshold);
        
        for (AlertRule rule : rulesToCheck) {
            try {
                checkRule(rule);
            } catch (Exception e) {
                logger.error("检查规则失败: {} - {}", rule.getName(), e.getMessage());
            }
        }
    }
    
    @Transactional
    public void checkRule(AlertRule rule) {
        logger.info("检查规则: {}", rule.getName());
        
        dynamicDataSourceService.switchDataSource(rule.getDataSourceId());
        
        boolean triggered = false;
        BigDecimal actualValue = null;
        BigDecimal expectedValue = null;
        BigDecimal deviationRate = null;
        String message = "";
        
        switch (rule.getRuleType()) {
            case "THRESHOLD":
                ThresholdResult result = checkThreshold(rule);
                triggered = result.triggered;
                actualValue = result.actualValue;
                expectedValue = rule.getThresholdValue();
                message = result.message;
                break;
            case "ANOMALY":
                AnomalyResult anomalyResult = checkAnomaly(rule);
                triggered = anomalyResult.triggered;
                actualValue = anomalyResult.actualValue;
                expectedValue = anomalyResult.expectedValue;
                deviationRate = anomalyResult.deviationRate;
                message = anomalyResult.message;
                break;
            case "TREND":
                TrendResult trendResult = checkTrend(rule);
                triggered = trendResult.triggered;
                actualValue = trendResult.actualValue;
                expectedValue = trendResult.expectedValue;
                deviationRate = trendResult.deviationRate;
                message = trendResult.message;
                break;
            case "MISSING":
                MissingResult missingResult = checkMissing(rule);
                triggered = missingResult.triggered;
                actualValue = missingResult.actualValue;
                message = missingResult.message;
                break;
        }
        
        rule.setLastCheckTime(LocalDateTime.now());
        ruleRepository.save(rule);
        
        if (triggered) {
            if (shouldTriggerAlert(rule)) {
                createAlertRecord(rule, actualValue, expectedValue, deviationRate, message);
                rule.setLastAlertTime(LocalDateTime.now());
                ruleRepository.save(rule);
            }
        }
    }
    
    private boolean shouldTriggerAlert(AlertRule rule) {
        if (rule.getLastAlertTime() == null) {
            return true;
        }
        
        LocalDateTime cooldownEnd = rule.getLastAlertTime()
            .plusMinutes(rule.getCooldownMinutes());
        return LocalDateTime.now().isAfter(cooldownEnd);
    }
    
    private ThresholdResult checkThreshold(AlertRule rule) {
        String sql = buildThresholdSql(rule);
        BigDecimal value = jdbcTemplate.queryForObject(sql, BigDecimal.class);
        
        boolean triggered = false;
        String message = "";
        
        if (value != null) {
            switch (rule.getOperator()) {
                case ">":
                    triggered = value.compareTo(rule.getThresholdValue()) > 0;
                    message = String.format("值 %s 大于阈值 %s", value, rule.getThresholdValue());
                    break;
                case "<":
                    triggered = value.compareTo(rule.getThresholdValue()) < 0;
                    message = String.format("值 %s 小于阈值 %s", value, rule.getThresholdValue());
                    break;
                case ">=":
                    triggered = value.compareTo(rule.getThresholdValue()) >= 0;
                    message = String.format("值 %s 大于等于阈值 %s", value, rule.getThresholdValue());
                    break;
                case "<=":
                    triggered = value.compareTo(rule.getThresholdValue()) <= 0;
                    message = String.format("值 %s 小于等于阈值 %s", value, rule.getThresholdValue());
                    break;
                case "==":
                    triggered = value.compareTo(rule.getThresholdValue()) == 0;
                    message = String.format("值 %s 等于阈值 %s", value, rule.getThresholdValue());
                    break;
                case "!=":
                    triggered = value.compareTo(rule.getThresholdValue()) != 0;
                    message = String.format("值 %s 不等于阈值 %s", value, rule.getThresholdValue());
                    break;
                case "between":
                    triggered = value.compareTo(rule.getThresholdValue()) >= 0 
                            && value.compareTo(rule.getThresholdValue2()) <= 0;
                    message = String.format("值 %s 在范围 [%s, %s] 外", 
                        value, rule.getThresholdValue(), rule.getThresholdValue2());
                    break;
            }
        }
        
        return new ThresholdResult(triggered, value, message);
    }
    
    private String buildThresholdSql(AlertRule rule) {
        if (rule.getCheckSql() != null && !rule.getCheckSql().isEmpty()) {
            return rule.getCheckSql();
        }
        
        String aggFunc = "SUM";
        if (rule.getColumnName() != null) {
            aggFunc = "MAX";
        }
        
        return String.format("SELECT %s(%s) FROM %s", 
            aggFunc, 
            rule.getColumnName() != null ? rule.getColumnName() : "*", 
            rule.getTableName());
    }
    
    private AnomalyResult checkAnomaly(AlertRule rule) {
        String currentValueSql = buildThresholdSql(rule);
        BigDecimal currentValue = jdbcTemplate.queryForObject(currentValueSql, BigDecimal.class);
        
        String baselineSql = String.format(
            "SELECT AVG(val), STDDEV(val) FROM (" +
            "SELECT %s(%s) as val FROM %s " +
            "WHERE created_at >= DATE_SUB(NOW(), INTERVAL %d DAY) " +
            "GROUP BY DATE(created_at)" +
            ") t",
            "SUM", rule.getColumnName() != null ? rule.getColumnName() : "*", 
            rule.getTableName(), rule.getBaselinePeriod()
        );
        
        Map<String, Object> baseline = jdbcTemplate.queryForMap(baselineSql);
        BigDecimal avgValue = (BigDecimal) baseline.get("AVG(val)");
        BigDecimal stdValue = (BigDecimal) baseline.get("STDDEV(val)");
        
        if (avgValue == null || stdValue == null || stdValue.compareTo(BigDecimal.ZERO) == 0) {
            return new AnomalyResult(false, currentValue, avgValue, null, "基线数据不足");
        }
        
        BigDecimal sensitivity = rule.getSensitivity();
        BigDecimal threshold = avgValue.add(stdValue.multiply(sensitivity));
        
        boolean triggered = currentValue != null && currentValue.compareTo(threshold) > 0;
        
        BigDecimal deviationRate = null;
        if (currentValue != null && avgValue.compareTo(BigDecimal.ZERO) != 0) {
            deviationRate = currentValue.subtract(avgValue)
                .divide(avgValue, 4, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"));
        }
        
        String message = triggered ? 
            String.format("检测到异常: 当前值 %s 超出正常范围(均值 %s + %s倍标准差)", 
                currentValue, avgValue, sensitivity) :
            "数据正常";
        
        return new AnomalyResult(triggered, currentValue, avgValue, deviationRate, message);
    }
    
    private TrendResult checkTrend(AlertRule rule) {
        String sql = String.format(
            "SELECT " +
            "  AVG(CASE WHEN created_at >= DATE_SUB(NOW(), INTERVAL 1 DAY) THEN %s END) as today_value, " +
            "  AVG(CASE WHEN created_at >= DATE_SUB(NOW(), INTERVAL 2 DAY) AND created_at < DATE_SUB(NOW(), INTERVAL 1 DAY) THEN %s END) as yesterday_value " +
            "FROM %s",
            rule.getColumnName() != null ? rule.getColumnName() : "1",
            rule.getColumnName() != null ? rule.getColumnName() : "1",
            rule.getTableName()
        );
        
        Map<String, Object> result = jdbcTemplate.queryForMap(sql);
        BigDecimal todayValue = (BigDecimal) result.get("today_value");
        BigDecimal yesterdayValue = (BigDecimal) result.get("yesterday_value");
        
        if (todayValue == null || yesterdayValue == null) {
            return new TrendResult(false, todayValue, yesterdayValue, null, "数据不足");
        }
        
        BigDecimal deviationRate = null;
        if (yesterdayValue.compareTo(BigDecimal.ZERO) != 0) {
            deviationRate = todayValue.subtract(yesterdayValue)
                .divide(yesterdayValue, 4, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"));
        }
        
        BigDecimal threshold = rule.getThresholdValue() != null ? rule.getThresholdValue() : new BigDecimal("20");
        boolean triggered = deviationRate != null && deviationRate.abs().compareTo(threshold) > 0;
        
        String message = triggered ?
            String.format("检测到趋势变化: 相比昨日变化 %.2f%%", deviationRate) :
            "趋势正常";
        
        return new TrendResult(triggered, todayValue, yesterdayValue, deviationRate, message);
    }
    
    private MissingResult checkMissing(AlertRule rule) {
        String sql = String.format(
            "SELECT COUNT(*) as total, SUM(CASE WHEN %s IS NULL OR %s = '' THEN 1 ELSE 0 END) as missing " +
            "FROM %s",
            rule.getColumnName(), rule.getColumnName(), rule.getTableName()
        );
        
        Map<String, Object> result = jdbcTemplate.queryForMap(sql);
        Long total = (Long) result.get("total");
        Long missing = (Long) result.get("missing");
        
        BigDecimal missingRate = total > 0 ? 
            new BigDecimal(missing).divide(new BigDecimal(total), 4, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100")) : BigDecimal.ZERO;
        
        BigDecimal threshold = rule.getThresholdValue() != null ? rule.getThresholdValue() : new BigDecimal("5");
        boolean triggered = missingRate.compareTo(threshold) > 0;
        
        String message = triggered ?
            String.format("检测到缺失数据: %d 条记录缺失, 占比 %.2f%%", missing, missingRate) :
            "数据完整性正常";
        
        return new MissingResult(triggered, new BigDecimal(missing), message);
    }
    
    @Transactional
    public void createAlertRecord(AlertRule rule, BigDecimal actualValue, 
                                   BigDecimal expectedValue, BigDecimal deviationRate, String message) {
        AlertRecord record = new AlertRecord();
        record.setRuleId(rule.getId());
        record.setRuleName(rule.getName());
        record.setDataSourceId(rule.getDataSourceId());
        record.setTableName(rule.getTableName());
        record.setAlertLevel(rule.getAlertLevel());
        record.setAlertType(rule.getRuleType());
        record.setTitle(generateAlertTitle(rule, message));
        record.setMessage(message);
        record.setActualValue(actualValue);
        record.setExpectedValue(expectedValue);
        record.setDeviationRate(deviationRate);
        record.setStatus("PENDING");
        
        recordRepository.save(record);
        
        sendNotifications(rule, record);
        
        logger.warn("告警触发: {} - {}", rule.getName(), message);
    }
    
    private String generateAlertTitle(AlertRule rule, String message) {
        return String.format("[%s] %s - %s", 
            rule.getAlertLevel(), rule.getName(), message.substring(0, Math.min(50, message.length())));
    }
    
    private void sendNotifications(AlertRule rule, AlertRecord record) {
        if (rule.getAlertChannels() == null || rule.getAlertChannels().isEmpty()) {
            return;
        }
        
        String[] channels = rule.getAlertChannels().split(",");
        Map<String, String> notifyStatus = new HashMap<>();
        
        for (String channel : channels) {
            try {
                switch (channel.trim().toUpperCase()) {
                    case "EMAIL":
                        sendEmailNotification(rule, record);
                        notifyStatus.put("EMAIL", "SENT");
                        break;
                    case "WECHAT":
                        sendWechatNotification(rule, record);
                        notifyStatus.put("WECHAT", "SENT");
                        break;
                    case "DINGTALK":
                        sendDingtalkNotification(rule, record);
                        notifyStatus.put("DINGTALK", "SENT");
                        break;
                }
            } catch (Exception e) {
                notifyStatus.put(channel.trim().toUpperCase(), "FAILED: " + e.getMessage());
                logger.error("发送通知失败: {} - {}", channel, e.getMessage());
            }
        }
        
        try {
            record.setNotifyStatus(objectMapper.writeValueAsString(notifyStatus));
            recordRepository.save(record);
        } catch (Exception e) {
            logger.error("保存通知状态失败", e);
        }
    }
    
    private void sendEmailNotification(AlertRule rule, AlertRecord record) {
        logger.info("发送邮件通知: {} -> {}", record.getTitle(), rule.getAlertReceivers());
    }
    
    private void sendWechatNotification(AlertRule rule, AlertRecord record) {
        logger.info("发送企业微信通知: {}", record.getTitle());
    }
    
    private void sendDingtalkNotification(AlertRule rule, AlertRecord record) {
        logger.info("发送钉钉通知: {}", record.getTitle());
    }
    
    public List<AlertRecord> getRecentAlerts(int hours) {
        LocalDateTime startTime = LocalDateTime.now().minusHours(hours);
        return recordRepository.findRecentAlerts(startTime);
    }
    
    public Map<String, Object> getAlertStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        stats.put("totalPending", recordRepository.countPendingAlerts());
        stats.put("criticalPending", recordRepository.countCriticalPendingAlerts());
        stats.put("todayAlerts", recordRepository.countAlertsSince(LocalDateTime.now().toLocalDate().atStartOfDay()));
        
        List<Object[]> statusCounts = recordRepository.countByStatus();
        Map<String, Long> byStatus = new HashMap<>();
        for (Object[] row : statusCounts) {
            byStatus.put((String) row[0], (Long) row[1]);
        }
        stats.put("byStatus", byStatus);
        
        return stats;
    }
    
    public List<AlertRecord> getAlertRecords(String status, String level, int limit) {
        List<AlertRecord> records;
        
        if (status != null && !status.isEmpty()) {
            records = recordRepository.findByStatus(status);
        } else if (level != null && !level.isEmpty()) {
            records = recordRepository.findByAlertLevel(level);
        } else {
            records = recordRepository.findRecentAlerts(LocalDateTime.now().minusDays(7));
        }
        
        return records.stream().limit(limit).toList();
    }
    
    @Transactional
    public void confirmAlert(Long id, String confirmedBy) {
        AlertRecord record = recordRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("告警记录不存在: " + id));
        record.setStatus("CONFIRMED");
        record.setConfirmedBy(confirmedBy);
        record.setConfirmedAt(LocalDateTime.now());
        recordRepository.save(record);
    }
    
    @Transactional
    public void resolveAlert(Long id, String resolvedBy, String note) {
        AlertRecord record = recordRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("告警记录不存在: " + id));
        record.setStatus("RESOLVED");
        record.setResolvedBy(resolvedBy);
        record.setResolvedAt(LocalDateTime.now());
        record.setResolveNote(note);
        recordRepository.save(record);
    }
    
    @Transactional
    public void ignoreAlert(Long id) {
        AlertRecord record = recordRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("告警记录不存在: " + id));
        record.setStatus("IGNORED");
        recordRepository.save(record);
    }
    
    @Transactional
    public void manualCheck(Long ruleId) {
        AlertRule rule = ruleRepository.findById(ruleId)
            .orElseThrow(() -> new IllegalArgumentException("规则不存在: " + ruleId));
        checkRule(rule);
    }
    
    private static class ThresholdResult {
        boolean triggered;
        BigDecimal actualValue;
        String message;
        
        ThresholdResult(boolean triggered, BigDecimal actualValue, String message) {
            this.triggered = triggered;
            this.actualValue = actualValue;
            this.message = message;
        }
    }
    
    private static class AnomalyResult {
        boolean triggered;
        BigDecimal actualValue;
        BigDecimal expectedValue;
        BigDecimal deviationRate;
        String message;
        
        AnomalyResult(boolean triggered, BigDecimal actualValue, BigDecimal expectedValue, 
                      BigDecimal deviationRate, String message) {
            this.triggered = triggered;
            this.actualValue = actualValue;
            this.expectedValue = expectedValue;
            this.deviationRate = deviationRate;
            this.message = message;
        }
    }
    
    private static class TrendResult {
        boolean triggered;
        BigDecimal actualValue;
        BigDecimal expectedValue;
        BigDecimal deviationRate;
        String message;
        
        TrendResult(boolean triggered, BigDecimal actualValue, BigDecimal expectedValue, 
                    BigDecimal deviationRate, String message) {
            this.triggered = triggered;
            this.actualValue = actualValue;
            this.expectedValue = expectedValue;
            this.deviationRate = deviationRate;
            this.message = message;
        }
    }
    
    private static class MissingResult {
        boolean triggered;
        BigDecimal actualValue;
        String message;
        
        MissingResult(boolean triggered, BigDecimal actualValue, String message) {
            this.triggered = triggered;
            this.actualValue = actualValue;
            this.message = message;
        }
    }
}
