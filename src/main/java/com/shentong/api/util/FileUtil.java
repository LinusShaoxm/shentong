// src/main/java/com/example/deepvision/util/FileUtil.java
package com.shentong.api.util;

import org.springframework.util.StreamUtils;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

public class FileUtil {

    public static void backupFile(String sourcePath, String backupPath) throws IOException {
        Path source = Paths.get(sourcePath);
        Path backup = Paths.get(backupPath);
        
        // 创建备份目录
        Path backupDir = backup.getParent();
        if (!Files.exists(backupDir)) {
            Files.createDirectories(backupDir);
        }
        
        // 复制文件
        Files.copy(source, backup, StandardCopyOption.REPLACE_EXISTING);
    }
    
    public static String readFileContent(String filePath) throws IOException {
        File file = new File(filePath);
        try (InputStream inputStream = new FileInputStream(file)) {
            return StreamUtils.copyToString(inputStream, java.nio.charset.StandardCharsets.UTF_8);
        }
    }
}