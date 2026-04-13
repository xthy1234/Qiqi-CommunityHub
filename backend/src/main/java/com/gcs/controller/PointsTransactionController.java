package com.gcs.controller;

import com.gcs.annotation.IgnoreAuth;
import com.gcs.service.PointsService;
import com.gcs.utils.AuthUtils;
import com.gcs.utils.PageUtils;
import com.gcs.utils.R;
import com.gcs.vo.PointsTransactionVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 积分流水控制器
 */
@Slf4j
@Tag(name = "积分流水管理", description = "积分流水查询和管理")
@RestController
@RequestMapping("/points-transactions")
public class PointsTransactionController {

    @Autowired
    private PointsService pointsService;
    
    @Autowired
    private AuthUtils authUtils;

    /**
     * 分页查询积分流水（管理员）
     */
    @Operation(summary = "查询积分流水", description = "分页查询所有用户的积分流水")
    @GetMapping
    public R getTransactions(@RequestParam Map<String, Object> params) {
        try {
            PageUtils page = pointsService.queryPage(params);
            
            // 转换为 VO
            List<PointsTransactionVO> voList = ((List<com.gcs.entity.PointsTransaction>) page.getList())
                .stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
            page.setList(voList);
            
            return R.ok().put("data", page);
        } catch (Exception e) {
            log.error("查询积分流水失败", e);
            return R.error("查询失败");
        }
    }

    /**
     * 查询当前用户的积分流水
     */
    @Operation(summary = "我的积分流水", description = "查询当前登录用户的积分变动记录")
    @GetMapping("/my")
    public R getMyTransactions(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer limit,
            HttpServletRequest request) {
        try {
            Long userId = (Long) request.getSession().getAttribute("userId");
            if (userId == null) {
                return R.error("请先登录");
            }
            
            Map<String, Object> params = new HashMap<>();
            params.put("page", page);
            params.put("limit", limit);
            params.put("userId", userId);
            
            PageUtils pageData = pointsService.queryPage(params);
            
            List<PointsTransactionVO> voList = ((List<com.gcs.entity.PointsTransaction>) pageData.getList())
                .stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
            pageData.setList(voList);
            
            return R.ok().put("data", pageData);
        } catch (Exception e) {
            log.error("查询个人积分流水失败", e);
            return R.error("查询失败");
        }
    }

    /**
     * 获取当前用户积分余额
     */
    @Operation(summary = "我的积分余额", description = "查询当前用户的积分总额")
    @GetMapping("/my/balance")
    public R getMyBalance(HttpServletRequest request) {
        try {
            Long userId = (Long) request.getSession().getAttribute("userId");
            if (userId == null) {
                return R.error("请先登录");
            }
            
            Integer balance = pointsService.getUserPoints(userId);
            return R.ok().put("data", balance != null ? balance : 0);
        } catch (Exception e) {
            log.error("查询积分余额失败", e);
            return R.error("查询失败");
        }
    }

    /**
     * 查询指定用户的积分流水（管理员）
     */
    @Operation(summary = "查询用户积分流水", description = "管理员查询指定用户的积分记录")
    @GetMapping("/user/{userId}")
    public R getUserTransactions(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer limit) {
        try {
            Map<String, Object> params = new HashMap<>();
            params.put("page", page);
            params.put("limit", limit);
            params.put("userId", userId);
            
            PageUtils pageData = pointsService.queryPage(params);
            
            List<PointsTransactionVO> voList = ((List<com.gcs.entity.PointsTransaction>) pageData.getList())
                .stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
            pageData.setList(voList);
            
            return R.ok().put("data", pageData);
        } catch (Exception e) {
            log.error("查询用户积分流水失败", e);
            return R.error("查询失败");
        }
    }

    /**
     * 手动调整用户积分（管理员）
     */
    @Operation(summary = "调整用户积分", description = "管理员手动增减用户积分")
    @PostMapping("/adjust")
    public R adjustPoints(
            @Parameter(description = "用户 ID") @RequestParam Long userId,
            @Parameter(description = "调整金额（正增负减）") @RequestParam Integer amount,
            @Parameter(description = "调整原因") @RequestParam String reason,
            HttpServletRequest request) {
        try {
            // 验证管理员权限
            Long currentUserId = (Long) request.getSession().getAttribute("userId");
            if (currentUserId == null) {
                return R.error("请先登录");
            }
            
            if (!authUtils.isAdmin(currentUserId)) {
                return R.error("无权限执行此操作");
            }
            
            if (amount > 0) {
                pointsService.addPoints(userId, amount, "admin_adjust", null, 
                    "管理员调整：" + reason);
            } else {
                pointsService.deductPoints(userId, Math.abs(amount), "admin_adjust", null, 
                    "管理员扣除：" + reason);
            }
            
            log.info("管理员调整积分，操作用户ID: {}, 调整金额: {}, 原因: {}, 操作人: {}", 
                    userId, amount, reason, currentUserId);
            
            return R.ok("调整成功");
        } catch (Exception e) {
            log.error("调整积分失败", e);
            return R.error("调整失败：" + e.getMessage());
        }
    }

    // ==================== 私有方法 ====================

    private PointsTransactionVO convertToVO(com.gcs.entity.PointsTransaction transaction) {
        PointsTransactionVO vo = new PointsTransactionVO();
        vo.setId(transaction.getId());
        vo.setUserId(transaction.getUserId());
        vo.setAmount(transaction.getAmount());
        vo.setBalance(transaction.getBalance());
        vo.setSource(transaction.getSource());
        vo.setSourceId(transaction.getSourceId());
        vo.setDescription(transaction.getDescription());
        vo.setCreateTime(transaction.getCreateTime());
        return vo;
    }
}
