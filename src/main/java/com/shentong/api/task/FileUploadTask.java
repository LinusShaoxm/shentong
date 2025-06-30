package com.shentong.api.task;

import com.shentong.api.service.FileScanService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.stereotype.Component;

@Component
@EnableScheduling
public class FileUploadTask implements SchedulingConfigurer {

    @Autowired
    private FileScanService fileScanService;

    @Override
    public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
        // 已经在FileScanService中使用@Scheduled注解
        // 这里可以添加额外的定时任务配置
    }
}