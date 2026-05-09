package com.example.exam_support_dtu.config;

import com.example.exam_support_dtu.entity.SystemSetting;
import com.example.exam_support_dtu.repository.SystemSettingRepository;
import com.example.exam_support_dtu.service.AutoTaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.scheduling.support.CronTrigger;

import java.time.Instant;

@Configuration
@RequiredArgsConstructor
public class DynamicSchedulerConfig implements SchedulingConfigurer {

    private final SystemSettingRepository systemSettingRepository;
    private final AutoTaskService autoTaskService;

    @Override
    public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
        taskRegistrar.setScheduler(taskScheduler());

        // 1. Tác vụ Cào dữ liệu (Auto Crawl)
        taskRegistrar.addTriggerTask(
                () -> {
                    try {
                        autoTaskService.autoCrawlTask();
                    } catch (Exception e) {
                        // Lỗi sẽ được bắt ở đây để không làm hỏng Scheduler
                    }
                },
                triggerContext -> {
                    String cron = systemSettingRepository.findById("crawl.cron")
                            .map(SystemSetting::getValue)
                            .orElse("0 0 2 * * ?");
                    CronTrigger trigger = new CronTrigger(cron);
                    return trigger.nextExecution(triggerContext);
                });

        // 2. Tác vụ Nhắc nhở (Auto Reminder)
        taskRegistrar.addTriggerTask(
                () -> {
                    try {
                        autoTaskService.autoReminderTask();
                    } catch (Exception e) {
                        // Lỗi sẽ được bắt ở đây để không làm hỏng Scheduler
                    }
                },
                triggerContext -> {
                    String cron = systemSettingRepository.findById("mail.cron")
                            .map(SystemSetting::getValue)
                            .orElse("0 0 6 * * ?");
                    CronTrigger trigger = new CronTrigger(cron);
                    return trigger.nextExecution(triggerContext);
                });
    }

    @Bean
    public TaskScheduler taskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(5);
        scheduler.setThreadNamePrefix("DynamicScheduler-");
        scheduler.initialize();
        return scheduler;
    }
}
