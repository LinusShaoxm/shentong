package com.shentong.api.controller;


import com.shentong.api.cache.FolderScanCache;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;

@RestController
@RequestMapping("cache")
public class CacheController {

    @Autowired
    private FolderScanCache folderScanCache;


    @GetMapping("/remove")
    public void remove(String path) throws IOException, NoSuchAlgorithmException {
        folderScanCache.clearFolderCache(path);
    }

}
