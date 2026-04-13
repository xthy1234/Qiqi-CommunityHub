package com.gcs.controller;


import com.gcs.annotation.IgnoreAuth;
import com.gcs.service.DashboardStatsService;
import com.gcs.utils.AuthUtils;
import com.gcs.utils.R;
import com.gcs.utils.SessionUtils;
import com.gcs.vo.ArticleAuditOverviewVO;
import com.gcs.vo.DashboardStatsVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 仪表盘统计控制器
 */
@Slf4j
@Tag(name = "仪表盘统计管理", description = "系统核心数据统计相关的 RESTful API 接口")
@RestController
@RequestMapping("/stats")
public class DashboardStatsController {
    
    @Autowired
    private DashboardStatsService dashboardStatsService;
    
    @Autowired
    private SessionUtils sessionUtils;
    
    @Autowired
    private AuthUtils authUtils;

    /**
     * 获取仪表盘统计数据
     */
    @GetMapping("/dashboard")
    @Operation(summary = "获取仪表盘统计数据", description = "获取系统核心指标数据，包括用户、文章、评论等统计信息")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "获取成功"),
        @ApiResponse(responseCode = "500", description = "获取失败")
    })
    public R getDashboardStats(HttpServletRequest request) {
        try {
            Long userId = sessionUtils.getCurrentUserId(request);
            if (userId == null) {
                return R.error("请先登录");
            }
            
            if (!authUtils.isAdmin(userId)) {
                return R.error("无管理员权限");
            }
            
            DashboardStatsVO stats = dashboardStatsService.getDashboardStats();
            return R.ok().put("data", stats);
        } catch (Exception e) {
            log.error("获取仪表盘统计数据失败", e);
            return R.error("获取统计数据失败");
        }
    }
    
    /**
     * 获取文章审核概览统计数据
     */
    @GetMapping("/audit-overview")
    @Operation(summary = "获取文章审核概览", description = "获取文章审核相关的统计数据，包括待审核、今日通过/拒绝数量等")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "获取成功"),
        @ApiResponse(responseCode = "401", description = "未登录"),
        @ApiResponse(responseCode = "403", description = "无管理员权限"),
        @ApiResponse(responseCode = "500", description = "获取失败")
    })
    public R getArticleAuditOverview(HttpServletRequest request) {
        try {
            Long userId = sessionUtils.getCurrentUserId(request);
            if (userId == null) {
                return R.error("请先登录");
            }
            
            if (!authUtils.isAdmin(userId)) {
                return R.error("无管理员权限");
            }
            
            ArticleAuditOverviewVO overview = dashboardStatsService.getArticleAuditOverview();
            return R.ok().put("data", overview);
        } catch (Exception e) {
            log.error("获取文章审核概览数据失败", e);
            return R.error("获取统计数据失败");
        }
    }
}
