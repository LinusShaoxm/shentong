package com.shentong.api.service;

import com.shentong.api.config.ApiConfig;
import com.shentong.api.model.FileUploadRecord;
import com.shentong.api.service.DeepVisionService;
import com.shentong.api.util.FileUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xwpf.usermodel.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.stream.Collectors;

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
        log.info("===== 开始文件扫描处理 =====");

        String scanDir = apiConfig.getFileScan().getDir();
        File rootDir = new File(scanDir);

        if (!rootDir.exists()) {
            log.error("目录不存在: {}", scanDir);
            return;
        }

        // 处理所有支持的文件类型（docx, doc, txt）
        processFiles(rootDir);

        // 清理过期文件
        cleanExpiredBackupFiles();

        log.info("===== 处理完成 =====");
    }

    private void processFiles(File rootDir) {
        // 获取所有子文件夹
        File[] subDirs = rootDir.listFiles(File::isDirectory);
        if (subDirs == null || subDirs.length == 0) {
            log.info("未找到子目录");
            return;
        }

        // 获取配置的支持的文件扩展名（如 docx,doc,txt）
        String supportedExtensions = apiConfig.getFileScan().getSupportedExtensions();
        List<String> extensions = Arrays.asList(supportedExtensions.split(","));

        for (File subDir : subDirs) {
            log.info("\n处理目录: {}", subDir.getName());

            // 获取当前文件夹下所有支持的文件
            List<File> files = new ArrayList<>();
            try {
                files = Files.walk(subDir.toPath())
                        .filter(p -> {
                            String fileName = p.toString().toLowerCase();
                            return extensions.stream().anyMatch(fileName::endsWith);
                        })
                        .map(Path::toFile)
                        .collect(Collectors.toList());
            } catch (Exception e) {
                log.error("获取当前文件夹下文件异常, 文件夹: {}, 异常信息:", subDir, e);
            }

            if (files.isEmpty()) {
                log.info("目录 {} 中没有支持的文件", subDir.getName());
                continue;
            }

            // 打印待处理文件信息
            logFileInfo(files);

            // 合并所有文件（docx, doc, txt）到一个 Word 文档
            File mergedFile = mergeAllFilesToWord(files, subDir.getName());
            if (mergedFile != null) {
                processMergedDocument(mergedFile, subDir.getName());
            }
        }
    }

    /**
     * 合并所有文件（docx, doc, txt）到一个 Word 文档
     */
    private File mergeAllFilesToWord(List<File> files, String folderName) {
        XWPFDocument mergedDoc = new XWPFDocument();
        String outputDir = apiConfig.getFileScan().getOutputDir();
        String mergedFileName = outputDir + "/" + folderName + "_merged.docx";

        try {
            for (File file : files) {
                if (file.length() == 0) {
                    log.warn("跳过空文件: {}", file.getAbsolutePath());
                    continue;
                }

                String fileName = file.getName().toLowerCase();
                if (fileName.endsWith(".docx") || fileName.endsWith(".doc")) {
                    // 处理 Word 文档（直接复制段落和表格）
                    mergeWordFile(mergedDoc, file);
                } else if (fileName.endsWith(".txt")) {
                    // 处理 TXT 文件（读取内容并写入 Word）
                    mergeTxtFile(mergedDoc, file);
                }

                // 添加分页符（除最后一个文件外）
                if (!file.equals(files.get(files.size() - 1))) {
                    mergedDoc.createParagraph().createRun().addBreak(BreakType.PAGE);
                }
            }

            // 保存合并后的 Word 文档
            FileOutputStream out = new FileOutputStream(mergedFileName);
            mergedDoc.write(out);
            out.close();
            log.info("合并完成: {}", mergedFileName);

            return new File(mergedFileName);
        } catch (Exception e) {
            log.error("合并文件失败: {}", e.getMessage(), e);
            return null;
        } finally {
            try {
                mergedDoc.close();
            } catch (IOException e) {
                log.error("关闭 XWPFDocument 失败", e);
            }
        }
    }

    /**
     * 合并 Word 文件（.docx 或 .doc）
     */
    private void mergeWordFile(XWPFDocument mergedDoc, File wordFile) throws Exception {
        String fileName = wordFile.getName();
        log.info("合并 Word 文件: {}", fileName);

        try (XWPFDocument doc = new XWPFDocument(new FileInputStream(wordFile))) {
            // 复制段落
            for (XWPFParagraph para : doc.getParagraphs()) {
                XWPFParagraph newPara = mergedDoc.createParagraph();
                newPara.getCTP().set(para.getCTP().copy());
            }

            // 复制表格
            for (XWPFTable table : doc.getTables()) {
                XWPFTable newTable = mergedDoc.createTable();
                newTable.getCTTbl().set(table.getCTTbl().copy());
            }
        }
    }

    /**
     * 合并 TXT 文件（读取内容并写入 Word）
     */
    private void mergeTxtFile(XWPFDocument mergedDoc, File txtFile) throws Exception {
        String fileName = txtFile.getName();
        log.info("合并 TXT 文件: {}", fileName);

        // 添加文件名作为标题
        XWPFParagraph titlePara = mergedDoc.createParagraph();
        XWPFRun titleRun = titlePara.createRun();
        titleRun.setBold(true);
        titleRun.setFontSize(14);

        // 读取 TXT 文件内容
        List<String> lines = Files.readAllLines(txtFile.toPath());
        XWPFParagraph contentPara = mergedDoc.createParagraph();
        XWPFRun contentRun = contentPara.createRun();

        for (String line : lines) {
            contentRun.setText(line);
            contentRun.addBreak(); // 换行
        }
    }

    /**
     * 处理合并后的 Word 文档（上传到知识库）
     */
    private void processMergedDocument(File mergedFile, String folderName) {
        try {
            // 知识库处理逻辑
            if (currentKnowledgeId == null || currentKnowledgeFileCount >= apiConfig.getKnowledgeBaseMaxFiles()) {
                createNewKnowledgeBase();
            }

            // 上传合并后的 Word 文档
            deepVisionService.uploadFileCreateUnit(currentKnowledgeId, mergedFile.getAbsolutePath());
            currentKnowledgeFileCount++;

            // 备份文件
            String backupPath = apiConfig.getFileScan().getBackupDir() +
                    "/" + folderName + "_" + mergedFile.getName();
            FileUtil.backupFile(mergedFile.getAbsolutePath(), backupPath);

            // 记录日志
            FileUploadRecord record = new FileUploadRecord();
            record.setFileName(mergedFile.getName());
            record.setFilePath(mergedFile.getAbsolutePath());
            record.setBackupPath(backupPath);
            record.setUploadTime(new Date());
            record.setKnowledgeId(currentKnowledgeId);
            // saveToDatabase(record);

            log.info("合并文件 {} 已上传到知识库 {}", mergedFile.getName(), currentKnowledgeId);

        } catch (Exception e) {
            log.error("处理合并文件失败: {}", mergedFile.getName(), e);
            if (currentKnowledgeFileCount >= apiConfig.getKnowledgeBaseMaxFiles()) {
                resetKnowledgeTracking();
            }
        }
    }

    private void logFileInfo(List<File> files) {
        log.info("待处理文件列表 ({}个):", files.size());
        for (File file : files) {
            try {
                log.info("- {} ({}字节)", file.getName(), file.length());
            } catch (Exception e) {
                log.error("读取文件信息失败: {}", file.getAbsolutePath(), e);
            }
        }
    }

    private void createNewKnowledgeBase() {
        currentKnowledgeId = deepVisionService.createKnowledgeBase(
                "知识库_" + System.currentTimeMillis(),
                "深瞳文件工具_自动创建的知识库"
        );
        currentKnowledgeFileCount = 0;
        log.info("创建新知识库: {}", currentKnowledgeId);
    }

    private void resetKnowledgeTracking() {
        currentKnowledgeId = null;
        currentKnowledgeFileCount = 0;
    }

    private void cleanExpiredBackupFiles() {
        String backupDir = apiConfig.getFileScan().getBackupDir();
        int cleanDays = apiConfig.getFileScan().getCleanDays();

        try {
            Files.walkFileTree(Paths.get(backupDir), new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                    if (attrs.isRegularFile() &&
                            attrs.lastModifiedTime().toMillis() < System.currentTimeMillis() - cleanDays * 86400_000L) {
                        Files.delete(file);
                        log.info("删除过期备份文件: {}", file);
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            log.error("清理过期备份文件失败", e);
        }
    }
}