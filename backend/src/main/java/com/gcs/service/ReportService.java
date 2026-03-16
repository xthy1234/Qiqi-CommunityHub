package com.gcs.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.extension.service.IService;
import com.gcs.entity.Report;
import com.gcs.utils.PageUtils;
import com.gcs.entity.view.ReportView;

import java.util.List;
import java.util.Map;

/**
 * 举报信息服务接口
 * 提供举报相关的业务操作
 * @author 
 * @date 2026-04-16
 */
public interface ReportService extends IService<Report> {

    /**
     * 分页查询举报列表
     *
     * @param params 查询参数
     * @return 分页结果
     */
    PageUtils queryPage(Map<String, Object> params);

    /**
     * 查询举报列表视图
     *
     * @param queryWrapper 查询条件包装器
     * @return 举报视图列表
     */
    List<ReportView> selectListView(Wrapper<Report> queryWrapper);

    /**
     * 查询单个举报视图
     *
     * @param queryWrapper 查询条件包装器
     * @return 举报视图
     */
    ReportView selectView(Wrapper<Report> queryWrapper);

    /**
     * 带条件的分页查询举报列表
     *
     * @param params 查询参数
     * @param queryWrapper 查询条件包装器
     * @return 分页结果
     */
    PageUtils queryPage(Map<String, Object> params, Wrapper<Report> queryWrapper);

    /**
     * 创建举报
     *
     * @param report 举报信息
     * @return 创建结果
     */
    boolean createReport(Report report);

    /**
     * 审核举报
     *
     * @param reportId 举报ID
     * @param reviewStatus 审核状态
     * @param replyContent 回复内容
     * @param reviewerAccount 审核人账号
     * @return 审核结果
     */
    boolean reviewReport(Long reportId, Integer reviewStatus, String replyContent, String reviewerAccount);

    /**
     * 批量审核举报
     *
     * @param reportIds 举报ID列表
     * @param reviewStatus 审核状态
     * @param replyContent 回复内容
     * @param reviewerAccount 审核人账号
     * @return 审核结果
     */
    boolean batchReviewReports(List<Long> reportIds, Integer reviewStatus, String replyContent, String reviewerAccount);

    /**
     * 统计不同审核状态的举报数量
     *
     * @param reviewStatus 审核状态
     * @return 数量
     */
    Integer countByReviewStatus(Integer reviewStatus);

    /**
     * 获取用户举报列表
     *
     * @param userId 用户ID
     * @param params 分页参数
     * @return 举报列表
     */
    PageUtils getUserReports(Long userId, Map<String, Object> params); // 添加这个方法
    
    /**
     * 根据举报ID获取举报详情
     *
     * @param reportId 举报ID
     * @return 举报信息
     */
    Report getReportById(Long reportId);
}

