package com.shentong.api.service;

import com.shentong.api.config.ApiConfig;
import com.shentong.api.model.ApiResponse;
import com.shentong.api.util.DateUtil;
import com.shentong.api.util.HttpUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

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

    // 获取token
    public String getToken() {
        String url = apiConfig.getApiBaseUrl() + "/appKey/getToken";
        Date now = new Date();

        Map<String, String> requestData = new HashMap<>();
        requestData.put("appkey", apiConfig.getAk());
        requestData.put("secret", apiConfig.getSk());

        String encryptedRequest = encryptService.sm4Encrypt(apiConfig.getSk(), now,
                HttpUtil.mapToJson(requestData));

        Map<String, String> requestMap = new HashMap<>();
        requestMap.put("data", encryptedRequest);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("date", DateUtil.formatGMT(now));
        headers.set("App-Id", apiConfig.getAk());
        headers.set("Authorization", "bearer "); // 实际项目中需要从之前接口获取

        HttpEntity<Map<String, String>> requestEntity = new HttpEntity<>(requestMap, headers);
        ResponseEntity<Map> response = restTemplate.postForEntity(url, requestEntity, Map.class);

        if (!response.getStatusCode().is2xxSuccessful()) {
            log.error("获取token失败: {}", response.getStatusCode());
            throw new RuntimeException("获取token失败: " + response.getStatusCode());
        }

        Map<String, Object> responseBody = response.getBody();
        if (responseBody == null || !"bizSuccess".equals(responseBody.get("code"))) {
            log.error("获取token失败: {}", responseBody);
            throw new RuntimeException("获取token失败: " + responseBody);
        }

        String encryptedResponse = (String) responseBody.get("data");
        String decryptedResponse = encryptService.sm4Decrypt(apiConfig.getSk(), now, encryptedResponse);

        ApiResponse<Map<String, String>> apiResponse = HttpUtil.jsonToObject(decryptedResponse,
                ApiResponse.class);

        if (!"bizSuccess".equals(apiResponse.getCode())) {
            log.error("获取token失败: {}", apiResponse.getMessage());
            throw new RuntimeException("获取token失败: " + apiResponse.getMessage());
        }

        return apiResponse.getData().get("token");
    }

    // 创建知识库
    public String createKnowledgeBase(String name, String description) {
        String url = apiConfig.getApiBaseUrl() + "/knowledge/knowledgeAdd";
        Date now = new Date();
        String token = getToken(); // 实际项目中应该缓存token

        Map<String, String> requestData = new HashMap<>();
        requestData.put("name", name);
        requestData.put("description", description);
        requestData.put("workspace", "personal"); // TODO: 配置化
        requestData.put("workspaceId", "admin");  // TODO: 配置化

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
        ResponseEntity<Map> response = restTemplate.postForEntity(url, requestEntity, Map.class);

        if (!response.getStatusCode().is2xxSuccessful()) {
            log.error("创建知识库失败: {}", response.getStatusCode());
            throw new RuntimeException("创建知识库失败: " + response.getStatusCode());
        }

        Map<String, Object> responseBody = response.getBody();
        if (responseBody == null || !"bizSuccess".equals(responseBody.get("code"))) {
            log.error("创建知识库失败: {}", responseBody);
            throw new RuntimeException("创建知识库失败: " + responseBody);
        }

        String encryptedResponse = (String) responseBody.get("data");
        String decryptedResponse = encryptService.sm4Decrypt(apiConfig.getSk(), now, encryptedResponse);

        ApiResponse<Map<String, String>> apiResponse = HttpUtil.jsonToObject(decryptedResponse,
                ApiResponse.class);

        if (!"bizSuccess".equals(apiResponse.getCode())) {
            log.error("创建知识库失败: {}", apiResponse.getMessage());
            throw new RuntimeException("创建知识库失败: " + apiResponse.getMessage());
        }

        return apiResponse.getData().get("id");
    }

    // 上传文件创建单元
    public void uploadFileCreateUnit(String knowledgeId, String filePath) {
        String url = apiConfig.getApiBaseUrl() + "/knowledge/uploadFileCreateUnit";
        Date now = new Date();
        String token = getToken(); // 实际项目中应该缓存token

        // 准备文件上传
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new FileSystemResource(filePath));

        Map<String, String> params = new HashMap<>();
        params.put("userId", "admin"); // TODO: 配置化
        params.put("tenantId", apiConfig.getTenantId());
        params.put("knowledgeId", knowledgeId);
        // 其他可选参数...

        String encryptedParams = encryptService.sm4Encrypt(apiConfig.getSk(), now,
                HttpUtil.mapToJson(params));

        body.add("data", encryptedParams);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        headers.set("date", DateUtil.formatGMT(now));
        headers.set("App-Id", apiConfig.getAk());
        headers.set("Authorization", "bearer " + token);

        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);
        ResponseEntity<Map> response = restTemplate.postForEntity(url, requestEntity, Map.class);

        if (!response.getStatusCode().is2xxSuccessful()) {
            log.error("上传文件创建单元失败: {}", response.getStatusCode());
            throw new RuntimeException("上传文件创建单元失败: " + response.getStatusCode());
        }

        Map<String, Object> responseBody = response.getBody();
        if (responseBody == null || !"bizSuccess".equals(responseBody.get("code"))) {
            log.error("上传文件创建单元失败: {}", responseBody);
            throw new RuntimeException("上传文件创建单元失败: " + responseBody);
        }

        String encryptedResponse = (String) responseBody.get("data");
        String decryptedResponse = encryptService.sm4Decrypt(apiConfig.getSk(), now, encryptedResponse);

        ApiResponse<Map<String, String>> apiResponse = HttpUtil.jsonToObject(decryptedResponse,
                ApiResponse.class);

        if (!"bizSuccess".equals(apiResponse.getCode())) {
            log.error("上传文件创建单元失败: {}", apiResponse.getMessage());
            throw new RuntimeException("上传文件创建单元失败: " + apiResponse.getMessage());
        }

        log.info("文件 {} 上传并创建单元成功", filePath);
    }
}