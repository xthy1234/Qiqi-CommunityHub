package com.gcs.task;

import com.gcs.service.UserOnlineStatusService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 心跳清理定时任务
 * 定期清理超过阈值未收到心跳的用户
 */
@Slf4j
@Component
public class HeartbeatCleanupTask {

    @Autowired
    private UserOnlineStatusService onlineStatusService;

    /**
     * 每 30 秒执行一次清理
     * 清理超过 60 秒未心跳的用户
     */
    @Scheduled(fixedDelay = 30000)
    public void cleanOfflineUsers() {
        log.debug("⏰ 开始执行心跳清理任务...");
        int cleanedCount = onlineStatusService.cleanupTimeoutUsers(60000);
        log.info("✅ 心跳清理完成，清理超时用户数：{}", cleanedCount);
    }
}
