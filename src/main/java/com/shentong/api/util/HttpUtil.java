// src/main/java/com/example/deepvision/util/HttpUtil.java
package com.shentong.api.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

@Slf4j
public class HttpUtil {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static String mapToJson(Map<String, String> map) {
        try {
            return objectMapper.writeValueAsString(map);
        } catch (JsonProcessingException e) {
            log.error("Map转JSON失败 json: {},exception:", map, e);
            throw new RuntimeException("Map转JSON失败", e);
        }
    }

    public static <T> T jsonToObject(String json, Class<T> valueType) {
        try {
            return objectMapper.readValue(json, valueType);
        } catch (JsonProcessingException e) {
            log.error("JSON转对象失败 json: {},exception:", json, e);
            throw new RuntimeException("JSON转对象失败", e);
        }
    }
}