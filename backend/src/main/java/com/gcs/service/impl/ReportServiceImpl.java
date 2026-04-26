package com.gcs.service.impl;

import com.gcs.entity.Article;
import com.gcs.entity.Report;
import com.gcs.entity.User;
import com.gcs.enums.AuditStatus;
import com.gcs.enums.CommonStatus;
import com.gcs.enums.NotificationType;
import com.gcs.service.ArticleService;
import com.gcs.service.NotificationService;
import com.gcs.service.PointsRuleService;
import com.gcs.service.PointsService;
import com.gcs.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Map;
import java.util.List;
import java.util.Objects;
import java.time.LocalDateTime;
import java.util.HashMap;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.gcs.utils.PageUtils;
import com.gcs.utils.Query;

import com.gcs.dao.ReportDao;
import com.gcs.service.ReportService;
import com.gcs.entity.view.ReportView;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.util.StringUtils;

import lombok.extern.slf4j.Slf4j;

/**
 * 举报信息服务实现类
 * 提供举报相关的业务逻辑处理
 * @author 
 * @date 2026-04-16
 */
@Slf4j
@Service("reportService")
public class ReportServiceImpl extends ServiceImpl<ReportDao, Report> implements ReportService {

    @Autowired
    private UserService userService;
    
    @Autowired
    private ArticleService articleService;
    
    @Autowired
    private NotificationService notificationService;
    
    @Autowired
    private PointsService pointsService;
    
    @Autowired
    private PointsRuleService pointsRuleService;
    
    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        validateParams(params);
        
        IPage<Report> reportPage = new Query<Report>(params).getPage();
        IPage<Report> resultPage = this.page(reportPage, new QueryWrapper<>());
        
        return new PageUtils(resultPage);
    }
    
    @Override
    public PageUtils queryPage(Map<String, Object> params, Wrapper<Report> queryWrapper) {
        validateQueryParams(params, queryWrapper);
        
        IPage<ReportView> reportViewPage = new Query<ReportView>(params).getPage();
        List<ReportView> reportViews = baseMapper.selectListView(reportViewPage, queryWrapper, params);
        reportViewPage.setRecords(reportViews);
        
        return new PageUtils(reportViewPage);
    }
    
    @Override
    public List<ReportView> selectListView(Wrapper<Report> queryWrapper) {
        validateWrapper(queryWrapper);
        return baseMapper.selectListView(queryWrapper);
    }

    @Override
    public ReportView selectView(Wrapper<Report> queryWrapper) {
        validateWrapper(queryWrapper);
        return baseMapper.selectView(queryWrapper);
    }

    @Override
    public boolean createReport(Report report) {
        validateReportForCreate(report);
        report.setReviewStatus(AuditStatus.PENDING.getCode());
        report.setStatus(CommonStatus.ENABLED);
        
        return this.save(report);
    }

    @Override
    public boolean reviewReport(Long reportId, Integer reviewStatus, String replyContent, String reviewerAccount) {
        if (reportId == null || reviewStatus == null) {
            throw new IllegalArgumentException("参数不能为空");
        }

        Report report = this.getById(reportId);
        if (report == null) {
            throw new RuntimeException("举报信息不存在");
        }
        AuditStatus auditStatus = AuditStatus.valueOf(reviewStatus);
        report.setReviewStatus(auditStatus.getCode());
        report.setReplyContent(replyContent);
        
        // 根据账号查询审核人 ID
        User reviewer = userService.getUserByAccount(reviewerAccount);
        if (reviewer != null) {
            report.setReviewerId(reviewer.getId());
        }
        
        report.setReviewTime(LocalDateTime.now());
        report.setUpdateTime(LocalDateTime.now());
        
        return this.updateById(report);
    }

    @Override
    public boolean batchReviewReports(List<Long> reportIds, Integer reviewStatus, String replyContent, Long reviewerId) {
        if (CollectionUtils.isEmpty(reportIds) || reviewStatus == null) {
            throw new IllegalArgumentException("参数不能为空");
        }

        int result = baseMapper.updateReviewStatusBatch(reportIds, reviewStatus, reviewerId);
        return result > 0;
    }

    @Override
    public Integer countByReviewStatus(Integer reviewStatus) {
        if (reviewStatus == null) {
            return 0;
        }
        
        return baseMapper.countByReviewStatus(reviewStatus);
    }

    @Override
    public PageUtils getUserReports(Long userId, Map<String, Object> params) {
        if (userId == null) {
            throw new IllegalArgumentException("用户ID不能为空");
        }
        
        QueryWrapper<Report> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("reporter_id", userId);
        
        return queryPage(params, queryWrapper);
    }

    @Override
    public Report getReportById(Long reportId) {
        if (reportId == null) {
            throw new IllegalArgumentException("举报ID不能为空");
        }
        return this.getById(reportId);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean reviewReportWithAction(Long reportId, Integer reviewStatus, String replyContent, 
                                          String reviewerAccount, String action,
                                          Boolean rewardReporter, Boolean penalizeReportedUser) {
        if (reportId == null || reviewStatus == null) {
            throw new IllegalArgumentException("参数不能为空");
        }

        Report report = this.getById(reportId);
        if (report == null) {
            throw new RuntimeException("举报信息不存在");
        }
        
        AuditStatus auditStatus = AuditStatus.valueOf(reviewStatus);
        report.setReviewStatus(auditStatus.getCode());
        report.setReplyContent(replyContent);
        
        User reviewer = userService.getUserByAccount(reviewerAccount);
        if (reviewer != null) {
            report.setReviewerId(reviewer.getId());
        }
        
        report.setReviewTime(LocalDateTime.now());
        report.setUpdateTime(LocalDateTime.now());
        
        boolean updateResult = this.updateById(report);
        
        if (updateResult && auditStatus == AuditStatus.APPROVED) {
            handleApprovedReport(report, action, rewardReporter, penalizeReportedUser);
        }
        
        return updateResult;
    }
    
    /**
     * 处理审核通过的举报
     */
    private void handleApprovedReport(Report report, String action, 
                                      Boolean rewardReporter, Boolean penalizeReportedUser) {
        try {
            if (StringUtils.hasText(action) && !"IGNORE".equals(action)) {
                handleReportedContent(report, action);
            }
            
            notifyReportedUser(report, action);
            
            notifyReporter(report);
            
            if (rewardReporter != null && rewardReporter) {
                rewardReporterPoints(report.getReporterId(), report.getId());
            }
            
            if (penalizeReportedUser != null && penalizeReportedUser) {
                penalizeReportedUserPoints(report.getReportedUserId(), report.getId());
            }
            
            log.info("举报处理完成，reportId: {}, action: {}", report.getId(), action);
        } catch (Exception e) {
            log.error("处理审核通过的举报失败，reportId: {}", report.getId(), e);
        }
    }
    
    /**
     * 处理被举报内容
     */
    private void handleReportedContent(Report report, String action) {
        if (report.getContentId() == null) {
            log.warn("举报记录无关联内容，reportId: {}", report.getId());
            return;
        }
        
        Article article = articleService.getById(report.getContentId());
        if (article == null) {
            log.warn("被举报文章不存在，contentId: {}", report.getContentId());
            return;
        }
        
        switch (action) {
            case "BLOCK":
                article.setAuditStatus(AuditStatus.BLOCKED);
                article.setAuditReply("因违规被屏蔽：" + report.getReplyContent());
                articleService.updateById(article);
                log.info("文章已屏蔽，articleId: {}", article.getId());
                break;
                
            case "DELETE":
                articleService.removeById(article.getId());
                log.info("文章已删除，articleId: {}", article.getId());
                break;
                
            case "WARN":
                log.info("仅发送警告，articleId: {}", article.getId());
                break;
                
            default:
                log.warn("未知处理动作：{}", action);
        }
    }
    
    /**
     * 通知被举报人
     */
    private void notifyReportedUser(Report report, String action) {
        if (report.getReportedUserId() == null) {
            return;
        }
        
        String actionDesc = getActionDescription(action);
        
        Map<String, Object> extra = new HashMap<>();
        extra.put("reportId", report.getId());
        extra.put("contentId", report.getContentId());
        extra.put("action", action);
        extra.put("replyContent", report.getReplyContent());
        
        notificationService.createNotification(
            report.getReportedUserId(),
            NotificationType.REPORT_RESULT.getCode(),
            report.getId(),
            null,
            extra
        );
        
        log.info("已通知被举报人，userId: {}, reportId: {}", report.getReportedUserId(), report.getId());
    }
    
    /**
     * 通知举报人
     */
    private void notifyReporter(Report report) {
        if (report.getReporterId() == null) {
            return;
        }
        
        Map<String, Object> extra = new HashMap<>();
        extra.put("reportId", report.getId());
        extra.put("result", "APPROVED");
        extra.put("replyContent", report.getReplyContent());
        
        notificationService.createNotification(
            report.getReporterId(),
            NotificationType.REPORT_RESULT.getCode(),
            report.getId(),
            null,
            extra
        );
        
        log.info("已通知举报人，userId: {}, reportId: {}", report.getReporterId(), report.getId());
    }
    
    /**
     * 奖励举报人积分（基于规则）
     */
    private void rewardReporterPoints(Long reporterId, Long reportId) {
        if (reporterId == null) {
            return;
        }
        
        try {
            pointsService.addPoints(reporterId, "report_reward", reportId, "举报属实奖励");
            log.info("举报人积分奖励成功，userId: {}, ruleKey: report_reward", reporterId);
        } catch (Exception e) {
            log.error("举报人积分奖励失败，userId: {}", reporterId, e);
        }
    }
    
    /**
     * 扣除被举报人积分（基于规则）
     */
    private void penalizeReportedUserPoints(Long reportedUserId, Long reportId) {
        if (reportedUserId == null) {
            return;
        }
        
        try {
            pointsService.deductPoints(reportedUserId, 20, "report_penalty", 
                                      reportId, "举报成立处罚");
            log.info("被举报人积分扣除成功，userId: {}, ruleKey: report_penalty", reportedUserId);
        } catch (Exception e) {
            log.error("被举报人积分扣除失败，userId: {}", reportedUserId, e);
        }
    }
    
    /**
     * 获取处理动作描述
     */
    private String getActionDescription(String action) {
        if (action == null) {
            return "处理";
        }
        
        switch (action) {
            case "BLOCK": return "屏蔽";
            case "DELETE": return "删除";
            case "WARN": return "警告";
            case "IGNORE": return "忽略";
            default: return "处理";
        }
    }

    /**
     * 验证查询参数
     */
    private void validateParams(Map<String, Object> params) {
        if (params == null || params.isEmpty()) {
            throw new IllegalArgumentException("查询参数不能为空");
        }
    }

    /**
     * 验证查询条件包装器
     */
    private void validateWrapper(Wrapper<Report> queryWrapper) {
        if (Objects.isNull(queryWrapper)) {
            throw new IllegalArgumentException("查询条件不能为空");
        }
    }

    /**
     * 验证查询参数和条件
     */
    private void validateQueryParams(Map<String, Object> params, Wrapper<Report> queryWrapper) {
        validateParams(params);
        validateWrapper(queryWrapper);
    }

    /**
     * 验证创建举报参数
     */
    private void validateReportForCreate(Report report) {
        if (report == null) {
            throw new IllegalArgumentException("举报信息不能为空");
        }
        if (!StringUtils.hasText(report.getReportReason())) {
            throw new IllegalArgumentException("举报原因不能为空");
        }
        if (report.getReporterId() == null) {
            throw new IllegalArgumentException("举报人ID不能为空");
        }
    }
}
