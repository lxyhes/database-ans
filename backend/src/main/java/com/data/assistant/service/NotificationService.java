package com.data.assistant.service;

import com.data.assistant.model.AlertRecord;
import com.data.assistant.model.AlertRule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.*;

@Service
public class NotificationService {
    
    private static final Logger logger = LoggerFactory.getLogger(NotificationService.class);
    
    @Value("${alert.email.enabled:false}")
    private boolean emailEnabled;
    
    @Value("${alert.email.smtp.host:}")
    private String smtpHost;
    
    @Value("${alert.email.smtp.port:587}")
    private int smtpPort;
    
    @Value("${alert.email.smtp.username:}")
    private String smtpUsername;
    
    @Value("${alert.email.smtp.password:}")
    private String smtpPassword;
    
    @Value("${alert.email.from:}")
    private String emailFrom;
    
    @Value("${alert.wechat.webhook:}")
    private String wechatWebhook;
    
    @Value("${alert.dingtalk.webhook:}")
    private String dingtalkWebhook;
    
    @Value("${alert.dingtalk.secret:}")
    private String dingtalkSecret;
    
    @Autowired
    private org.springframework.web.client.RestTemplate restTemplate;
    
    private JavaMailSender mailSender;
    
    @PostConstruct
    public void init() {
        if (emailEnabled && smtpHost != null && !smtpHost.isEmpty()) {
            JavaMailSenderImpl sender = new JavaMailSenderImpl();
            sender.setHost(smtpHost);
            sender.setPort(smtpPort);
            sender.setUsername(smtpUsername);
            sender.setPassword(smtpPassword);
            
            Properties props = sender.getJavaMailProperties();
            props.put("mail.transport.protocol", "smtp");
            props.put("mail.smtp.auth", "true");
            props.put("mail.smtp.starttls.enable", "true");
            props.put("mail.smtp.starttls.required", "true");
            props.put("mail.smtp.connectiontimeout", "5000");
            props.put("mail.smtp.timeout", "5000");
            
            mailSender = sender;
            logger.info("邮件通知服务已启用: {}", smtpHost);
        } else {
            logger.info("邮件通知服务未配置");
        }
    }
    
    public Map<String, Object> sendEmail(String to, String subject, String content) {
        Map<String, Object> result = new HashMap<>();
        
        if (!emailEnabled || mailSender == null) {
            result.put("success", false);
            result.put("message", "邮件服务未启用");
            logger.warn("邮件服务未启用，无法发送邮件");
            return result;
        }
        
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(emailFrom);
            message.setTo(to.split(","));
            message.setSubject(subject);
            message.setText(content);
            
            mailSender.send(message);
            
            result.put("success", true);
            result.put("message", "邮件发送成功");
            logger.info("邮件发送成功: {} -> {}", subject, to);
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "邮件发送失败: " + e.getMessage());
            logger.error("邮件发送失败: {}", e.getMessage());
        }
        
        return result;
    }
    
    public Map<String, Object> sendWechat(String content) {
        Map<String, Object> result = new HashMap<>();
        
        if (wechatWebhook == null || wechatWebhook.isEmpty()) {
            result.put("success", false);
            result.put("message", "企业微信Webhook未配置");
            logger.warn("企业微信Webhook未配置");
            return result;
        }
        
        try {
            Map<String, Object> body = new HashMap<>();
            Map<String, String> text = new HashMap<>();
            text.put("content", content);
            body.put("msgtype", "text");
            body.put("text", text);
            
            Map<String, Object> response = restTemplate.postForObject(wechatWebhook, body, Map.class);
            
            if (response != null && response.get("errcode") != null) {
                int errcode = ((Number) response.get("errcode")).intValue();
                if (errcode == 0) {
                    result.put("success", true);
                    result.put("message", "企业微信通知发送成功");
                    logger.info("企业微信通知发送成功");
                } else {
                    result.put("success", false);
                    result.put("message", "企业微信返回错误: " + response.get("errmsg"));
                    logger.error("企业微信返回错误: {}", response.get("errmsg"));
                }
            } else {
                result.put("success", true);
                result.put("message", "企业微信通知已发送");
            }
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "企业微信发送失败: " + e.getMessage());
            logger.error("企业微信发送失败: {}", e.getMessage());
        }
        
        return result;
    }
    
    public Map<String, Object> sendDingtalk(String content) {
        Map<String, Object> result = new HashMap<>();
        
        if (dingtalkWebhook == null || dingtalkWebhook.isEmpty()) {
            result.put("success", false);
            result.put("message", "钉钉Webhook未配置");
            logger.warn("钉钉Webhook未配置");
            return result;
        }
        
        try {
            String webhookUrl = dingtalkWebhook;
            
            if (dingtalkSecret != null && !dingtalkSecret.isEmpty()) {
                long timestamp = System.currentTimeMillis();
                String stringToSign = timestamp + "\n" + dingtalkSecret;
                javax.crypto.spec.SecretKeySpec keySpec = new javax.crypto.spec.SecretKeySpec(
                    dingtalkSecret.getBytes(java.nio.charset.StandardCharsets.UTF_8), "HmacSHA256");
                javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA256");
                mac.init(keySpec);
                byte[] signData = mac.doFinal(stringToSign.getBytes(java.nio.charset.StandardCharsets.UTF_8));
                String sign = java.util.Base64.getEncoder().encodeToString(signData);
                sign = java.net.URLEncoder.encode(sign, "UTF-8");
                webhookUrl = webhookUrl + "&timestamp=" + timestamp + "&sign=" + sign;
            }
            
            Map<String, Object> body = new HashMap<>();
            Map<String, String> text = new HashMap<>();
            text.put("content", content);
            body.put("msgtype", "text");
            body.put("text", text);
            
            Map<String, Object> response = restTemplate.postForObject(webhookUrl, body, Map.class);
            
            if (response != null && response.get("errcode") != null) {
                int errcode = ((Number) response.get("errcode")).intValue();
                if (errcode == 0) {
                    result.put("success", true);
                    result.put("message", "钉钉通知发送成功");
                    logger.info("钉钉通知发送成功");
                } else {
                    result.put("success", false);
                    result.put("message", "钉钉返回错误: " + response.get("errmsg"));
                    logger.error("钉钉返回错误: {}", response.get("errmsg"));
                }
            } else {
                result.put("success", true);
                result.put("message", "钉钉通知已发送");
            }
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "钉钉发送失败: " + e.getMessage());
            logger.error("钉钉发送失败: {}", e.getMessage());
        }
        
        return result;
    }
    
    public void sendAlertNotification(AlertRule rule, AlertRecord record) {
        String content = buildAlertContent(rule, record);
        String subject = "[数据告警] " + record.getTitle();
        
        if (rule.getAlertChannels() == null || rule.getAlertChannels().isEmpty()) {
            logger.info("未配置告警通知渠道");
            return;
        }
        
        String[] channels = rule.getAlertChannels().split(",");
        
        for (String channel : channels) {
            switch (channel.trim().toUpperCase()) {
                case "EMAIL":
                    if (rule.getAlertReceivers() != null && !rule.getAlertReceivers().isEmpty()) {
                        sendEmail(rule.getAlertReceivers(), subject, content);
                    }
                    break;
                case "WECHAT":
                    sendWechat(content);
                    break;
                case "DINGTALK":
                    sendDingtalk(content);
                    break;
            }
        }
    }
    
    private String buildAlertContent(AlertRule rule, AlertRecord record) {
        StringBuilder sb = new StringBuilder();
        sb.append("【数据告警通知】\n\n");
        sb.append("告警名称: ").append(rule.getName()).append("\n");
        sb.append("告警级别: ").append(record.getAlertLevel()).append("\n");
        sb.append("告警类型: ").append(record.getAlertType()).append("\n");
        sb.append("数据源ID: ").append(rule.getDataSourceId()).append("\n");
        sb.append("数据表: ").append(rule.getTableName()).append("\n\n");
        sb.append("告警详情:\n");
        sb.append(record.getMessage()).append("\n\n");
        
        if (record.getActualValue() != null) {
            sb.append("实际值: ").append(record.getActualValue()).append("\n");
        }
        if (record.getExpectedValue() != null) {
            sb.append("预期值: ").append(record.getExpectedValue()).append("\n");
        }
        if (record.getDeviationRate() != null) {
            sb.append("偏差率: ").append(record.getDeviationRate()).append("%\n");
        }
        
        sb.append("\n触发时间: ").append(record.getCreatedAt()).append("\n");
        sb.append("\n请及时处理！");
        
        return sb.toString();
    }
}
