package com.saibao.invoice.config;

import com.saibao.invoice.service.IDingDirectorySyncService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** 钉钉通讯录每小时定时同步入口。 */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "sebo.dingtalk", name = "enabled", havingValue = "true")
public class DingDirectorySyncScheduler {
    private static final Logger LOGGER = LoggerFactory.getLogger(DingDirectorySyncScheduler.class);
    private final IDingDirectorySyncService syncService;

    @Scheduled(cron = "${sebo.dingtalk.sync-cron:0 0 * * * *}", zone = "${sebo.dingtalk.sync-zone:Asia/Shanghai}")
    public void synchronizeHourly() {
        try {
            syncService.synchronize("SCHEDULED", "system");
        } catch (RuntimeException exception) {
            // 定时线程不能因单次网络异常退出；失败详情已写入同步日志。
            LOGGER.error("钉钉通讯录定时同步失败：{}", exception.getMessage());
        }
    }
}
