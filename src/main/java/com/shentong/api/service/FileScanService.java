package com.shentong.api.service;

import com.shentong.api.cache.FolderScanCache;
import com.shentong.api.config.ApiConfig;
import com.shentong.api.model.FileUploadRecord;
import com.shentong.api.service.DeepVisionService;
import com.shentong.api.util.DateUtil;
import com.shentong.api.util.FileUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.apache.poi.xwpf.usermodel.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.NoSuchAlgorithmException;
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

    @Autowired
    private FolderScanCache folderScanCache;

    @Scheduled(cron = "${deepvision.file-scan.cron}")
    public void scanFiles() throws IOException, NoSuchAlgorithmException {
        log.info("===== 开始文件扫描处理 =====");

        String scanDir = apiConfig.getFileScan().getDir();
        log.info("apiConfig: {}", apiConfig);
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

    private void processFiles(File rootDir) throws IOException, NoSuchAlgorithmException {
        // 获取所有子文件夹
        File[] yearDirs = rootDir.listFiles(File::isDirectory);
        if (yearDirs == null || yearDirs.length == 0) {
            log.info("未找到年份子目录");
            return;
        }

        // 获取配置的支持的文件扩展名（如 docx,doc,txt）
        String supportedExtensions = apiConfig.getFileScan().getSupportedExtensions();
        List<String> extensions = Arrays.asList(supportedExtensions.split(","));

        for (File yearDir : yearDirs) {
            File[] subDirs = yearDir.listFiles(File::isDirectory);
            if (subDirs == null || subDirs.length == 0) {
                log.info("未找到月份子目录");
                continue;
            }
            for (File subDir : subDirs) {
                log.info("\n处理目录: {}", yearDir.getName() + "/" + subDir.getName());
                if (folderScanCache.isFolderScanned(subDir.getPath())) {
                    System.out.println("文件夹已扫描过，跳过: " + subDir.getPath());
                    continue;
                }
                // 一个文件夹对应一个知识库
                resetKnowledgeTracking();
                // 获取当前文件夹下所有支持的文件
                List<File> files = new ArrayList<>();
                try {
                    files = Files.walk(subDir.toPath())
                            .filter(p -> {
                                String fileName = p.toString().toLowerCase();
                                return extensions.stream().anyMatch(fileName::endsWith) && fileName.equalsIgnoreCase(subDir.toPath() + "/merged.docx");
                            })
                            .map(Path::toFile)
                            .collect(Collectors.toList());
                } catch (Exception e) {
                    log.error("获取当前文件夹下文件异常, 文件夹: {}, 异常信息:", yearDir.getName() + "/" + subDir.getName(), e);
                }

                if (files.isEmpty()) {
                    log.info("目录 {} 中没有支持的文件", yearDir.getName() + "/" + subDir.getName());
                    continue;
                }

                // 打印待处理文件信息
                logFileInfo(files);
                // 合并所有文件（docx, doc, txt）到一个 Word 文档
                File mergedFile = mergeAllFilesToWord(files, yearDir.getName(), subDir.getName());
                processMergedDocument(mergedFile, yearDir.getName(), subDir.getName());
                folderScanCache.markFolderAsScanned(subDir.getPath());
            }
        }
    }

    /**
     * 合并所有文件（docx, doc, txt）到一个 Word 文档
     */
    private File mergeAllFilesToWord(List<File> files, String yearName, String folderName) throws IOException {
        XWPFDocument mergedDoc = new XWPFDocument();
        String outputDir = apiConfig.getFileScan().getOutputDir();
        // 不存在先创建
        Path cachePath = Paths.get(outputDir);
        if (!Files.exists(cachePath)) {
            Files.createDirectories(cachePath);
        }
        String mergedFileName = outputDir + "/" + yearName + "年" + folderName + "月分析报告.docx";
        log.info("合并文件名称:{}", mergedFileName);
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
            throw new RuntimeException(e);
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
    private void processMergedDocument(File mergedFile, String yearName, String folderName) {
        try {
            // 知识库处理逻辑
            if (currentKnowledgeId == null || currentKnowledgeFileCount >= apiConfig.getKnowledgeBaseMaxFiles()) {
                createNewKnowledgeBase(yearName, folderName);
                //currentKnowledgeId = "1";
            }
            if (Objects.isNull(currentKnowledgeId)) {
                log.error("知识库Id为空");
                return;
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
            throw new RuntimeException(e);
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

    private void createNewKnowledgeBase(String yearName, String folderName) {
        String name = yearName + "年" + folderName;
        try {
            currentKnowledgeId = deepVisionService.createKnowledgeBase(
                    name + "月份整体分析报告",
                    name + "月份整体分析报告");
        } catch (Exception e) {
            log.error("知识库创建失败:", e);
            currentKnowledgeId = null;
        }
        currentKnowledgeFileCount = 0;
        log.info("创建新知识库: {}", currentKnowledgeId);
    }

    private void resetKnowledgeTracking() {
        currentKnowledgeId = null;
        currentKnowledgeFileCount = 0;
    }

    private void cleanExpiredBackupFiles() throws IOException {
        String backupDir = apiConfig.getFileScan().getBackupDir();
        // 不存在先创建
        Path backupPath = Paths.get(backupDir);
        if (!Files.exists(backupPath)) {
            Files.createDirectories(backupPath);
        }
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