package com.shentong.api.cache;

import com.shentong.api.config.ApiConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

@Slf4j
@Component
public class FolderScanCache {

    @Autowired
    private ApiConfig config;

    @PostConstruct
    public void init() throws IOException {
        // 初始化缓存目录
        Path cachePath = Paths.get(config.getFileScan().getCacheDir());
        if (!Files.exists(cachePath)) {
            Files.createDirectories(cachePath);
        }
    }

    /**
     * 检查文件夹是否已扫描过
     */
    public boolean isFolderScanned(String folderPath) {
        try {
            if (config.getFileScan().isCacheOn()) {
                String cacheFileName = generateCacheFileName(folderPath);
                cacheFileName = folderPath.replaceAll("[\\\\/:]", "") + "__"  + cacheFileName;
                Path cacheFilePath = Paths.get(config.getFileScan().getCacheDir(), cacheFileName);
                return Files.exists(cacheFilePath);
            }
            return false;
        } catch (Exception e) {
            // 出错时返回false，确保文件夹会被处理
            return false;
        }
    }

    /**
     * 标记文件夹为已扫描
     */
    public void markFolderAsScanned(String folderPath) throws IOException, NoSuchAlgorithmException {
        if (!config.getFileScan().isCacheOn()) {
            return;
        }
        log.info("folderPath已被扫描过,缓存标记");
        String cacheFileName = generateCacheFileName(folderPath);
        cacheFileName = folderPath.replaceAll("[\\\\/:]", "") + "__" + cacheFileName;
        Path cacheFilePath = Paths.get(config.getFileScan().getCacheDir(), cacheFileName);
        // 创建空标记文件
        Files.write(cacheFilePath, new byte[0], 
                   StandardOpenOption.CREATE, StandardOpenOption.WRITE);
    }

    /**
     * 清除文件夹的扫描标记（强制下次重新扫描）
     */
    public void clearFolderCache(String folderPath) throws IOException, NoSuchAlgorithmException {
        String cacheFileName = generateCacheFileName(folderPath);
        cacheFileName = folderPath.replaceAll("[\\\\/:]", "")  + "__" + cacheFileName;
        Path cacheFilePath = Paths.get(config.getFileScan().getCacheDir(), cacheFileName);
        if (Files.exists(cacheFilePath)) {
            log.info("删除缓存文件:{}",folderPath);
            Files.delete(cacheFilePath);
        }
    }

    /**
     * 生成缓存文件名（使用SHA-256哈希）
     */
    private String generateCacheFileName(String folderPath) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(folderPath.getBytes());
        
        // 转换为十六进制字符串
        StringBuilder hexString = new StringBuilder();
        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }
        
        return hexString.toString();
    }
}