package com.gcs.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.gcs.enums.FeedbackStatus;
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

import com.gcs.dao.FeedbackDao;
import com.gcs.entity.Feedback;
import com.gcs.service.FeedbackService;
import com.gcs.entity.view.FeedbackView;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.util.StringUtils;

import lombok.extern.slf4j.Slf4j;

/**
 * 用户反馈服务实现类
 * 提供反馈相关的业务逻辑处理
 * @author 
 * @date 2026-04-16
 */
@Slf4j
@Service("feedbackService")
public class FeedbackServiceImpl extends ServiceImpl<FeedbackDao, Feedback> implements FeedbackService {

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        validateParams(params);
        
        IPage<Feedback> feedbackPage = new Query<Feedback>(params).getPage();
        IPage<Feedback> resultPage = this.page(feedbackPage, new QueryWrapper<>());
        
        return new PageUtils(resultPage);
    }
    
    @Override
    public PageUtils queryPage(Map<String, Object> params, Wrapper<Feedback> queryWrapper) {
        validateQueryParams(params, queryWrapper);
        
        IPage<FeedbackView> feedbackViewPage = new Query<FeedbackView>(params).getPage();
        List<FeedbackView> feedbackViews = baseMapper.selectListView(feedbackViewPage, queryWrapper);
        feedbackViewPage.setRecords(feedbackViews);
        
        return new PageUtils(feedbackViewPage);
    }
    
    @Override
    public List<FeedbackView> selectListView(Wrapper<Feedback> queryWrapper) {
        validateWrapper(queryWrapper);
        return baseMapper.selectListView(queryWrapper);
    }

    @Override
    public FeedbackView selectView(Wrapper<Feedback> queryWrapper) {
        validateWrapper(queryWrapper);
        return baseMapper.selectView(queryWrapper);
    }

    @Override
    public boolean createFeedback(Feedback feedback) {
        validateFeedbackForCreate(feedback);
        
        // 设置默认值
        feedback.setCreateTime(LocalDateTime.now());
        feedback.setStatus(FeedbackStatus.PENDING); // 默认待处理状态
        
        return this.save(feedback);
    }

    @Override
    public boolean replyFeedback(Long feedbackId, String replyContent, String handlerAccount) {
        if (feedbackId == null || !StringUtils.hasText(replyContent)) {
            throw new IllegalArgumentException("参数不能为空");
        }

        Feedback feedback = this.getById(feedbackId);
        if (feedback == null) {
            throw new RuntimeException("反馈不存在");
        }

        feedback.setReplyContent(replyContent);
        feedback.setReplyTime(LocalDateTime.now());
        feedback.setStatus(FeedbackStatus.REPLIED); // 已回复状态
        feedback.setUpdateTime(LocalDateTime.now());
        
        return this.updateById(feedback);
    }

    @Override
    public boolean updateStatus(Long feedbackId, Integer status) {
        if (feedbackId == null || status == null) {
            throw new IllegalArgumentException("参数不能为空");
        }

        Feedback feedback = this.getById(feedbackId);
        if (feedback == null) {
            throw new RuntimeException("反馈不存在");
        }
        FeedbackStatus feedbackStatus = FeedbackStatus.valueOf(status);
        feedback.setStatus(feedbackStatus);
        feedback.setUpdateTime(LocalDateTime.now());
        
        return this.updateById(feedback);
    }

    @Override
    public boolean batchUpdateStatus(List<Long> feedbackIds, Integer status) {
        if (CollectionUtils.isEmpty(feedbackIds) || status == null) {
            throw new IllegalArgumentException("参数不能为空");
        }

        int result = baseMapper.updateStatusBatch(feedbackIds, status);
        return result > 0;
    }

    @Override
    public Integer countByStatus(Integer status) {
        if (status == null) {
            return 0;
        }
        
        return baseMapper.countByStatus(status);
    }

    @Override
    public PageUtils getUserFeedbacks(String nickname, Map<String, Object> params) {
        if (!StringUtils.hasText(nickname)) {
            throw new IllegalArgumentException("用户名不能为空");
        }
        
        LambdaQueryWrapper<Feedback> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Feedback::getUserNickname, nickname);
        
        return queryPage(params, queryWrapper);
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
    private void validateWrapper(Wrapper<Feedback> queryWrapper) {
        if (Objects.isNull(queryWrapper)) {
            throw new IllegalArgumentException("查询条件不能为空");
        }
    }

    /**
     * 验证查询参数和条件
     */
    private void validateQueryParams(Map<String, Object> params, Wrapper<Feedback> queryWrapper) {
        validateParams(params);
        validateWrapper(queryWrapper);
    }

    /**
     * 验证创建反馈参数
     */
    private void validateFeedbackForCreate(Feedback feedback) {
        if (feedback == null) {
            throw new IllegalArgumentException("反馈信息不能为空");
        }
        if (feedback.getUserId()==null) {
            throw new IllegalArgumentException("用户账号不能为空");
        }
        if (!StringUtils.hasText(feedback.getContent())) {
            throw new IllegalArgumentException("反馈内容不能为空");
        }
    }
}
