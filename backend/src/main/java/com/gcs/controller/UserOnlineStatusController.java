package com.gcs.controller;

import com.gcs.service.UserOnlineStatusService;
import com.gcs.utils.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * 用户在线状态控制器
 */
@Slf4j
@Tag(name = "在线状态管理", description = "用户在线状态相关的 RESTful API")
@RestController
@RequestMapping("/online-status")
public class UserOnlineStatusController {
    
    @Autowired
    private UserOnlineStatusService onlineStatusService;
    
    /**
     * 检查用户是否在线
     */
    @GetMapping("/check/{userId}")
    @Operation(summary = "检查用户在线状态", description = "检查指定用户是否在线")
    public R checkUserOnline(
            @Parameter(description = "用户 ID", required = true) 
            @PathVariable("userId") Long userId) {
        try {
            boolean isOnline = onlineStatusService.isOnline(userId);
            
            Map<String, Object> result = new HashMap<>();
            result.put("userId", userId);
            result.put("isOnline", isOnline);
            
            return R.ok().put("data", result);
        } catch (Exception e) {
            log.error("检查用户在线状态失败", e);
            return R.error("查询失败");
        }
    }
    
    /**
     * 获取所有在线用户
     */
    @GetMapping("/list")
    @Operation(summary = "获取在线用户列表", description = "获取当前所有在线用户的 ID 列表")
    public R getOnlineUsers() {
        try {
            Set<Long> onlineUserIds = onlineStatusService.getOnlineUserIds();
            int onlineCount = onlineStatusService.getOnlineCount();
            
            Map<String, Object> result = new HashMap<>();
            result.put("onlineUsers", onlineUserIds);
            result.put("count", onlineCount);
            
            return R.ok().put("data", result);
        } catch (Exception e) {
            log.error("获取在线用户列表失败", e);
            return R.error("获取失败");
        }
    }
    
    /**
     * 获取在线用户数量
     */
    @GetMapping("/count")
    @Operation(summary = "获取在线用户数", description = "获取当前在线用户总数")
    public R getOnlineCount() {
        try {
            int count = onlineStatusService.getOnlineCount();
            return R.ok().put("data", count);
        } catch (Exception e) {
            log.error("获取在线用户数失败", e);
            return R.error("获取失败");
        }
    }
}
