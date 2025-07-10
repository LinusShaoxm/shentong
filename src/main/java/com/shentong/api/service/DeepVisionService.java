package com.shentong.api.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.shentong.api.config.ApiConfig;
import com.shentong.api.util.DateUtil;
import com.shentong.api.util.HttpUtil;

import lombok.extern.slf4j.Slf4j;

import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.*;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
public class DeepVisionService {

    @Autowired
    private ApiConfig apiConfig;

    @Autowired
    private EncryptService encryptService;

    @Autowired
    private RestTemplate restTemplate;

    private static String STATIC_TOKEN = "5XAykdouNUp4dr2LG/TCGPNsSV4VKKgHiROXoGNtTRhb3BKWgZEsoFgBIgT/rfVD0wdHXzKhmSRgGJp1zIhltQ==";

    // 获取token
    public String getToken() {
        String url = apiConfig.getApiBaseUrl() + "/appKey/getToken";
        Date now = new Date();

        Map<String, String> requestData = new HashMap<>();
        requestData.put("appkey", apiConfig.getApiKey());
        requestData.put("secret", apiConfig.getSecret());


        String encryptedRequest = encryptService.sm4Encrypt(apiConfig.getSk(), now,
                HttpUtil.mapToJson(requestData));

        Map<String, String> requestMap = new HashMap<>();
        requestMap.put("data", encryptedRequest);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("date", DateUtil.formatGMT(now));
        headers.set("App-Id", apiConfig.getAk());

        HttpEntity<Map<String, String>> requestEntity = new HttpEntity<>(requestMap, headers);

        log.info("\n\n\n========= 获取token请求 =========  \n 加密前数据:{}\n 加密后数据:{} \n url:{} \n requestEntity:{}\n\n\n", JSONObject.toJSONString(requestData), JSONObject.toJSONString(requestMap), url, JSONObject.toJSONString(requestEntity));

        ResponseEntity<Map> response = restTemplate.postForEntity(url, requestEntity, Map.class);

        log.info("\n\n\n========= 获取token请求接口请求成功 ========= \n response:{}\n\n\n", JSONObject.toJSONString(response));


        if (!response.getStatusCode().is2xxSuccessful()) {
            log.error("获取token失败: {}", response.getStatusCode());
            throw new RuntimeException("获取token失败: " + response.getStatusCode());
        }

        Map<String, Object> responseBody = response.getBody();

        log.info("获取token接口返回Body:{}", JSONObject.toJSONString(responseBody));

        if (responseBody == null || !"bizSuccess".equals(responseBody.get("code"))) {
            log.error("获取token失败: {}", responseBody);
            throw new RuntimeException("获取token失败: " + responseBody);
        }

        String encryptedResponse = (String) responseBody.get("data");
        return encryptService.sm4Decrypt(apiConfig.getSk(), now, encryptedResponse);
    }

    // 创建知识库
    public String createKnowledgeBase(String name, String description) {
        String url = apiConfig.getApiBaseUrl() + "/knowledge/knowledgeAdd";
        Date now = new Date();
        String token = "";
        try {
            // 实际项目中应该缓存token
            token = getToken();
            log.error("创建知识库请求获取token成功 token:{}", token);
        } catch (Exception e) {
            log.error("创建知识库请求获取token异常 使用token样例:{}", STATIC_TOKEN);
            token = STATIC_TOKEN;
        }

        Map<String, String> requestData = new HashMap<>();
        requestData.put("name", name);
        requestData.put("description", description);
        requestData.put("workspace", apiConfig.getWorkspace());
        requestData.put("workspaceId", apiConfig.getWorkspaceId());
        requestData.put("userId", apiConfig.getUserId());
        requestData.put("tenantId", apiConfig.getTenantId());
        requestData.put("ak", apiConfig.getAk());
        requestData.put("sk", apiConfig.getSk());
        requestData.put("appKey", apiConfig.getApiKey());
        requestData.put("secret", apiConfig.getSecret());


        //tenantId：zyxx
        //secret：multibot@@zhiwei
        //userId：chenjiahao@xz
        //appKey：8K1FV-NZLFA-91OP5-4NBJ7-0116
        //ak = 7a04e90ad5e14b53
        //sk = b53a4eb44e2c42d98b0fbda93086337d

        String encryptedRequest = encryptService.sm4Encrypt(apiConfig.getSk(), now,
                HttpUtil.mapToJson(requestData));

        Map<String, String> requestMap = new HashMap<>();
        requestMap.put("data", encryptedRequest);


        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("date", DateUtil.formatGMT(now));
        headers.set("App-Id", apiConfig.getAk());
        headers.set("Authorization", "bearer " + token);

        HttpEntity<Map<String, String>> requestEntity = new HttpEntity<>(requestMap, headers);

        log.info("\n\n\n========= 创建知识库请求 ========= 加密前数据:{} \n 加密后数据:{} \n url:{} \n requestEntity:{} \n\n\n", JSONObject.toJSONString(requestData), JSONObject.toJSONString(requestMap), url, JSONObject.toJSONString(requestEntity));

        ResponseEntity<Map> response = restTemplate.postForEntity(url, requestEntity, Map.class);

        log.info("\n\n\n========= 创建知识库请求接口调用成功 ========= \n response:{} \n\n\n", JSONObject.toJSONString(response));


        if (!response.getStatusCode().is2xxSuccessful()) {
            log.error("创建知识库失败: {}", response.getStatusCode());
            throw new RuntimeException("创建知识库失败: " + response.getStatusCode());
        }

        Map<String, Object> responseBody = response.getBody();
        log.info("创建知识库接口返回Body:{}", JSONObject.toJSONString(responseBody));
        if (responseBody == null || !"bizSuccess".equals(responseBody.get("code"))) {
            log.error("创建知识库失败: {}", responseBody);
            throw new RuntimeException("创建知识库失败: " + responseBody);
        }

        String encryptedResponse = (String) responseBody.get("data");
        String decryptedResponse = encryptService.sm4Decrypt(apiConfig.getSk(), now, encryptedResponse);
        log.info("======= 创建知识库解密结果:{}", decryptedResponse);
        JSONObject decryptedJson = JSON.parseObject(decryptedResponse);
        log.info("======= 创建知识库解密结果转JSON:{}", decryptedJson);
        return decryptedJson.getString("id");
    }

    // 上传文件创建单元
    public void uploadFileCreateUnit(String knowledgeId, String filePath) throws IOException {
        String url = apiConfig.getApiBaseUrl() + "/knowledge/uploadFileCreateUnit";
        Date now = new Date();
        String token; // 实际项目中应该缓存token
        try {
            // 实际项目中应该缓存token
            token = getToken();
            log.error("上传文件创建单元接口获取token成功 token:{}", token);
        } catch (Exception e) {
            log.error("上传文件创建单元接口获取token异常 使用token样例:{}", STATIC_TOKEN);
            token = STATIC_TOKEN;
        }

        Path path = Paths.get(filePath);
        byte[] fileContent = Files.readAllBytes(path);

        ByteArrayResource resource = new ByteArrayResource(fileContent) {
            @Override
            public String getFilename() {
                return path.getFileName().toString();
            }
        };

        //构建 multipart/form-data 请求体
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();

        body.add("file", resource);
        Map<String, String> params = new HashMap<>();
        params.put("userId", apiConfig.getUserId());
        params.put("tenantId", apiConfig.getTenantId());
        params.put("knowledgeId", knowledgeId);
        params.put("ak", apiConfig.getAk());
        params.put("sk", apiConfig.getSk());
        params.put("appKey", apiConfig.getApiKey());
        params.put("secret", apiConfig.getSecret());

        String encryptedParams = encryptService.sm4Encrypt(apiConfig.getSk(), now,
                HttpUtil.mapToJson(params));

        body.add("data", encryptedParams);


        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        headers.set("date", DateUtil.formatGMT(now));
        headers.set("App-Id", apiConfig.getAk());
        headers.set("Authorization", "bearer " + token);

        log.info("\n\n\n========= 上传文件创建单元接口 ========= 加密前数据:{} \n url:{} \n\n\n", JSONObject.toJSONString(params), url);
        HttpEntity<MultiValueMap<String, Object>> multiValueMapHttpEntity = new HttpEntity<>(body, headers);
        ResponseEntity<Map> response = restTemplate.postForEntity(url, multiValueMapHttpEntity , Map.class);

        log.info("\n\n\n========= 上传文件创建单元接口调用成功 ========= \n response:{} \n\n\n", JSONObject.toJSONString(response));

        if (!response.getStatusCode().is2xxSuccessful()) {
            log.error("上传文件创建单元失败: {}", response.getStatusCode());
            throw new RuntimeException("上传文件创建单元失败: " + response.getStatusCode());
        }

        Map<String, Object> responseBody = response.getBody();
        log.info("上传文件创建单元接口返回Body:{}", JSONObject.toJSONString(responseBody));
        if (responseBody == null || !"bizSuccess".equals(responseBody.get("code"))) {
            log.error("上传文件创建单元失败: {}", responseBody);
            throw new RuntimeException("上传文件创建单元失败: " + responseBody);
        }

        String encryptedResponse = (String) responseBody.get("data");
        String decryptedResponse = encryptService.sm4Decrypt(apiConfig.getSk(), now, encryptedResponse);
        log.info("文件 {} 上传并创建单元成功,:{}", filePath, decryptedResponse);
    }
}