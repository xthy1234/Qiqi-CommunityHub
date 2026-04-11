package com.gcs.controller;


import com.gcs.annotation.IgnoreAuth;
import com.gcs.dto.DanmakuSendDTO;
import com.gcs.service.VideoDanmakuService;
import com.gcs.utils.AuthUtils;
import com.gcs.utils.R;
import com.gcs.vo.VideoDanmakuDetailVO;
import com.gcs.vo.VideoDanmakuVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Slf4j
@Tag(name = "视频弹幕管理", description = "视频弹幕的发送、查询、管理 API")
@RestController
@RequestMapping("/api/danmaku")
public class VideoDanmakuController {

    @Autowired
    private VideoDanmakuService danmakuService;

    @Autowired
    private AuthUtils authUtils;

    @Operation(summary = "发送弹幕", description = "登录用户发送弹幕，需校验内容和频率")
    @PostMapping
    public R sendDanmaku(
            @Valid @RequestBody DanmakuSendDTO dto,
            HttpServletRequest request) {
        try {
            Long userId = getCurrentUserId(request);
            if (userId == null) {
                return R.error("请先登录");
            }

            String ipAddress = getClientIp(request);
            VideoDanmakuDetailVO result = danmakuService.sendDanmaku(userId, ipAddress, dto);
            return R.ok("弹幕发送成功").put("data", result);
        } catch (IllegalArgumentException e) {
            log.warn("发送弹幕失败: {}", e.getMessage());
            return R.error(e.getMessage());
        } catch (SecurityException e) {
            log.warn("敏感词拦截: {}", e.getMessage());
            return R.error(403, e.getMessage());
        } catch (Exception e) {
            log.error("发送弹幕异常", e);
            return R.error("弹幕发送失败");
        }
    }

    @Operation(summary = "获取视频弹幕", description = "根据文章ID和视频URL获取弹幕列表")
    @GetMapping
    @IgnoreAuth
    public R getDanmaku(
            @Parameter(description = "文章 ID", required = true) @RequestParam Long articleId,
            @Parameter(description = "视频 URL", required = true) @RequestParam String videoUrl,
            @Parameter(description = "开始时间（秒）") @RequestParam(required = false) BigDecimal from,
            @Parameter(description = "结束时间（秒）") @RequestParam(required = false) BigDecimal to) {
        try {
            List<VideoDanmakuDetailVO> list = danmakuService.getDanmakuByTimeRange(articleId, videoUrl, from, to);
            return R.ok().put("data", list);
        } catch (Exception e) {
            log.error("获取弹幕失败", e);
            return R.error("获取弹幕失败");
        }
    }

    @Operation(summary = "获取最新弹幕", description = "获取指定文章下视频的最新弹幕（用于初始加载）")
    @GetMapping("/latest")
    @IgnoreAuth
    public R getLatestDanmaku(
            @Parameter(description = "文章 ID", required = true) @RequestParam Long articleId,
            @Parameter(description = "视频 URL", required = true) @RequestParam String videoUrl,
            @Parameter(description = "数量限制") @RequestParam(required = false, defaultValue = "100") Integer limit) {
        try {
            List<VideoDanmakuVO> list = danmakuService.getLatestDanmaku(articleId, videoUrl, limit);
            return R.ok().put("data", list);
        } catch (Exception e) {
            log.error("获取最新弹幕失败", e);
            return R.error("获取最新弹幕失败");
        }
    }

    @Operation(summary = "屏蔽弹幕", description = "管理员屏蔽违规弹幕")
    @PutMapping("/{danmakuId}/block")
    public R blockDanmaku(
            @Parameter(description = "弹幕 ID", required = true) @PathVariable Long danmakuId,
            HttpServletRequest request) {
        try {
            Long currentUserId = getCurrentUserId(request);
            if (currentUserId == null) {
                return R.error("请先登录");
            }

            if (!authUtils.isAdmin(currentUserId)) {
                return R.error("无管理员权限");
            }

            boolean success = danmakuService.blockDanmaku(danmakuId);
            if (success) {
                log.info("管理员屏蔽弹幕成功，danmakuId={}, 操作人={}", danmakuId, currentUserId);
                return R.ok("弹幕已屏蔽");
            } else {
                return R.error("屏蔽失败");
            }
        } catch (Exception e) {
            log.error("屏蔽弹幕失败", e);
            return R.error("屏蔽弹幕失败");
        }
    }

    @Operation(summary = "分页查询弹幕", description = "管理后台使用，支持多条件筛选")
    @GetMapping("/page")
    public R queryPage(@RequestParam Map<String, Object> params) {
        try {
            return R.ok().put("data", danmakuService.queryPage(params));
        } catch (Exception e) {
            log.error("分页查询失败", e);
            return R.error("分页查询失败");
        }
    }

    @Operation(summary = "统计视频弹幕数量", description = "获取指定文章下视频的弹幕总数")
    @GetMapping("/count")
    @IgnoreAuth
    public R countByVideo(
            @Parameter(description = "文章 ID", required = true) @RequestParam Long articleId,
            @Parameter(description = "视频 URL", required = true) @RequestParam String videoUrl) {
        try {
            Integer count = danmakuService.countByVideo(articleId, videoUrl);
            return R.ok().put("data", count);
        } catch (Exception e) {
            log.error("统计弹幕数量失败", e);
            return R.error("统计失败");
        }
    }

    private Long getCurrentUserId(HttpServletRequest request) {
        Object userId = request.getAttribute("userId");
        if (userId == null) {
            userId = request.getSession().getAttribute("userId");
        }
        return userId != null ? Long.valueOf(userId.toString()) : null;
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}
