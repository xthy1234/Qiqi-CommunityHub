package com.gcs.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.gcs.entity.Feedback;
import com.gcs.entity.view.FeedbackView;

import org.apache.ibatis.annotations.Param;
import java.util.List;

/**
 * 用户反馈数据访问接口
 * 提供反馈相关的数据库操作
 * @author 
 * @email 
 * @date 2026-04-16 11:22:10
 */
public interface FeedbackDao extends BaseMapper<Feedback> {
	
    /**
     * 查询反馈列表视图
     * @param wrapper 查询条件包装器
     * @return 反馈视图列表
     */
    List<FeedbackView> selectListView(@Param("ew") Wrapper<Feedback> wrapper);

    /**
     * 分页查询反馈列表视图
     * @param page 分页对象
     * @param wrapper 查询条件包装器
     * @return 反馈视图列表
     */
    List<FeedbackView> selectListView(IPage<FeedbackView> page, @Param("ew") Wrapper<Feedback> wrapper);
	
    /**
     * 查询单个反馈视图
     * @param wrapper 查询条件包装器
     * @return 反馈视图
     */
    FeedbackView selectView(@Param("ew") Wrapper<Feedback> wrapper);

    /**
     * 统计不同状态的反馈数量
     * @param status 状态
     * @return 数量
     */
    Integer countByStatus(@Param("status") Integer status);

    /**
     * 批量更新反馈状态
     * @param feedbackIds 反馈ID列表
     * @param status 状态
     * @return 更新结果
     */
    int updateStatusBatch(@Param("feedbackIds") List<Long> feedbackIds, @Param("status") Integer status);
}
