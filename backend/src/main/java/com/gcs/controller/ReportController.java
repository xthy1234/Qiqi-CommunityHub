package com.gcs.controller;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.gcs.enums.AuditStatus;
import com.gcs.enums.CommonStatus;
import com.gcs.service.ArticleService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.gcs.annotation.IgnoreAuth;
import com.gcs.dto.ReportCreateDTO;
import com.gcs.dto.ReportCreateSimpleDTO;
import com.gcs.dto.ReportReviewDTO;
import com.gcs.dto.ReportBatchReviewDTO;
import com.gcs.entity.Article;
import com.gcs.entity.Report;
import com.gcs.entity.view.ReportView;
import com.gcs.service.ArticleService;
import com.gcs.service.ReportService;
import com.gcs.utils.PageUtils;
import com.gcs.utils.R;
import com.gcs.utils.MPUtil;
import com.gcs.vo.ReportVO;
import com.gcs.vo.ReportDetailVO;
import com.gcs.vo.ReportStatisticsVO;
import com.gcs.entity.User;
import com.gcs.service.UserService;
import com.gcs.vo.UserSimpleVO;
import lombok.extern.slf4j.Slf4j;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

/**
 * 举报信息控制器
 * 提供举报相关的 RESTful API 接口
 * @author 
 * @email 
 * @date 2026-04-16 11:22:09
 */
@Slf4j
@Tag(name = "举报信息管理", description = "举报信息相关的 RESTful API 接口")
@RestController
@RequestMapping("/reports")
public class ReportController {
    
    @Autowired
    private ReportService reportService;
    
    @Autowired
    private ArticleService articleService;

    @Autowired
    private UserService userService;
    /**
     * 获取举报分页列表
     */
    @Operation(summary = "获取举报分页列表", description = "分页查询举报信息列表，支持条件筛选")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "获取成功"),
        @ApiResponse(responseCode = "500", description = "获取失败")
    })
    @GetMapping
    public R getPage(
        @Parameter(description = "查询参数") @RequestParam Map<String, Object> params, 
        @Parameter(description = "举报查询条件") Report report,
        @Parameter(hidden = true) HttpServletRequest request) {
        try {
            String userType = getSessionAttribute(request, "tableName");
            if ("user".equals(userType)) {
                Long userId = (Long) request.getSession().getAttribute("userId");
                report.setReporterId(userId);
            }
            
            QueryWrapper<Report> queryWrapper = new QueryWrapper<>();
            PageUtils page = reportService.queryPage(params, 
                MPUtil.sort(MPUtil.between(MPUtil.likeOrEq(queryWrapper, report), params), params));
            
            // 将 Report 转换为 ReportVO，并填充用户信息
            List<ReportVO> voList = ((List<Report>) page.getList())
                .stream()
                .map(this::convertToVOWithUserInfo)
                .collect(Collectors.toList());
            page.setList(voList);
            
            return R.ok().put("data", page);
        } catch (Exception e) {
            log.error("获取举报分页列表失败", e);
            return R.error("获取数据失败");
        }
    }
    
    /**
     * 获取举报详情
     */
    @Operation(summary = "获取举报详情", description = "根据举报 ID 获取详细信息")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "获取成功"),
        @ApiResponse(responseCode = "404", description = "举报信息不存在"),
        @ApiResponse(responseCode = "500", description = "获取失败")
    })
    @GetMapping("/{id}")
    public R getReportInfo(
        @Parameter(description = "举报 ID", required = true) @PathVariable("id") Long id) {
        try {
            Report report = reportService.getById(id);
            if (report == null) {
                return R.error("举报信息不存在");
            }
            
            // 将 Report 转换为 ReportDetailVO，并填充用户信息
            ReportDetailVO vo = convertToDetailVOWithUserInfo(report);
            return R.ok().put("data", vo);
        } catch (Exception e) {
            log.error("获取举报详情失败，ID: {}", id, e);
            return R.error("获取详情失败");
        }
    }

    /**
     * 获取用户举报列表
     */
    @Operation(summary = "获取用户举报列表", description = "根据用户 ID 获取该用户的所有举报记录列表")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "获取成功"),
        @ApiResponse(responseCode = "500", description = "获取失败")
    })
    @GetMapping("/user/{userId}")
    public R getUserReports(
        @Parameter(description = "用户 ID", required = true) @PathVariable("userId") Long userId,
        @Parameter(description = "查询参数") @RequestParam Map<String, Object> params) {
        try {
            PageUtils page = reportService.getUserReports(userId, params);
            
            // 将 Report 转换为 ReportVO，并填充用户信息
            List<ReportVO> voList = ((List<Report>) page.getList())
                .stream()
                .map(this::convertToVOWithUserInfo)
                .collect(Collectors.toList());
            page.setList(voList);
            
            return R.ok().put("data", page);
        } catch (Exception e) {
            log.error("获取用户举报列表失败，userId: {}", userId, e);
            return R.error("获取数据失败");
        }
    }

    /**
     * 统计不同审核状态的举报数量
     */
    @Operation(summary = "统计举报状态数量", description = "根据审核状态统计该状态的举报总数")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "统计成功"),
        @ApiResponse(responseCode = "500", description = "统计失败")
    })
    @GetMapping("/stats/status/{reviewStatus}")
    public R countByReviewStatus(
        @Parameter(description = "审核状态 (0:待审核，1:已通过，2:已拒绝)", required = true) @PathVariable("reviewStatus") Integer reviewStatus) {
        try {
            AuditStatus status = AuditStatus.valueOf(reviewStatus);
            Integer count = reportService.countByReviewStatus(status.getCode());
            return R.ok().put("data", count);
        } catch (Exception e) {
            log.error("统计举报状态失败，reviewStatus: {}", reviewStatus, e);
            return R.error("统计失败");
        }
    }

    /**
     * 创建举报（简化版 - 推荐使用）
     */
    @Operation(summary = "创建举报", description = "用户提交新的举报信息，只需提供内容 ID 和举报原因")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "举报提交成功"),
        @ApiResponse(responseCode = "400", description = "提交失败"),
        @ApiResponse(responseCode = "404", description = "内容不存在")
    })
    @PostMapping
    public R createReportSimple(
        @Parameter(description = "举报信息", required = true) @Valid @RequestBody ReportCreateSimpleDTO createDTO, 
        @Parameter(hidden = true) HttpServletRequest request) {
        try {
            // 获取当前登录用户
            Long currentUserId = (Long) request.getSession().getAttribute("userId");
            if (currentUserId == null) {
                return R.error("请先登录");
            }
            
            // 根据内容类型自动查询被举报内容信息
            String reportType = createDTO.getReportType() != null ? createDTO.getReportType() : "ARTICLE";
            
            Long reportedUserId = null;
            String reportedUserAccount = null;
            String reportedNickName = null;
            String contentTitle = null;
            
            if ("ARTICLE".equals(reportType)) {
                // 查询文章信息
                Article article = articleService.getById(createDTO.getContentId());
                if (article == null) {
                    return R.error("文章不存在");
                }
                
                reportedUserId = article.getAuthorId();
                contentTitle = article.getTitle();
                
                // 查询作者详细信息
                if (reportedUserId != null) {
                    User author = userService.getById(reportedUserId);
                    if (author != null) {
                        reportedUserAccount = author.getAccount();
                        reportedNickName = author.getNickname();
                    } else {
                        log.warn("举报时无法找到作者信息，authorId: {}", reportedUserId);
                    }
                }
            }
            // TODO: 支持其他类型的举报（评论、用户等）
            
            // 构建举报实体
            Report report = new Report();
            report.setContentId(createDTO.getContentId());
            report.setReporterId(currentUserId);
            report.setReportedUserId(reportedUserId);
            report.setReportReason(createDTO.getReportReason());
            report.setReviewStatus(AuditStatus.PENDING);
            report.setStatus(CommonStatus.ENABLED);
            report.setReviewTime(LocalDateTime.now());
            
            boolean result = reportService.createReport(report);
            if (result) {
                log.info("用户举报成功，userId: {}, contentId: {}, reportedUserId: {}", 
                    currentUserId, createDTO.getContentId(), reportedUserId);
                return R.ok("举报提交成功，感谢您的反馈！");
            } else {
                return R.error("提交失败");
            }
        } catch (Exception e) {
            log.error("添加举报失败，userId: {}, dto: {}", 
                request.getSession().getAttribute("userId"), createDTO, e);
            return R.error(e.getMessage());
        }
    }

    /**
     * 审核举报
     */
    @Operation(summary = "审核举报", description = "管理员对举报信息进行审核处理")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "审核成功"),
        @ApiResponse(responseCode = "400", description = "审核失败")
    })
    @PostMapping("/{id}/review")
    public R reviewReport(
        @Parameter(description = "举报 ID", required = true) @PathVariable("id") Long reportId,
        @Parameter(description = "审核状态", required = true) @RequestParam Integer reviewStatus,
        @Parameter(description = "回复内容", required = true) @RequestParam String replyContent,
        @Parameter(hidden = true) HttpServletRequest request) {
        try {
            // 验证审核状态的有效性
            AuditStatus.valueOf(reviewStatus);
            
            String reviewerAccount = getSessionAttribute(request, "account");
            boolean result = reportService.reviewReport(reportId, reviewStatus, replyContent, reviewerAccount);
            if (result) {
                return R.ok("审核成功");
            } else {
                return R.error("审核失败");
            }
        } catch (Exception e) {
            log.error("审核举报失败，ID: {}", reportId, e);
            return R.error(e.getMessage());
        }
    }

    /**
     * 批量审核举报
     */
    @Operation(summary = "批量审核举报", description = "管理员批量审核多个举报信息")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "批量审核成功"),
        @ApiResponse(responseCode = "400", description = "请选择要审核的举报信息"),
        @ApiResponse(responseCode = "500", description = "批量审核失败")
    })
    @PostMapping("/batch-review")
    @Transactional
    public R batchReview(
        @Parameter(description = "批量审核请求", required = true) @Valid @RequestBody ReportBatchReviewDTO batchDTO,
        @Parameter(hidden = true) HttpServletRequest request) {
        try {
            if (batchDTO.getReportIds() == null || batchDTO.getReportIds().length == 0) {
                return R.error("请选择要审核的举报信息");
            }

            Long reviewerId = (Long) request.getSession().getAttribute("id");
            if (reviewerId == null) {
                String reviewerIdStr = getSessionAttribute(request, "id");
                if (reviewerIdStr != null && !reviewerIdStr.isEmpty()) {
                    try {
                        reviewerId = Long.parseLong(reviewerIdStr);
                    } catch (NumberFormatException e) {
                        log.error("解析审核人 ID 失败：{}", reviewerIdStr, e);
                        return R.error("无效的审核人 ID");
                    }
                }
            }
            
            if (reviewerId == null) {
                return R.error("请先登录");
            }
            
            List<Long> reportIdList = Arrays.stream(batchDTO.getReportIds()).collect(Collectors.toList());
            
            boolean result = reportService.batchReviewReports(
                reportIdList, 
                batchDTO.getReviewStatus(), 
                batchDTO.getReplyContent(), 
                reviewerId);
            
            if (result) {
                return R.ok("批量审核成功");
            } else {
                return R.error("批量审核失败");
            }
        } catch (Exception e) {
            log.error("批量审核举报失败", e);
            return R.error("审核失败");
        }
    }

    /**
     * 更新举报信息
     */
    @Operation(summary = "更新举报信息", description = "根据举报 ID 更新举报信息")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "更新成功"),
        @ApiResponse(responseCode = "400", description = "更新失败")
    })
    @PutMapping("/{id}")
    @Transactional
    public R updateReport(
        @Parameter(description = "举报 ID", required = true) @PathVariable("id") Long id, 
        @Parameter(description = "举报信息", required = true) @Valid @RequestBody Report report) {
        try {
            report.setId(id);
            boolean result = reportService.updateById(report);
            if (result) {
                return R.ok("更新成功");
            } else {
                return R.error("更新失败");
            }
        } catch (Exception e) {
            log.error("修改举报信息失败，ID: {}", report.getId(), e);
            return R.error(e.getMessage());
        }
    }

    /**
     * 部分更新举报信息
     */
    @Operation(summary = "部分更新举报信息", description = "根据举报 ID 部分更新举报信息")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "更新成功"),
        @ApiResponse(responseCode = "400", description = "更新失败")
    })
    @PatchMapping("/{id}")
    @Transactional
    public R partialUpdateReport(
        @Parameter(description = "举报 ID", required = true) @PathVariable("id") Long id, 
        @Parameter(description = "举报信息") @RequestBody Report report) {
        try {
            report.setId(id);
            boolean result = reportService.updateById(report);
            if (result) {
                return R.ok("更新成功");
            } else {
                return R.error("更新失败");
            }
        } catch (Exception e) {
            log.error("部分更新举报信息失败，ID: {}", id, e);
            return R.error(e.getMessage());
        }
    }

    /**
     * 删除举报（单个）
     */
    @Operation(summary = "删除举报", description = "根据举报 ID 删除单个举报记录")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "删除成功"),
        @ApiResponse(responseCode = "500", description = "删除失败")
    })
    @DeleteMapping("/{id}")
    public R deleteReport(
        @Parameter(description = "举报 ID", required = true) @PathVariable("id") Long id) {
        try {
            reportService.removeById(id);
            return R.ok("删除成功");
        } catch (Exception e) {
            log.error("删除举报失败，ID: {}", id, e);
            return R.error("删除失败");
        }
    }

    /**
     * 批量删除举报
     */
    @Operation(summary = "批量删除举报", description = "批量删除多个举报记录")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "删除成功"),
        @ApiResponse(responseCode = "400", description = "请选择要删除的举报信息"),
        @ApiResponse(responseCode = "500", description = "删除失败")
    })
    @PostMapping("/batch-delete")
    public R batchDeleteReports(
        @Parameter(description = "举报 ID 数组", required = true) @RequestBody Long[] reportIds) {
        try {
            if (reportIds == null || reportIds.length == 0) {
                return R.error("请选择要删除的举报信息");
            }
            
            boolean result = reportService.removeByIds(Arrays.asList(reportIds));
            if (result) {
                return R.ok("删除成功");
            } else {
                return R.error("删除失败");
            }
        } catch (Exception e) {
            log.error("删除举报失败", e);
            return R.error("删除失败");
        }
    }
    
    /**
     * 统计举报信息总数
     */
    @Operation(summary = "统计举报信息总数", description = "统计系统中的举报总数量")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "统计成功"),
        @ApiResponse(responseCode = "500", description = "统计失败")
    })
    @GetMapping("/count")
    public R getReportCount(
        @Parameter(description = "查询参数") @RequestParam Map<String, Object> params, 
        @Parameter(description = "举报查询条件") Report report, 
        @Parameter(hidden = true) HttpServletRequest request) {
        try {
            String userType = getSessionAttribute(request, "tableName");
            if ("user".equals(userType)) {
                Long userId = (Long) request.getSession().getAttribute("userId");
                report.setReporterId(userId);
            }
            
            QueryWrapper<Report> queryWrapper = new QueryWrapper<>();
            long totalCount = reportService.count(
                MPUtil.sort(MPUtil.between(MPUtil.likeOrEq(queryWrapper, report), params), params));
            return R.ok().put("data", totalCount);
        } catch (Exception e) {
            log.error("统计举报信息总数失败", e);
            return R.error("统计失败");
        }
    }

    // ==================== 私有转换方法 ====================
    
    /**
     * 将 Report 转换为 ReportVO（不包含用户信息）
     */
    private ReportVO convertToVO(Report report) {
        ReportVO vo = new ReportVO();
        vo.setId(report.getId());
        vo.setContentId(report.getContentId());
        vo.setReportedUserId(report.getReportedUserId());
        vo.setReportReason(report.getReportReason());
        vo.setReviewStatus(report.getReviewStatus());
        vo.setStatus(report.getStatus());
        vo.setReportTime(report.getCreateTime());
        vo.setCreateTime(report.getCreateTime());
        return vo;
    }
    
    /**
     * 将 Report 转换为 ReportVO（包含用户信息）
     */
    private ReportVO convertToVOWithUserInfo(Report report) {
        ReportVO vo = convertToVO(report);
        
        // 设置举报人信息
        if (report.getReporterId() != null) {
            User reporter = userService.getById(report.getReporterId());
            if (reporter != null) {
                vo.setReporterUserInfo(convertToUserSimpleVO(reporter));
            }
        }
        
        // 设置审核人信息
        if (report.getReviewerId() != null) {
            User reviewer = userService.getById(report.getReviewerId());
            if (reviewer != null) {
                vo.setReviewUserInfo(convertToUserSimpleVO(reviewer));
            }
        }
        
        return vo;
    }
    
    /**
     * 将 Report 转换为 ReportDetailVO（包含用户信息）
     */
    private ReportDetailVO convertToDetailVOWithUserInfo(Report report) {
        ReportDetailVO vo = new ReportDetailVO();
        vo.setId(report.getId());
        vo.setContentId(report.getContentId());
        vo.setReportedUserId(report.getReportedUserId());
        vo.setReportReason(report.getReportReason());
        vo.setReplyContent(report.getReplyContent());
        vo.setReviewStatus(report.getReviewStatus());
        vo.setStatus(report.getStatus());
        vo.setReportTime(report.getCreateTime());
        vo.setReviewTime(report.getReviewTime());
        vo.setCreateTime(report.getCreateTime());
        vo.setUpdateTime(report.getUpdateTime());
        
        // 设置举报人信息
        if (report.getReporterId() != null) {
            User reporter = userService.getById(report.getReporterId());
            if (reporter != null) {
                vo.setReporterUserInfo(convertToUserSimpleVO(reporter));
            }
        }
        
        // 设置审核人信息
        if (report.getReviewerId() != null) {
            User reviewer = userService.getById(report.getReviewerId());
            if (reviewer != null) {
                vo.setReviewUserInfo(convertToUserSimpleVO(reviewer));
            }
        }
        
        return vo;
    }
    
    /**
     * 将 User 转换为 UserSimpleVO
     */
    private UserSimpleVO convertToUserSimpleVO(User user) {
        UserSimpleVO vo = new UserSimpleVO();
        vo.setId(user.getId());
        vo.setNickname(user.getNickname());
        vo.setAvatar(user.getAvatar());
        return vo;
    }
    
    /**
     * 将 ReportCreateDTO 转换为 Report
     */
    private Report convertToEntity(ReportCreateDTO dto) {
        Report report = new Report();
        report.setContentId(dto.getContentId());
        report.setReportedUserId(dto.getReportedUserId());
        report.setReportReason(dto.getReportReason());
        return report;
    }

    // ==================== 原有辅助方法 ====================

    /**
     * 获取会话属性
     */
    private String getSessionAttribute(HttpServletRequest request, String attributeName) {
        Object attribute = request.getSession().getAttribute(attributeName);
        return attribute != null ? attribute.toString() : null;
    }
}
