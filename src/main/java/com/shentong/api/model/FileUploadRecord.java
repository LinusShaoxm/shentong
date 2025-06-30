package com.shentong.api.model;

import lombok.Data;

import java.util.Date;

@Data
public class FileUploadRecord {
    private String fileName;
    private String filePath;
    private String backupPath;
    private Date uploadTime;
    private String knowledgeId;
    // 其他字段...
}