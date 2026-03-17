package com.gcs.controller;

import com.gcs.dto.CircleCreateDTO;
import com.gcs.dto.CircleUpdateDTO;
import com.gcs.dto.CircleDetailVO;
import com.gcs.entity.Circle;
import com.gcs.service.CircleService;
import com.gcs.utils.PageUtils;
import com.gcs.utils.R;

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

import java.util.Map;

/**
 * 圈子控制器
 * 提供圈子相关的 RESTful API 接口
 * @author 
 * @date 2026-03-17
 */
@Slf4j
@Tag(name = "圈子管理", description = "圈子相关的 RESTful API 接口")
@RestController
@RequestMapping("/circles")
public class CircleController {
    
    @Autowired
    private CircleService circleService;

    /**
     * 获取当前登录用户 ID
     */
    private Long getCurrentUserId(HttpServletRequest request) {
        Object userIdObj = request.getSession().getAttribute("userId");
        if (userIdObj == null) {
            throw new RuntimeException("用户未登录");
        }
        return (Long) userIdObj;
    }

    /**
     * 创建圈子
     */
    @PostMapping
    @Operation(summary = "创建圈子", description = "创建新的圈子")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "创建成功"),
        @ApiResponse(responseCode = "400", description = "创建失败"),
        @ApiResponse(responseCode = "401", description = "用户未登录")
    })
    public R createCircle(
        @Parameter(description = "圈子信息", required = true) 
        @Valid @RequestBody CircleCreateDTO createDTO,
        @Parameter(hidden = true) HttpServletRequest request) {
        try {
            Long userId = getCurrentUserId(request);
            Circle circle = circleService.createCircle(createDTO, userId);
            return R.ok("创建成功").put("data", circle);
        } catch (Exception e) {
            log.error("创建圈子失败", e);
            return R.error(e.getMessage());
        }
    }

    /**
     * 获取圈子详情
     */
    @GetMapping("/{id}")
    @Operation(summary = "获取圈子详情", description = "根据圈子 ID 获取详细信息")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "获取成功"),
        @ApiResponse(responseCode = "404", description = "圈子不存在"),
        @ApiResponse(responseCode = "403", description = "无权查看")
    })
    public R getCircleDetail(
        @Parameter(description = "圈子 ID", required = true) 
        @PathVariable("id") Long id,
        @Parameter(hidden = true) HttpServletRequest request) {
        try {
            Long currentUserId = getCurrentUserId(request);
            CircleDetailVO detailVO = circleService.getCircleDetail(id, currentUserId);
            return R.ok().put("data", detailVO);
        } catch (Exception e) {
            log.error("获取圈子详情失败，id: {}", id, e);
            return R.error(e.getMessage());
        }
    }

    /**
     * 更新圈子信息
     */
    @PutMapping("/{id}")
    @Operation(summary = "更新圈子信息", description = "更新圈子信息（仅圈主或管理员）")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "更新成功"),
        @ApiResponse(responseCode = "403", description = "无权限"),
        @ApiResponse(responseCode = "404", description = "圈子不存在")
    })
    public R updateCircle(
        @Parameter(description = "圈子 ID", required = true) 
        @PathVariable("id") Long id,
        @Parameter(description = "圈子信息", required = true) 
        @Valid @RequestBody CircleUpdateDTO updateDTO,
        @Parameter(hidden = true) HttpServletRequest request) {
        try {
            Long userId = getCurrentUserId(request);
            circleService.updateCircle(id, updateDTO, userId);
            return R.ok("更新成功");
        } catch (Exception e) {
            log.error("更新圈子失败，id: {}", id, e);
            return R.error(e.getMessage());
        }
    }

    /**
     * 解散圈子
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "解散圈子", description = "解散圈子（仅圈主）")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "解散成功"),
        @ApiResponse(responseCode = "403", description = "无权限"),
        @ApiResponse(responseCode = "404", description = "圈子不存在")
    })
    public R dissolveCircle(
        @Parameter(description = "圈子 ID", required = true) 
        @PathVariable("id") Long id,
        @Parameter(hidden = true) HttpServletRequest request) {
        try {
            Long userId = getCurrentUserId(request);
            circleService.dissolveCircle(id, userId);
            return R.ok("解散成功");
        } catch (Exception e) {
            log.error("解散圈子失败，id: {}", id, e);
            return R.error(e.getMessage());
        }
    }

    /**
     * 退出圈子
     */
    @PostMapping("/{id}/leave")
    @Operation(summary = "退出圈子", description = "用户退出所在圈子")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "退出成功"),
        @ApiResponse(responseCode = "400", description = "圈主不能退出"),
        @ApiResponse(responseCode = "404", description = "圈子不存在")
    })
    public R leaveCircle(
        @Parameter(description = "圈子 ID", required = true) 
        @PathVariable("id") Long id,
        @Parameter(hidden = true) HttpServletRequest request) {
        try {
            Long userId = getCurrentUserId(request);
            circleService.leaveCircle(id, userId);
            return R.ok("退出成功");
        } catch (Exception e) {
            log.error("退出圈子失败，id: {}", id, e);
            return R.error(e.getMessage());
        }
    }

    /**
     * 获取用户加入的圈子列表
     */
    @GetMapping("/mine")
    @Operation(summary = "获取用户加入的圈子列表", description = "获取当前用户加入的所有圈子")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "获取成功"),
        @ApiResponse(responseCode = "401", description = "用户未登录")
    })
    public R getMyCircles(
        @Parameter(description = "查询参数") @RequestParam Map<String, Object> params,
        @Parameter(hidden = true) HttpServletRequest request) {
        try {
            Long currentUserId = getCurrentUserId(request);
            PageUtils page = circleService.getMyCircles(currentUserId, params);
            return R.ok().put("data", page);
        } catch (Exception e) {
            log.error("获取用户圈子列表失败", e);
            return R.error(e.getMessage());
        }
    }

    /**
     * 发现公开圈子
     */
    @GetMapping("/public")
    @Operation(summary = "发现公开圈子", description = "浏览和搜索公开圈子")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "获取成功")
    })
    public R getPublicCircles(
        @Parameter(description = "查询参数") @RequestParam Map<String, Object> params,
        @Parameter(hidden = true) HttpServletRequest request) {
        try {
            Long currentUserId = null;
            try {
                currentUserId = getCurrentUserId(request);
            } catch (Exception e) {
                // 未登录也可以访问公开圈子
            }
            PageUtils page = circleService.getPublicCircles(params, currentUserId);
            return R.ok().put("data", page);
        } catch (Exception e) {
            log.error("获取公开圈子列表失败", e);
            return R.error(e.getMessage());
        }
    }
}
