package com.gcs.controller;

import com.gcs.dto.*;
import com.gcs.entity.Circle;
import com.gcs.enums.CommonStatus;
import com.gcs.service.CircleService;
import com.gcs.service.CircleMemberService;
import com.gcs.utils.PageUtils;
import com.gcs.utils.R;
import com.gcs.enums.CircleMemberRole;
import com.gcs.enums.CircleType;

import com.gcs.vo.CircleInviteLinkVO;
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

import java.util.*;

/**
 * 圈子成员控制器
 * 提供圈子成员相关的 RESTful API 接口
 * @author 
 * @date 2026-03-17
 */
@Slf4j
@Tag(name = "圈子成员管理", description = "圈子成员相关的 RESTful API 接口")
@RestController
@RequestMapping("/circles")
public class CircleMemberController {
    
    @Autowired
    private CircleService circleService;
    
    @Autowired
    private CircleMemberService circleMemberService;

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

    // ==================== 圈子成员接口 ====================

    /**
     * 获取成员列表
     */
    @GetMapping("/{circleId}/members")
    @Operation(summary = "获取成员列表", description = "获取圈子的所有成员（需为成员）")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "获取成功"),
        @ApiResponse(responseCode = "403", description = "无权查看"),
        @ApiResponse(responseCode = "404", description = "圈子不存在")
    })
    public R getMemberList(
        @Parameter(description = "圈子 ID", required = true) 
        @PathVariable("circleId") Long circleId,
        @Parameter(description = "查询参数（page、limit、role）") 
        @RequestParam Map<String, Object> params,
        @Parameter(hidden = true) HttpServletRequest request) {
        try {
            Long currentUserId = getCurrentUserId(request);
            
            // 检查是否是成员
            Boolean isMember = circleMemberService.hasManagePermission(circleId, currentUserId);
            if (!isMember) {
                Integer userRole = circleMemberService.getUserRoleInCircle(circleId, currentUserId);
                if (userRole == null) {
                    return R.error("无权查看，非圈子成员");
                }
            }
            
            PageUtils page = circleMemberService.getMemberPage(circleId, params);
            return R.ok().put("data", page);
        } catch (Exception e) {
            log.error("获取成员列表失败，circleId: {}", circleId, e);
            return R.error(e.getMessage());
        }
    }

    /**
     * 加入圈子
     */
    @PostMapping("/{circleId}/members")
    @Operation(summary = "加入圈子", description = "申请加入公开圈子")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "加入成功"),
        @ApiResponse(responseCode = "400", description = "加入失败"),
        @ApiResponse(responseCode = "403", description = "私密圈子需邀请")
    })
    public R joinCircle(
        @Parameter(description = "圈子 ID", required = true) 
        @PathVariable("circleId") Long circleId,
        @Parameter(description = "加入信息") 
        @RequestBody(required = false) CircleJoinDTO joinDTO,
        @Parameter(hidden = true) HttpServletRequest request) {
        try {
            Long userId = getCurrentUserId(request);
            
            // 获取圈子信息
            Circle circle = circleService.getById(circleId);
            if (circle == null || circle.getStatus() == CommonStatus.DISABLED) {
                return R.error("圈子不存在或已解散");
            }
            
            // 检查是否已是成员
            Boolean isMember = circleMemberService.isMember(circleId, userId);
            if (isMember != null && isMember) {
                return R.error("已是圈子成员");
            }
            
            // 私密圈子需要邀请
            if (circle.getType() == CircleType.PRIVATE.getCode()) {
                // 简化处理：如果有邀请码则允许加入
                if (joinDTO != null && joinDTO.getInviteCode() != null) {
                    // TODO: 验证邀请码
                    log.info("用户使用邀请码{}加入私密圈子{}", joinDTO.getInviteCode(), circleId);
                } else {
                    return R.error("私密圈子需要邀请才能加入");
                }
            }
            
            // 添加成员（默认为普通成员）
            circleMemberService.addMember(circleId, userId, CircleMemberRole.MEMBER.getCode());
            return R.ok("加入成功");
        } catch (Exception e) {
            log.error("加入圈子失败，circleId: {}", circleId, e);
            return R.error(e.getMessage());
        }
    }

    /**
     * 退出圈子
     */
    @DeleteMapping("/{circleId}/members/self")
    @Operation(summary = "退出圈子", description = "成员自行退出圈子")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "退出成功"),
        @ApiResponse(responseCode = "400", description = "圈主不能退出"),
        @ApiResponse(responseCode = "404", description = "圈子不存在")
    })
    public R leaveCircle(
        @Parameter(description = "圈子 ID", required = true) 
        @PathVariable("circleId") Long circleId,
        @Parameter(hidden = true) HttpServletRequest request) {
        try {
            Long userId = getCurrentUserId(request);
            circleMemberService.leaveCircle(circleId, userId);
            return R.ok("退出成功");
        } catch (Exception e) {
            log.error("退出圈子失败，circleId: {}", circleId, e);
            return R.error(e.getMessage());
        }
    }

    /**
     * 移除成员
     */
    @DeleteMapping("/{circleId}/members/{userId}")
    @Operation(summary = "移除成员", description = "管理员或圈主移除成员")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "移除成功"),
        @ApiResponse(responseCode = "403", description = "无权限"),
        @ApiResponse(responseCode = "404", description = "圈子或用户不存在")
    })
    public R removeMember(
        @Parameter(description = "圈子 ID", required = true) 
        @PathVariable("circleId") Long circleId,
        @Parameter(description = "用户 ID", required = true) 
        @PathVariable("userId") Long userId,
        @Parameter(hidden = true) HttpServletRequest request) {
        try {
            Long currentUserId = getCurrentUserId(request);
            
            // 检查是否有管理权限
            if (!circleMemberService.hasManagePermission(circleId, currentUserId)) {
                return R.error("无权限移除成员");
            }
            
            // 不能移除圈主
            Integer targetRole = circleMemberService.getUserRoleInCircle(circleId, userId);
            if (targetRole != null && targetRole == CircleMemberRole.OWNER.getCode()) {
                return R.error("不能移除圈主");
            }
            
            circleMemberService.removeMember(circleId, userId);
            return R.ok("移除成功");
        } catch (Exception e) {
            log.error("移除成员失败，circleId: {}, userId: {}", circleId, userId, e);
            return R.error(e.getMessage());
        }
    }

    /**
     * 任命/撤销管理员
     */
    @PutMapping("/{circleId}/members/{userId}/role")
    @Operation(summary = "更新成员角色", description = "圈主任命或撤销管理员")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "更新成功"),
        @ApiResponse(responseCode = "403", description = "无权限"),
        @ApiResponse(responseCode = "400", description = "更新失败")
    })
    public R updateMemberRole(
        @Parameter(description = "圈子 ID", required = true) 
        @PathVariable("circleId") Long circleId,
        @Parameter(description = "用户 ID", required = true) 
        @PathVariable("userId") Long userId,
        @Parameter(description = "角色信息", required = true) 
        @Valid @RequestBody CircleMemberRoleUpdateDTO roleDTO,
        @Parameter(hidden = true) HttpServletRequest request) {
        try {
            Long currentUserId = getCurrentUserId(request);
            
            // 获取当前用户角色
            Integer currentUserRole = circleMemberService.getUserRoleInCircle(circleId, currentUserId);
            if (currentUserRole == null || currentUserRole != CircleMemberRole.OWNER.getCode()) {
                return R.error("只有圈主可以任命管理员");
            }
            
            // 不能修改圈主角色
            Integer targetRole = circleMemberService.getUserRoleInCircle(circleId, userId);
            if (targetRole == null) {
                return R.error("用户不是圈子成员");
            }
            if (targetRole == CircleMemberRole.OWNER.getCode()) {
                return R.error("不能修改圈主角色");
            }
            
            // 更新角色（只能是管理员或成员）
            Integer newRole = roleDTO.getRole();
            if (newRole != CircleMemberRole.ADMIN.getCode() && 
                newRole != CircleMemberRole.MEMBER.getCode()) {
                return R.error("只能设置为管理员或成员");
            }
            
            circleMemberService.updateMemberRole(circleId, userId, newRole);
            return R.ok("更新成功");
        } catch (Exception e) {
            log.error("更新成员角色失败，circleId: {}, userId: {}", circleId, userId, e);
            return R.error(e.getMessage());
        }
    }

    /**
     * 邀请成员
     */
    @PostMapping("/{circleId}/invite")
    @Operation(summary = "邀请成员", description = "管理员或圈主邀请用户加入")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "邀请成功"),
        @ApiResponse(responseCode = "403", description = "无权限"),
        @ApiResponse(responseCode = "400", description = "邀请失败")
    })
    public R inviteMember(
        @Parameter(description = "圈子 ID", required = true) 
        @PathVariable("circleId") Long circleId,
        @Parameter(description = "邀请信息", required = true) 
        @Valid @RequestBody CircleInviteDTO inviteDTO,
        @Parameter(hidden = true) HttpServletRequest request) {
        try {
            Long currentUserId = getCurrentUserId(request);
            
            // 检查是否有管理权限
            if (!circleMemberService.hasManagePermission(circleId, currentUserId)) {
                return R.error("无权限邀请成员");
            }
            
            // 检查用户是否已是成员
            Boolean isMember = circleMemberService.isMember(circleId, inviteDTO.getUserId());
            if (isMember != null && isMember) {
                return R.error("该用户已是圈子成员");
            }
            
            // 生成邀请码（简化实现，实际应该存储在数据库）
            String inviteCode = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
            String inviteLink = String.format("http://localhost:8080/circles/%d/join?code=%s", circleId, inviteCode);
            
            // 构建响应
            CircleInviteLinkVO linkVO = new CircleInviteLinkVO();
            linkVO.setInviteLink(inviteLink);
            linkVO.setInviteCode(inviteCode);
            linkVO.setExpireTime(java.time.LocalDateTime.now().plusDays(7)
                .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            
            return R.ok("邀请成功").put("data", linkVO);
        } catch (Exception e) {
            log.error("邀请成员失败，circleId: {}", circleId, e);
            return R.error(e.getMessage());
        }
    }
}
