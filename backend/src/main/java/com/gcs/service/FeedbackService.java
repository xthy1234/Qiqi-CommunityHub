package com.gcs.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.extension.service.IService;
import com.gcs.utils.PageUtils;
import com.gcs.entity.Feedback;
import com.gcs.entity.view.FeedbackView;

import java.util.List;
import java.util.Map;

/**
 * 用户反馈服务接口
 * 提供反馈相关的业务操作
 * @author 
 * @date 2026-04-16
 */
public interface FeedbackService extends IService<Feedback> {

    /**
     * 分页查询反馈列表
     *
     * @param params 查询参数
     * @return 分页结果
     */
    PageUtils queryPage(Map<String, Object> params);

    /**
     * 查询反馈列表视图
     *
     * @param queryWrapper 查询条件包装器
     * @return 反馈视图列表
     */
    List<FeedbackView> selectListView(Wrapper<Feedback> queryWrapper);

    /**
     * 查询单个反馈视图
     *
     * @param queryWrapper 查询条件包装器
     * @return 反馈视图
     */
    FeedbackView selectView(Wrapper<Feedback> queryWrapper);

    /**
     * 带条件的分页查询反馈列表
     *
     * @param params 查询参数
     * @param queryWrapper 查询条件包装器
     * @return 分页结果
     */
    PageUtils queryPage(Map<String, Object> params, Wrapper<Feedback> queryWrapper);

    /**
     * 创建反馈
     *
     * @param feedback 反馈信息
     * @return 创建结果
     */
    boolean createFeedback(Feedback feedback);

    /**
     * 回复反馈
     *
     * @param feedbackId 反馈ID
     * @param replyContent 回复内容
     * @param handlerAccount 处理人账号
     * @return 回复结果
     */
    boolean replyFeedback(Long feedbackId, String replyContent, String handlerAccount);

    /**
     * 更新反馈状态
     *
     * @param feedbackId 反馈ID
     * @param status 状态
     * @return 更新结果
     */
    boolean updateStatus(Long feedbackId, Integer status);

    /**
     * 批量更新反馈状态
     *
     * @param feedbackIds 反馈ID列表
     * @param status 状态
     * @return 更新结果
     */
    boolean batchUpdateStatus(List<Long> feedbackIds, Integer status);

    /**
     * 统计不同状态的反馈数量
     *
     * @param status 状态
     * @return 数量
     */
    Integer countByStatus(Integer status);

    /**
     * 获取用户反馈列表
     *
     * @param userAccount 用户账号
     * @param params 分页参数
     * @return 反馈列表
     */
    PageUtils getUserFeedbacks(String userAccount, Map<String, Object> params);
}

