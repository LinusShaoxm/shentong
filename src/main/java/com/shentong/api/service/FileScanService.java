package com.shentong.api.service;

import com.shentong.api.config.ApiConfig;
import com.shentong.api.model.FileUploadRecord;
import com.shentong.api.util.FileUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.Date;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileScanService {

    private final ApiConfig apiConfig;
    private final DeepVisionService deepVisionService;

    private String currentKnowledgeId;
    private int currentKnowledgeFileCount = 0;

    @Scheduled(cron = "${deepvision.file-scan.cron}")
    public void scanFiles() {
        log.info("开始扫描文件...");

        String scanDir = apiConfig.getFileScan().getDir();
        File dir = new File(scanDir);

        if (!dir.exists() || !dir.isDirectory()) {
            log.error("扫描目录不存在或不是目录: {}", scanDir);
            return;
        }

        File[] monthDirs = dir.listFiles(File::isDirectory);
        if (monthDirs == null || monthDirs.length == 0) {
            log.info("没有找到月份子目录");
            return;
        }

        for (File monthDir : monthDirs) {
            log.info("处理文件夹: {}", monthDir.getName());

            File[] files = Optional.ofNullable(monthDir.listFiles()).orElse(new File[0]);
            log.info("文件夹 {} 中有 {} 个文件", monthDir.getName(), files.length);

            for (File file : files) {
                if (file.isFile()) {
                    processFile(file, monthDir.getName());
                }
            }
        }

        cleanExpiredBackupFiles();
        resetKnowledgeTracking();
    }

    private void processFile(File file, String monthDirName) {
        try {
            // 1. 检查是否需要新建知识库（当前知识库为空或已满）
            if (currentKnowledgeId == null || currentKnowledgeFileCount >= apiConfig.getKnowledgeBaseMaxFiles()) {
                createNewKnowledgeBase();
            }

            // 2. 上传文件到当前知识库
            deepVisionService.uploadFileCreateUnit(currentKnowledgeId, file.getAbsolutePath());
            currentKnowledgeFileCount++;

            // 3. 备份文件
            String backupPath = apiConfig.getFileScan().getBackupDir() +
                    "/" + monthDirName + "/" + file.getName();
            FileUtil.backupFile(file.getAbsolutePath(), backupPath);

            // 4. 记录上传日志
            FileUploadRecord record = new FileUploadRecord();
            record.setFileName(file.getName());
            record.setFilePath(file.getAbsolutePath());
            record.setBackupPath(backupPath);
            record.setUploadTime(new Date());
            record.setKnowledgeId(currentKnowledgeId);
            // saveToDatabase(record);

            // 5. 删除原文件
            file.delete();

            log.info("文件 {} 上传到知识库 {} 成功 (当前知识库文件数: {}/{})",
                    file.getName(),
                    currentKnowledgeId,
                    currentKnowledgeFileCount,
                    apiConfig.getKnowledgeBaseMaxFiles());
        } catch (Exception e) {
            log.error("处理文件 {} 失败: {}", file.getName(), e.getMessage());
            if (currentKnowledgeFileCount >= apiConfig.getKnowledgeBaseMaxFiles()) {
                resetKnowledgeTracking();
            }
        }
    }

    private void createNewKnowledgeBase() {
        currentKnowledgeId = deepVisionService.createKnowledgeBase(
                "知识库_" + System.currentTimeMillis(),
                "自动创建的知识库"
        );
        currentKnowledgeFileCount = 0;
        log.info("创建新知识库: {}", currentKnowledgeId);
    }

    private void resetKnowledgeTracking() {
        currentKnowledgeId = null;
        currentKnowledgeFileCount = 0;
    }

    private void cleanExpiredBackupFiles() {
        // 保持原逻辑不变
        int cleanDays = apiConfig.getFileScan().getCleanDays();
        if (cleanDays <= 0) {
            return;
        }

        String backupDir = apiConfig.getFileScan().getBackupDir();
        File dir = new File(backupDir);

        if (!dir.exists() || !dir.isDirectory()) {
            return;
        }

        File[] monthDirs = dir.listFiles(File::isDirectory);
        if (monthDirs == null || monthDirs.length == 0) {
            return;
        }

        for (File monthDir : monthDirs) {
            File[] files = monthDir.listFiles();
            if (files == null || files.length == 0) {
                continue;
            }

            for (File file : files) {
                if (file.isFile()) {
                    long fileAge = System.currentTimeMillis() - file.lastModified();
                    long daysOld = fileAge / (24 * 60 * 60 * 1000);

                    if (daysOld >= cleanDays) {
                        if (file.delete()) {
                            log.info("已删除过期备份文件: {}", file.getAbsolutePath());
                        } else {
                            log.error("删除过期备份文件失败: {}", file.getAbsolutePath());
                        }
                    }
                }
            }
        }
    }
}