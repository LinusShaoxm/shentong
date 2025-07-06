package com.shentong.api.service;

import com.shentong.api.config.ApiConfig;
import com.shentong.api.model.FileUploadRecord;
import com.shentong.api.util.FileUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xwpf.usermodel.*;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTblPr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTcPr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTrPr;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.*;
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

        // 处理Word文档合并
        processWordDocuments(rootDir);

        // 清理过期文件
        cleanExpiredBackupFiles();

        log.info("===== 处理完成 =====");
    }

    private void processWordDocuments(File rootDir) {
        // 获取所有子文件夹
        File[] subDirs = rootDir.listFiles(File::isDirectory);
        if (subDirs == null || subDirs.length == 0) {
            log.info("未找到子目录");
            return;
        }

        for (File subDir : subDirs) {
            log.info("\n处理目录: {}", subDir.getName());

            // 获取当前文件夹下所有Word文档
            List<File> wordFiles = new ArrayList<>();
            try {
                wordFiles = Files.walk(subDir.toPath())
                        .filter(p -> p.toString().toLowerCase().endsWith(".docx"))
                        .map(Path::toFile)
                        .collect(Collectors.toList());
            } catch (Exception e) {
                log.error("获取当前文件夹下所有Word文档异常,文件夹:{},异常信息:", subDir, e);
            }

            if (wordFiles.isEmpty()) {
                log.info("目录 {} 中没有Word文档", subDir.getName());
                continue;
            }

            // 打印待合并文档信息
            logDocumentInfo(wordFiles);

            // 合并文档
            File mergedFile = mergeWordFiles(wordFiles);
            if (mergedFile == null) {
                log.error("目录 {} 文档合并失败", subDir.getName());
                continue;
            }

            // 处理合并后的文档
            processMergedDocument(mergedFile, subDir.getName());
        }
    }

    private void logDocumentInfo(List<File> documents) {
        log.info("待合并文档列表 ({}个):", documents.size());

        for (File doc : documents) {
            // 检查文件是否为空
            if (doc.length() == 0) {
                log.warn("跳过空文件: {}", doc.getAbsolutePath());
                continue;
            }

            try (InputStream is = Files.newInputStream(doc.toPath())) {
                // 再次验证流是否可读（双重检查）
                if (is.available() <= 0) {
                    log.warn("文件流为空: {}", doc.getAbsolutePath());
                    continue;
                }

                XWPFDocument document = new XWPFDocument(is);

                // 提取文档内容摘要
                StringBuilder contentPreview = new StringBuilder();
                int paraCount = 0;
                List<XWPFParagraph> paragraphs = document.getParagraphs();

                log.debug("文档 {} 段落数: {}", doc.getName(), paragraphs.size());

                for (XWPFParagraph para : paragraphs) {
                    if (paraCount++ < 3) { // 只预览前3段
                        String text = para.getText();
                        if (text != null && !text.trim().isEmpty()) {
                            contentPreview.append(text.substring(0, Math.min(text.length(), 50)))
                                    .append(text.length() > 50 ? "..." : "")
                                    .append(" | ");
                        }
                    } else {
                        break;
                    }
                }

                // 统计表格数量
                int tableCount = document.getTables().size();
                log.debug("文档 {} 表格数: {}", doc.getName(), tableCount);

                log.info("- {} ({}字节, {}段, {}表, 预览: {})",
                        doc.getName(),
                        doc.length(),
                        paragraphs.size(),
                        tableCount,
                        contentPreview.toString().replaceAll("\\s+", " "));

                document.close();

            } catch (org.apache.poi.EmptyFileException e) {
                log.error("空文件异常 - 文件: {} (大小: {}字节)",
                        doc.getAbsolutePath(), doc.length(), e);
            } catch (IOException e) {
                log.error("读取文档失败 - 文件: {}, 错误: {}",
                        doc.getAbsolutePath(), e.getMessage());
                if (log.isDebugEnabled()) {
                    log.debug("堆栈信息:", e);
                }
            } catch (Exception e) {
                log.error("处理文档时发生意外错误 - 文件: {}, 错误: {}",
                        doc.getAbsolutePath(), e.getMessage());
                if (log.isDebugEnabled()) {
                    log.debug("堆栈信息:", e);
                }
            }
        }
    }

    private File mergeWordFiles(List<File> wordFiles) {
        try (XWPFDocument mergedDoc = new XWPFDocument()) {
            log.info("开始合并 {} 个文档", wordFiles.size());

            for (int i = 0; i < wordFiles.size(); i++) {
                File file = wordFiles.get(i);
                log.info("正在处理文档 {}/{}: {}", i + 1, wordFiles.size(), file.getName());

                try (FileInputStream fis = new FileInputStream(file);
                     XWPFDocument doc = new XWPFDocument(fis)) {

                    // 添加文档间分隔（空行）
                    if (i > 0) {
                        for (int j = 0; j < 3; j++) {
                            mergedDoc.createParagraph().createRun().addBreak();
                        }
                    }

                    // 复制所有段落
                    for (XWPFParagraph para : doc.getParagraphs()) {
                        XWPFParagraph newPara = mergedDoc.createParagraph();
                        copyParagraph(para, newPara);
                    }

                    // 复制所有表格
                    for (XWPFTable table : doc.getTables()) {
                        XWPFTable newTable = mergedDoc.createTable();
                        copyTable(table, newTable);
                    }
                }
            }

            // 保存合并后的文档
            String outputDir = apiConfig.getFileScan().getOutputDir();
            new File(outputDir).mkdirs();

            String mergedName = "merged_" + System.currentTimeMillis() + ".docx";
            File mergedFile = new File(outputDir, mergedName);

            try (FileOutputStream out = new FileOutputStream(mergedFile)) {
                mergedDoc.write(out);
            }

            log.info("合并完成，生成文件: {} ({}字节)",
                    mergedFile.getName(), mergedFile.length());

            return mergedFile;

        } catch (Exception e) {
            log.error("文档合并过程中出错: {}", e.getMessage());
            return null;
        }
    }

    private void copyParagraph(XWPFParagraph source, XWPFParagraph target) {
        // 复制段落样式
        if (source.getCTP() != null && source.getCTP().getPPr() != null) {
            target.getCTP().setPPr(source.getCTP().getPPr());
        }

        // 复制文本和样式
        for (XWPFRun run : source.getRuns()) {
            XWPFRun newRun = target.createRun();
            if (run.getCTR() != null && run.getCTR().getRPr() != null) {
                newRun.getCTR().setRPr(run.getCTR().getRPr());
            }
            if (run.getText(0) != null) {
                newRun.setText(run.getText(0));
            }
        }
    }

    private void copyTable(XWPFTable source, XWPFTable target) {
        // 复制表格属性
        if (source.getCTTbl().getTblPr() != null) {
            target.getCTTbl().setTblPr((CTTblPr) source.getCTTbl().getTblPr().copy());
        }

        // 复制行和单元格
        for (int i = 0; i < source.getRows().size(); i++) {
            XWPFTableRow sourceRow = source.getRow(i);
            XWPFTableRow targetRow = i < target.getRows().size() ?
                    target.getRow(i) : target.createRow();

            // 复制行属性
            if (sourceRow.getCtRow().getTrPr() != null) {
                targetRow.getCtRow().setTrPr((CTTrPr) sourceRow.getCtRow().getTrPr().copy());
            }

            for (int j = 0; j < sourceRow.getTableCells().size(); j++) {
                XWPFTableCell sourceCell = sourceRow.getCell(j);
                XWPFTableCell targetCell = targetRow.getCell(j);
                if (targetCell == null) {
                    targetCell = targetRow.createCell();
                }

                // 复制单元格属性
                if (sourceCell.getCTTc().getTcPr() != null) {
                    targetCell.getCTTc().setTcPr((CTTcPr) sourceCell.getCTTc().getTcPr().copy());
                }

                // 复制单元格内容
                for (XWPFParagraph para : sourceCell.getParagraphs()) {
                    XWPFParagraph newPara = targetCell.addParagraph();
                    copyParagraph(para, newPara);
                }
            }
        }
    }

    private void processMergedDocument(File mergedFile, String folderName) {
        try {
            // 知识库处理逻辑
            if (currentKnowledgeId == null ||
                    currentKnowledgeFileCount >= apiConfig.getKnowledgeBaseMaxFiles()) {
                createNewKnowledgeBase();
            }

            // 上传到知识库
            deepVisionService.uploadFileCreateUnit(currentKnowledgeId, mergedFile.getAbsolutePath());
            currentKnowledgeFileCount++;

            // 备份文件
            String backupPath = apiConfig.getFileScan().getBackupDir() +
                    "/" + folderName + "_merged.docx";
            FileUtil.backupFile(mergedFile.getAbsolutePath(), backupPath);

            // 记录日志
            FileUploadRecord record = new FileUploadRecord();
            record.setFileName(mergedFile.getName());
            record.setFilePath(mergedFile.getAbsolutePath());
            record.setBackupPath(backupPath);
            record.setUploadTime(new Date());
            record.setKnowledgeId(currentKnowledgeId);
            // saveToDatabase(record);

            log.info("合并文档 {} 已上传到知识库 {}", mergedFile.getName(), currentKnowledgeId);

        } catch (Exception e) {
            log.error("处理合并文档失败: {}", e.getMessage());
            if (currentKnowledgeFileCount >= apiConfig.getKnowledgeBaseMaxFiles()) {
                resetKnowledgeTracking();
            }
        }
    }

    // 原有方法保持不变
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