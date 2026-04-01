package com.gcs.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.gcs.annotation.IgnoreAuth;
import com.gcs.dto.AdminSendNotificationDTO;
import com.gcs.dto.MarkReadRequest;
import com.gcs.dto.NotificationCreateDTO;
import com.gcs.dto.AdminSendNotificationDTO;
import com.gcs.entity.Category;
import com.gcs.entity.Notification;
import com.gcs.enums.NotificationType;
import com.gcs.service.NotificationService;
import com.gcs.utils.MPUtil;
import com.gcs.utils.PageUtils;
import com.gcs.utils.R;
import com.gcs.utils.AuthUtils;
import com.gcs.vo.NotificationVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.gcs.utils.MPUtil;
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
    
    @Autowired
    private AuthUtils authUtils;

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

    /**
     * 管理员发送通知
     */
    @Operation(summary = "发送通知（管理员）", description = "管理员向指定用户或全员发送系统通知")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "发送成功"),
        @ApiResponse(responseCode = "400", description = "参数错误"),
        @ApiResponse(responseCode = "403", description = "无权限")
    })
    @PostMapping("/send")
    @Transactional
    public R sendNotification(
            @Parameter(description = "通知数据", required = true) @Valid @RequestBody AdminSendNotificationDTO dto,
            HttpServletRequest request) {
        try {
            // 验证管理员权限
            Long currentUserId = getCurrentUserId(request);
            if (currentUserId == null) {
                return R.error("请先登录");
            }
            
            if (!authUtils.isAdmin(currentUserId)) {
                return R.error("无权限执行此操作");
            }

            String adminAccount = getSessionAttribute(request, "account");

            // 构建通知内容
            Map<String, Object> content = new HashMap<>();
            content.put("title", dto.getTitle());
            content.put("content", dto.getContent());
            content.put("linkUrl", dto.getLinkUrl());
            content.put("priority", dto.getPriority());
            content.put("isTop", dto.getIsTop());
            content.put("sender", "system");
            content.put("senderAccount", adminAccount);

            // 合并额外数据
            Map<String, Object> extraData = new HashMap<>();
            if (dto.getExtra() != null) {
                extraData.putAll(dto.getExtra());
            }
            extraData.put("sentTime", LocalDateTime.now().toString());
            extraData.put("adminAccount", adminAccount);

            // 发送通知
            int sentCount;
            if (CollectionUtils.isEmpty(dto.getUserIds())) {
                // 全员广播
                sentCount = notificationService.sendBroadcastNotification(dto.getType(), content, extraData);
            } else {
                // 发送给指定用户
                sentCount = notificationService.sendBatchNotifications(dto.getUserIds(), dto.getType(), content, extraData);
            }

            log.info("管理员发送通知，类型：{}, 接收人数：{}, 操作人：{}", 
                    dto.getType(), sentCount, adminAccount);

            return R.ok("发送成功，共发送 " + sentCount + " 条通知");
        } catch (Exception e) {
            log.error("发送通知失败", e);
            return R.error("发送失败：" + e.getMessage());
        }
    }

    /**
     * 管理员撤回通知
     */
    @Operation(summary = "撤回通知（管理员）", description = "管理员撤回已发送的通知")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "撤回成功"),
        @ApiResponse(responseCode = "404", description = "通知不存在")
    })
    @DeleteMapping("/{id}")
    @Transactional
    public R withdrawNotification(
            @Parameter(description = "通知 ID", required = true) @PathVariable("id") Long notificationId,
            HttpServletRequest request) {
        try {
            // 验证管理员权限
            Long currentUserId = getCurrentUserId(request);
            if (currentUserId == null) {
                return R.error("请先登录");
            }
            
            if (!authUtils.isAdmin(currentUserId)) {
                return R.error("无权限执行此操作");
            }

            String adminAccount = getSessionAttribute(request, "account");

            boolean result = notificationService.withdrawNotification(notificationId);
            if (result) {
                log.info("管理员撤回通知，ID: {}, 操作人：{}", notificationId, adminAccount);
                return R.ok("撤回成功");
            } else {
                return R.error("撤回失败");
            }
        } catch (Exception e) {
            log.error("撤回通知失败，ID: {}", notificationId, e);
            return R.error("撤回失败：" + e.getMessage());
        }
    }

    /**
     * 管理员批量撤回通知
     */
    @Operation(summary = "批量撤回通知（管理员）", description = "管理员批量撤回多个通知")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "撤回成功"),
        @ApiResponse(responseCode = "400", description = "参数错误")
    })
    @PostMapping("/batch-withdraw")
    @Transactional
    public R batchWithdrawNotifications(
            @Parameter(description = "通知 ID 数组", required = true) @RequestBody Long[] notificationIds,
            HttpServletRequest request) {
        try {
            // 验证管理员权限
            Long currentUserId = getCurrentUserId(request);
            if (currentUserId == null) {
                return R.error("请先登录");
            }
            
            if (!authUtils.isAdmin(currentUserId)) {
                return R.error("无权限执行此操作");
            }

            String adminAccount = getSessionAttribute(request, "account");

            if (notificationIds == null || notificationIds.length == 0) {
                return R.error("请选择要撤回的通知");
            }

            List<Long> ids = Arrays.asList(notificationIds);
            int count = notificationService.batchWithdrawNotifications(ids);

            log.info("管理员批量撤回通知，数量：{}, 操作人：{}", count, adminAccount);
            return R.ok("撤回成功，共撤回 " + count + " 条通知");
        } catch (Exception e) {
            log.error("批量撤回通知失败", e);
            return R.error("撤回失败：" + e.getMessage());
        }
    }

    /**
     * 管理员查询通知记录（分页）
     */
    @Operation(summary = "查询通知记录（管理员）", description = "管理员查看所有用户的通知记录")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "查询成功"),
        @ApiResponse(responseCode = "500", description = "查询失败")
    })
    @GetMapping("/records")
    public R getNotificationRecords(
            @Parameter(description = "查询参数") @RequestParam Map<String, Object> params,
            @Parameter(description = "通知查询条件") Notification notification,
    HttpServletRequest request) {
        try {
            // 验证管理员权限
            Long currentUserId = getCurrentUserId(request);
            if (currentUserId == null) {
                return R.error("请先登录");
            }
            
            if (!authUtils.isAdmin(currentUserId)) {
                return R.error("无权限执行此操作");
            }

            String adminAccount = getSessionAttribute(request, "account");

            // 构建查询条件
            QueryWrapper<Notification> queryWrapper = new QueryWrapper<>();
            
            // 支持按类型筛选
            if (params.containsKey("type")) {
                queryWrapper.eq("type", params.get("type"));
            }
            // 支持按用户 ID 筛选
            if (params.containsKey("userId")) {
                queryWrapper.eq("user_id", params.get("userId"));
            }
            // 支持按已读/未读状态筛选
            if (params.containsKey("isRead")) {
                queryWrapper.eq("is_read", "true".equals(params.get("isRead")));
            }
            
            // 默认按创建时间倒序
            queryWrapper.orderByDesc("create_time");

            // 分页查询
            PageUtils page = notificationService.queryPage(params, 
                MPUtil.sort(MPUtil.between(MPUtil.likeOrEq(queryWrapper, notification), params), params));
            
            // 转换为 VO
            List<NotificationVO> voList = ((List<Notification>) page.getList())
                    .stream()
                    .map(this::convertToVO)
                    .collect(Collectors.toList());
            page.setList(voList);

            log.info("管理员查询通知记录，操作人：{}, 结果数：{}", adminAccount, page.getList().size());
            return R.ok().put("data", page);
        } catch (Exception e) {
            log.error("查询通知记录失败", e);
            return R.error("查询失败");
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
        
        // 🔍 添加调试日志
        log.info("🔄 [转换 VO] notificationId: {}, extra: {}", notification.getId(), notification.getExtra());
        if (notification.getExtra() instanceof Map) {
            Map<?, ?> extraMap = (Map<?, ?>) notification.getExtra();
            log.info("   - extra 是 Map，包含 keys: {}", extraMap.keySet());
            Object liker = extraMap.get("liker");
            if (liker instanceof Map) {
                log.info("   - liker 信息：{}", liker);
            }
        }
        
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
        return (Long) request.getSession().getAttribute("userId");
    }

    private String getSessionAttribute(HttpServletRequest request, String attributeName) {
        Object attribute = request.getSession().getAttribute(attributeName);
        return attribute != null ? attribute.toString() : null;
    }
}
