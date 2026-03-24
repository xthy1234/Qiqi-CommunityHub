package com.gcs.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.gcs.annotation.IgnoreAuth;
import com.gcs.dto.MarkReadRequest;
import com.gcs.dto.NotificationCreateDTO;
import com.gcs.entity.Notification;
import com.gcs.enums.NotificationType;
import com.gcs.service.NotificationService;
import com.gcs.utils.PageUtils;
import com.gcs.utils.R;
import com.gcs.vo.NotificationVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 通知控制器
 */
@Slf4j
@Tag(name = "通知管理", description = "通知相关的 RESTful API 接口")
@RestController
@RequestMapping("/notifications")
public class NotificationController {

    @Autowired
    private NotificationService notificationService;

    /**
     * 获取当前用户的通知列表
     */
    @Operation(summary = "获取通知列表", description = "查询当前用户的通知列表（分页）")
    @GetMapping
    public R getNotifications(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer page,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "20") Integer limit,
            @Parameter(description = "是否已读（true:已读，false:未读，null:全部）") @RequestParam(required = false) Boolean isRead,
            HttpServletRequest request) {
        try {
            Long currentUserId = getCurrentUserId(request);
            if (currentUserId == null) {
                return R.error("请先登录");
            }

            List<Notification> notifications = notificationService.getUserNotifications(
                currentUserId, isRead, page, limit);

            List<NotificationVO> voList = notifications.stream()
                    .map(this::convertToVO)
                    .collect(Collectors.toList());

            // 手动构建分页信息
            Map<String, Object> pageInfo = new HashMap<>();
            pageInfo.put("list", voList);
            pageInfo.put("total", notificationService.count(new QueryWrapper<Notification>()
                    .eq("user_id", currentUserId)));
            pageInfo.put("pageSize", limit);
            pageInfo.put("currPage", page);

            return R.ok().put("data", pageInfo);
        } catch (Exception e) {
            log.error("获取通知列表失败", e);
            return R.error("获取通知列表失败");
        }
    }

    /**
     * 获取未读通知数量
     */
    @Operation(summary = "获取未读通知数量", description = "查询当前用户的未读通知数量")
    @GetMapping("/unread-count")
    public R getUnreadCount(HttpServletRequest request) {
        try {
            Long currentUserId = getCurrentUserId(request);
            if (currentUserId == null) {
                return R.error("请先登录");
            }

            long count = notificationService.countUnread(currentUserId);
            return R.ok().put("data", count);
        } catch (Exception e) {
            log.error("获取未读通知数量失败", e);
            return R.error("获取未读通知数量失败");
        }
    }

    /**
     * 批量标记为已读
     */
    @Operation(summary = "标记通知已读", description = "批量将通知标记为已读状态")
    @PutMapping("/mark-read")
    @Transactional
    public R markAsRead(@Valid @RequestBody MarkReadRequest req, 
                       HttpServletRequest request) {
        try {
            Long currentUserId = getCurrentUserId(request);
            if (currentUserId == null) {
                return R.error("请先登录");
            }

            notificationService.markAsRead(currentUserId, req.getNotificationIds());
            return R.ok("操作成功");
        } catch (Exception e) {
            log.error("标记通知已读失败", e);
            return R.error("操作失败");
        }
    }

    /**
     * 一键已读（将所有未读通知标记为已读）
     */
    @Operation(summary = "一键已读", description = "将所有未读通知标记为已读")
    @PutMapping("/mark-all-read")
    @Transactional
    public R markAllAsRead(HttpServletRequest request) {
        try {
            Long currentUserId = getCurrentUserId(request);
            if (currentUserId == null) {
                return R.error("请先登录");
            }

            // 查询所有未读通知 ID
            List<Notification> unreadList = notificationService.getUserNotifications(
                currentUserId, false, 1, 1000);

            List<Long> ids = unreadList.stream()
                    .map(Notification::getId)
                    .collect(Collectors.toList());

            if (!ids.isEmpty()) {
                notificationService.markAsRead(currentUserId, ids);
            }

            return R.ok("操作成功");
        } catch (Exception e) {
            log.error("一键已读失败", e);
            return R.error("操作失败");
        }
    }

    /**
     * 清空通知
     */
    @Operation(summary = "清空通知", description = "清空当前用户的所有通知")
    @DeleteMapping("/clear")
    @Transactional
    public R clearNotifications(HttpServletRequest request) {
        try {
            Long currentUserId = getCurrentUserId(request);
            if (currentUserId == null) {
                return R.error("请先登录");
            }

            notificationService.clearAll(currentUserId);
            return R.ok("清空成功");
        } catch (Exception e) {
            log.error("清空通知失败", e);
            return R.error("清空失败");
        }
    }

    // ==================== 辅助方法 ====================

    private NotificationVO convertToVO(Notification notification) {
        NotificationVO vo = new NotificationVO();
        vo.setId(notification.getId());
        vo.setUserId(notification.getUserId());
        vo.setType(notification.getType());
        vo.setSourceId(notification.getSourceId());
        vo.setContent(notification.getContent());
        vo.setExtra(notification.getExtra());
        vo.setIsRead(notification.getIsRead());
        vo.setCreateTime(notification.getCreateTime());

        // 设置类型描述
        try {
            NotificationType type = NotificationType.valueOfCode(notification.getType());
            vo.setTypeName(type.getDescription());
        } catch (IllegalArgumentException e) {
            vo.setTypeName("未知类型");
        }

        return vo;
    }

    private Long getCurrentUserId(HttpServletRequest request) {
        String userIdStr = (String) request.getSession().getAttribute("userId");
        if (userIdStr == null) {
            return null;
        }
        return Long.parseLong(userIdStr);
    }
}
