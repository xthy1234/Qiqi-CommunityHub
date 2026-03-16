package com.gcs.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.gcs.entity.Report;
import com.gcs.entity.view.ReportView;

import org.apache.ibatis.annotations.Param;
import java.util.List;

/**
 * 举报信息数据访问接口
 * 提供举报相关的数据库操作
 * @author 
 * @email 
 * @date 2026-04-16 11:22:09
 */
public interface ReportDao extends BaseMapper<Report> {
	
    /**
     * 查询举报列表视图
     * @param wrapper 查询条件包装器
     * @return 举报视图列表
     */
    List<ReportView> selectListView(@Param("ew") Wrapper<Report> wrapper);

    /**
     * 分页查询举报列表视图
     * @param page 分页对象
     * @param wrapper 查询条件包装器
     * @return 举报视图列表
     */
    List<ReportView> selectListView(IPage<ReportView> page, @Param("ew") Wrapper<Report> wrapper);
	
    /**
     * 查询单个举报视图
     * @param wrapper 查询条件包装器
     * @return 举报视图
     */
    ReportView selectView(@Param("ew") Wrapper<Report> wrapper);

    /**
     * 统计不同审核状态的举报数量
     * @param reviewStatus 审核状态
     * @return 数量
     */
    Integer countByReviewStatus(@Param("reviewStatus") Integer reviewStatus);

    /**
     * 批量更新举报审核状态
     * @param reportIds 举报ID列表
     * @param reviewStatus 审核状态
     * @param reviewerAccount 审核人账号
     * @return 更新结果
     */
    int updateReviewStatusBatch(@Param("reportIds") List<Long> reportIds, 
                               @Param("reviewStatus") Integer reviewStatus,
                               @Param("reviewerAccount") String reviewerAccount);
}
