// src/main/java/com/example/deepvision/DeepVisionApplication.java
package com.shentong.api;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.client.RestTemplate;

@Slf4j
@SpringBootApplication
@EnableScheduling
public class DeepVisionApplication {
    public static void main(String[] args) {
        SpringApplication.run(DeepVisionApplication.class, args);
        log.info("++++++++++++++++++++++++++++++++++");
        log.info("+++ 深瞳文件上传工具 RunSuccess +++++");
        log.info("++++++++++++++++++++++++++++++++++");
    }

    @Bean
    public RestTemplate restTemplate(){
        return new RestTemplate();
    }
}