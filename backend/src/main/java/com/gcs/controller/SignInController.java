package com.gcs.controller;

import com.gcs.annotation.IgnoreAuth;
import com.gcs.service.SignInService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.gcs.utils.R;
import com.gcs.service.PointsService;

import java.util.HashMap;
import java.util.Map;

/**
 * 签到控制器
 */
@Slf4j
@Tag(name = "签到管理", description = "签到相关的 RESTful API 接口")
@RestController
@RequestMapping("/sign-in")
public class SignInController {
    
    @Autowired
    private SignInService signInService;
    
    @Autowired
    private PointsService pointsService;

    /**
     * 用户签到
     */
    @Operation(summary = "用户签到", description = "用户每日签到获取积分，连续签到有额外奖励")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "签到成功"),
        @ApiResponse(responseCode = "400", description = "参数错误"),
        @ApiResponse(responseCode = "500", description = "服务器错误")
    })
    @PostMapping
    public R doSignIn(HttpServletRequest request) {
        try {
            Long userId = getCurrentUserId(request);
            if (userId == null) {
                return R.error("请先登录");
            }
            
            // 检查今日是否已签到
            if (signInService.hasSignedInToday(userId)) {
                return R.error("今日已签到");
            }
            
            Map<String, Object> result = signInService.doSignIn(userId);
            return R.ok("签到成功").put("data", result);
        } catch (Exception e) {
            log.error("签到失败", e);
            return R.error(e.getMessage());
        }
    }

    /**
     * 检查今日签到状态
     */
    @Operation(summary = "检查今日签到状态", description = "查询当前用户今日是否已签到")
    @GetMapping("/status")
    @IgnoreAuth
    public R getSignInStatus(HttpServletRequest request) {
        try {
            Long userId = getCurrentUserId(request);
            if (userId == null) {
                Map<String, Boolean> data = new HashMap<>();
                data.put("signedIn", false);
                return R.ok().put("data", data);
            }
            
            boolean signedIn = signInService.hasSignedInToday(userId);
            int streak = signInService.getSignInStreak(userId);
            
            Map<String, Object> data = new HashMap<>();
            data.put("signedIn", signedIn);
            data.put("streak", streak);
            
            return R.ok().put("data", data);
        } catch (Exception e) {
            log.error("获取签到状态失败", e);
            return R.error("获取签到状态失败");
        }
    }

    /**
     * 获取当前用户积分
     */
    @Operation(summary = "获取当前用户积分", description = "查询当前用户的积分余额和连续签到天数")
    @GetMapping("/points")
    @IgnoreAuth
    public R getUserPoints(HttpServletRequest request) {
        try {
            Long userId = getCurrentUserId(request);
            if (userId == null) {
                return R.error("请先登录");
            }
            
            Integer points = pointsService.getUserPoints(userId);
            int streak = signInService.getSignInStreak(userId);
            
            Map<String, Object> data = new HashMap<>();
            data.put("points", points);
            data.put("streak", streak);
            
            return R.ok().put("data", data);
        } catch (Exception e) {
            log.error("获取积分失败", e);
            return R.error("获取积分失败");
        }
    }

    /**
     * 获取积分流水
     */
    @Operation(summary = "获取积分流水", description = "分页查询用户的积分变动记录")
    @GetMapping("/transactions")
    @IgnoreAuth
    public R getTransactions(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer limit,
            HttpServletRequest request) {
        try {
            Long userId = getCurrentUserId(request);
            if (userId == null) {
                return R.error("请先登录");
            }
            
            Map<String, Object> params = new HashMap<>();
            params.put("page", page);
            params.put("limit", limit);
            params.put("userId", userId);
            
            return R.ok().put("data", pointsService.queryPage(params));
        } catch (Exception e) {
            log.error("获取积分流水失败", e);
            return R.error("获取积分流水失败");
        }
    }

    /**
     * 获取当前登录用户 ID
     */
    private Long getCurrentUserId(HttpServletRequest request) {
        Object attribute = request.getSession().getAttribute("userId");
        return attribute != null ? Long.parseLong(attribute.toString()) : null;
    }
}
