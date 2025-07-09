package com.shentong.api.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "deepvision")
public class ApiConfig {
    private String tenantId;
    private String apiKey;
    private String secret;
    private String ak;
    private String sk;
    private String userId;
    private String workspace;
    private String workspaceId;
    private String apiBaseUrl;
    private FileScanConfig fileScan;
    private LogConfig log;
    private int knowledgeBaseMaxFiles; // 新增：知识库最大文件数


    @Data
    public static class FileScanConfig {
        private String dir;
        private String backupDir;
        private String outputDir;
        private String cacheDir;
        private String cron;
        private int cleanDays;
        private String supportedExtensions = "docx,doc,txt"; // 支持的文件扩展名（可配置）
    }

    @Data
    public static class LogConfig {
        private String dir;
    }
}