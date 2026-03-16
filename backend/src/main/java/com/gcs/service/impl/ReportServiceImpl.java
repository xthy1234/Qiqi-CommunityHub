package com.gcs.service.impl;

import com.gcs.entity.Report;
import com.gcs.enums.AuditStatus;
import com.gcs.enums.CommonStatus;
import org.springframework.stereotype.Service;
import java.util.Map;
import java.util.List;
import java.util.Objects;
import java.time.LocalDateTime;

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
        List<ReportView> reportViews = baseMapper.selectListView(reportViewPage, queryWrapper);
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
        
        // 设置默认值
        report.setCreateTime(LocalDateTime.now());
        report.setReportTime(LocalDateTime.now());
        report.setReviewStatus(AuditStatus.PENDING); // 默认待审核状态
        report.setStatus(CommonStatus.ENABLED); // 默认有效
        
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
        report.setReviewStatus(auditStatus);
        report.setReplyContent(replyContent);
        report.setReviewerAccount(reviewerAccount);
        report.setReviewTime(LocalDateTime.now());
        report.setUpdateTime(LocalDateTime.now());
        
        return this.updateById(report);
    }

    @Override
    public boolean batchReviewReports(List<Long> reportIds, Integer reviewStatus, String replyContent, String reviewerAccount) {
        if (CollectionUtils.isEmpty(reportIds) || reviewStatus == null) {
            throw new IllegalArgumentException("参数不能为空");
        }

        int result = baseMapper.updateReviewStatusBatch(reportIds, reviewStatus, reviewerAccount);
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

    // ==================== 私有验证方法 ====================

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
        if (!StringUtils.hasText(report.getContentTitle())) {
            throw new IllegalArgumentException("被举报内容标题不能为空");
        }
        if (!StringUtils.hasText(report.getReportReason())) {
            throw new IllegalArgumentException("举报原因不能为空");
        }
        if (report.getReporterId() == null) {
            throw new IllegalArgumentException("举报人ID不能为空");
        }
    }
}
