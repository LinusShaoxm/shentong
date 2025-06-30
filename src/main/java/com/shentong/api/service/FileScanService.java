package com.shentong.api.service;

import com.shentong.api.config.ApiConfig;
import com.shentong.api.model.FileUploadRecord;
import com.shentong.api.util.FileUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.Date;

@Slf4j
@Service
public class FileScanService {

    @Autowired
    private ApiConfig apiConfig;

    @Autowired
    private DeepVisionService deepVisionService;

    // 定时任务扫描文件
    @Scheduled(cron = "${deepvision.file-scan.cron}")
    public void scanFiles() {
        log.info("开始扫描文件...");

        String scanDir = apiConfig.getFileScan().getDir();
        File dir = new File(scanDir);

        if (!dir.exists() || !dir.isDirectory()) {
            log.error("扫描目录不存在或不是目录: {}", scanDir);
            return;
        }

        // 获取所有月份子目录
        File[] monthDirs = dir.listFiles(File::isDirectory);
        if (monthDirs == null || monthDirs.length == 0) {
            log.info("没有找到月份子目录");
            return;
        }
        log.info("扫描到的文件夹数量:{}", monthDirs.length);

        for (File monthDir : monthDirs) {
            log.info("循环当前文件夹文件,文件夹名称:{}", monthDir.getName());
            File[] files = monthDir.listFiles();
            if (files == null) {
                log.info("文件夹{}下不存在文件,跳过当前文件夹", monthDir.getName());
                continue;
            }
            log.info("当前文件夹名称:{},扫描到的文件数量为:{}", monthDir.getName(), files.length);
            for (File file : files) {
                log.info("开始创建知识库上传文件,文件名称:{}", file.getName());
                if (file.isFile()) {
                    try {
                        // 1. 上传文件
                        String knowledgeId = deepVisionService.createKnowledgeBase(
                                "知识库_" + System.currentTimeMillis(),
                                "自动创建的知识库"
                        );

                        deepVisionService.uploadFileCreateUnit(knowledgeId, file.getAbsolutePath());

                        // 2. 备份文件
                        String backupPath = apiConfig.getFileScan().getBackupDir() +
                                "/" + monthDir.getName() + "/" + file.getName();
                        FileUtil.backupFile(file.getAbsolutePath(), backupPath);

                        // 3. 记录上传日志
                        FileUploadRecord record = new FileUploadRecord();
                        record.setFileName(file.getName());
                        record.setFilePath(file.getAbsolutePath());
                        record.setBackupPath(backupPath);
                        record.setUploadTime(new Date());
                        record.setKnowledgeId(knowledgeId);
                        // saveToDatabase(record); // 实际项目中应该保存到数据库

                        // 4. 删除原文件
                        file.delete();

                        log.info("文件 {} 上传并处理成功", file.getName());
                    } catch (Exception e) {
                        log.error("处理文件 {} 失败: {}", file.getName(), e.getMessage());
                    }
                }
            }
        }

        // 清理过期备份文件
        cleanExpiredBackupFiles();
    }

    // 清理过期备份文件
    private void cleanExpiredBackupFiles() {
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