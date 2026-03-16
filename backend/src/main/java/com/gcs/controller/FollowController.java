package com.gcs.controller;

import com.gcs.dto.FollowCreateDTO;
import com.gcs.service.FollowService;
import com.gcs.utils.R;
import com.gcs.utils.PageUtils;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 用户关注控制器
 * 提供关注相关的 RESTful API 接口
 * @author 
 * @email 
 * @date 2026-03-16
 */
@Slf4j
@Tag(name = "关注管理", description = "关注相关的 RESTful API 接口")
@RestController
@RequestMapping("/follow")
public class FollowController {
    
    @Autowired
    private FollowService followService;

    /**
     * 获取当前登录用户ID
     */
    private Long getCurrentUserId(HttpServletRequest request) {
        Long userId = (Long) request.getSession().getAttribute("userId");
        if (userId == null) {
            throw new RuntimeException("用户未登录");
        }
        return userId;
    }

    /**
     * 关注/取关用户
     */
    @PostMapping
    @Operation(summary = "关注/取关用户", description = "当前登录用户关注或取消关注指定用户")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "操作成功"),
        @ApiResponse(responseCode = "400", description = "操作失败")
    })
    public R followOrUnfollow(
            @Parameter(description = "关注信息", required = true) 
            @Valid @RequestBody FollowCreateDTO followDTO,
            @Parameter(hidden = true) HttpServletRequest request) {
        try {
            Long currentUserId = getCurrentUserId(request);
            
            // 验证操作类型
            if (!"follow".equalsIgnoreCase(followDTO.getAction()) && 
                !"unfollow".equalsIgnoreCase(followDTO.getAction())) {
                return R.error("无效的操作类型，应为 follow 或 unfollow");
            }

            boolean result = followService.followOrUnfollow(
                currentUserId, 
                followDTO.getUserId(), 
                followDTO.getAction()
            );

            if (result) {
                return R.ok("操作成功");
            } else {
                return R.error("操作失败");
            }
        } catch (RuntimeException e) {
            log.error("关注/取关操作失败", e);
            return R.error(e.getMessage());
        } catch (Exception e) {
            log.error("关注/取关操作失败", e);
            return R.error("操作失败");
        }
    }

    /**
     * 获取用户的关注列表
     */
    @GetMapping("/following")
    @Operation(summary = "获取用户的关注列表", description = "查询指定用户关注的人列表")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "获取成功"),
        @ApiResponse(responseCode = "400", description = "获取失败")
    })
    public R getFollowingList(
            @Parameter(description = "用户ID", required = true) @RequestParam Long userId,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer page,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "20") Integer limit) {
        try {
            Map<String, Object> params = Map.of(
                "page", page.toString(),
                "limit", limit.toString()
            );

            PageUtils pageUtils = followService.getFollowingList(params, userId);
            return R.ok().put("data", pageUtils);
        } catch (Exception e) {
            log.error("获取关注列表失败，用户ID: {}", userId, e);
            return R.error("获取数据失败");
        }
    }

    /**
     * 获取用户的粉丝列表
     */
    @GetMapping("/followers")
    @Operation(summary = "获取用户的粉丝列表", description = "查询指定用户的粉丝列表")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "获取成功"),
        @ApiResponse(responseCode = "400", description = "获取失败")
    })
    public R getFollowerList(
            @Parameter(description = "用户ID", required = true) @RequestParam Long userId,
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer page,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "20") Integer limit) {
        try {
            Map<String, Object> params = Map.of(
                "page", page.toString(),
                "limit", limit.toString()
            );

            PageUtils pageUtils = followService.getFollowerList(params, userId);
            return R.ok().put("data", pageUtils);
        } catch (Exception e) {
            log.error("获取粉丝列表失败，用户ID: {}", userId, e);
            return R.error("获取数据失败");
        }
    }

    /**
     * 查询关注状态（批量）
     */
    @GetMapping("/status")
    @Operation(summary = "查询关注状态", description = "查询当前用户对多个目标用户的关注状态")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "获取成功"),
        @ApiResponse(responseCode = "400", description = "获取失败")
    })
    public R getFollowStatus(
            @Parameter(description = "目标用户ID，多个用逗号分隔", required = true) 
            @RequestParam String targetIds,
            @Parameter(hidden = true) HttpServletRequest request) {
        try {
            Long currentUserId = getCurrentUserId(request);
            
            // 解析目标用户ID 列表
            List<Long> targetIdList = Arrays.stream(targetIds.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(Long::parseLong)
                .collect(Collectors.toList());

            if (targetIdList.isEmpty()) {
                return R.error("目标用户ID 不能为空");
            }

            Map<Long, Boolean> statusMap = followService.getFollowStatus(currentUserId, targetIdList);
            return R.ok().put("data", statusMap);
        } catch (Exception e) {
            log.error("查询关注状态失败", e);
            return R.error("获取状态失败");
        }
    }

    /**
     * 获取互关状态（好友关系）
     */
    @GetMapping("/is-friend/{userId}")
    @Operation(summary = "获取互关状态", description = "判断当前用户与指定用户是否为好友")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "获取成功"),
        @ApiResponse(responseCode = "400", description = "获取失败")
    })
    public R isFriend(
            @Parameter(description = "目标用户ID", required = true) 
            @PathVariable("userId") Long userId,
            @Parameter(hidden = true) HttpServletRequest request) {
        try {
            Long currentUserId = getCurrentUserId(request);
            
            boolean isFriend = followService.isFriend(currentUserId, userId);
            return R.ok().put("data", isFriend);
        } catch (Exception e) {
            log.error("获取互关状态失败，目标用户ID: {}", userId, e);
            return R.error("获取状态失败");
        }
    }

    /**
     * 统计关注数量
     */
    @GetMapping("/count/following")
    @Operation(summary = "统计关注数量", description = "统计指定用户的关注数量")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "统计成功"),
        @ApiResponse(responseCode = "400", description = "统计失败")
    })
    public R countFollowing(
            @Parameter(description = "用户ID", required = true) @RequestParam Long userId) {
        try {
            Integer count = followService.countFollowing(userId);
            return R.ok().put("data", count);
        } catch (Exception e) {
            log.error("统计关注数量失败，用户ID: {}", userId, e);
            return R.error("统计失败");
        }
    }

    /**
     * 统计粉丝数量
     */
    @GetMapping("/count/followers")
    @Operation(summary = "统计粉丝数量", description = "统计指定用户的粉丝数量")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "统计成功"),
        @ApiResponse(responseCode = "400", description = "统计失败")
    })
    public R countFollowers(
            @Parameter(description = "用户ID", required = true) @RequestParam Long userId) {
        try {
            Integer count = followService.countFollowers(userId);
            return R.ok().put("data", count);
        } catch (Exception e) {
            log.error("统计粉丝数量失败，用户ID: {}", userId, e);
            return R.error("统计失败");
        }
    }
}
