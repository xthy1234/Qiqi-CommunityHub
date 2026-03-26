package com.gcs.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.gcs.dao.ArticleDao;
import com.gcs.dao.ArticleEditSuggestionDao;
import com.gcs.entity.ArticleEditSuggestion;
import com.gcs.entity.Article;
import com.gcs.entity.User;
import com.gcs.service.ArticleEditSuggestionService;
import com.gcs.service.NotificationService;
import com.gcs.service.ArticleVersionService;
import com.gcs.service.ArticleContributorService;
import com.gcs.dao.ArticleDao;
import com.gcs.dao.UserDao;
import com.gcs.enums.NotificationType;
import com.gcs.enums.SuggestionStatus;
import com.gcs.utils.NotificationBuilder;
import com.gcs.vo.UserSimpleVO;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.extern.slf4j.Slf4j;

/**
 * 文章修改建议服务实现类
 */
@Slf4j
@Service("articleEditSuggestionService")
public class ArticleEditSuggestionServiceImpl extends ServiceImpl<ArticleEditSuggestionDao, ArticleEditSuggestion> 
        implements ArticleEditSuggestionService {

    @Autowired
    private ArticleEditSuggestionDao articleEditSuggestionDao;

    @Autowired
    private ArticleDao articleDao;

    @Autowired
    private ArticleVersionService articleVersionService;

    @Autowired
    private ArticleContributorService articleContributorService;
    
    @Autowired
    private NotificationService notificationService;
    
    @Autowired
    private UserDao userDao;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submitSuggestion(Long articleId, Long proposerId, String title,
                                Map<String, Object> content, String changeSummary) {
        // 检查文章是否存在
        Article article = articleDao.selectById(articleId);
        if (article == null) {
            throw new IllegalArgumentException("文章不存在");
        }

        // 创建建议记录
        ArticleEditSuggestion suggestion = new ArticleEditSuggestion();
        suggestion.setArticleId(articleId);
        suggestion.setProposerId(proposerId);
        suggestion.setTitle(title);
        suggestion.setContent(content);
        suggestion.setChangeSummary(changeSummary);
        suggestion.setStatus(SuggestionStatus.PENDING.getCode());

        baseMapper.insert(suggestion);

        log.info("提交修改建议成功，suggestionId: {}, articleId: {}", suggestion.getId(), articleId);
        
        // 📢 发送通知给文章作者
        sendSuggestionSubmitNotification(suggestion, article.getAuthorId());
    }

    @Override
    public IPage<ArticleEditSuggestion> getSuggestions(Long articleId, Integer status, 
                                                       Integer page, Integer limit) {
        Page<ArticleEditSuggestion> mpPage = new Page<>(page, limit);
        
        QueryWrapper<ArticleEditSuggestion> wrapper = new QueryWrapper<>();
        wrapper.eq("article_id", articleId);
        if (status != null) {
            wrapper.eq("status", status);
        }
        wrapper.orderByDesc("create_time");

        return baseMapper.selectPage(mpPage, wrapper);
    }

    @Override
    public IPage<ArticleEditSuggestion> getSuggestionsByProposer(Long proposerId, Integer status,
                                                                 Integer page, Integer limit) {
        QueryWrapper<ArticleEditSuggestion> wrapper = new QueryWrapper<>();
        wrapper.eq("proposer_id", proposerId);
        if (status != null) {
            wrapper.eq("status", status);
        }
        wrapper.orderByDesc("create_time");
        
        return baseMapper.selectPage(new Page<>(page, limit), wrapper);
    }

    @Override
    public IPage<ArticleEditSuggestion> getSuggestionsByAuthor(Long authorId, Integer status,
                                                               Integer page, Integer limit) {
        // 需要通过文章关联查询 - 使用 DAO 的方法
        List<ArticleEditSuggestion> suggestions = baseMapper.selectByAuthorId(authorId, status);
        // 手动分页（简化处理）
        int total = suggestions.size();
        int fromIndex = (page - 1) * limit;
        int toIndex = Math.min(fromIndex + limit, total);
        
        if (fromIndex >= total) {
            Page<ArticleEditSuggestion> emptyPage = new Page<>(page, limit);
            emptyPage.setTotal(total);
            return emptyPage;
        }
        
        List<ArticleEditSuggestion> pagedList = suggestions.subList(fromIndex, toIndex);
        Page<ArticleEditSuggestion> resultPage = new Page<>(page, limit);
        resultPage.setRecords(pagedList);
        resultPage.setTotal(total);
        return resultPage;
    }

    @Override
    public Integer countPendingSuggestions(Long articleId) {
        // 直接调用 DAO 层方法
        return baseMapper.countPendingSuggestions(articleId);
    }

    @Override
    public ArticleEditSuggestion getSuggestionDetail(Long suggestionId) {
        ArticleEditSuggestion suggestion = baseMapper.selectById(suggestionId);
        if (suggestion == null) {
            throw new IllegalArgumentException("建议不存在");
        }

        // 获取当前文章内容用于对比
        Article article = articleDao.selectById(suggestion.getArticleId());
        if (article != null) {
            // 将差异信息放入 extra 字段返回（供前端参考）
            Map<String, Object> extra = new HashMap<>();
            extra.put("currentContent", article.getContent());
            extra.put("suggestionContent", suggestion.getContent());
            suggestion.setExtra(extra);
        }

        return suggestion;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reviewSuggestion(Long suggestionId, Long reviewerId, boolean approved, String reason) {
        // 查询建议
        ArticleEditSuggestion suggestion = baseMapper.selectById(suggestionId);
        if (suggestion == null) {
            throw new IllegalArgumentException("建议不存在");
        }

        // 查询文章
        Article article = articleDao.selectById(suggestion.getArticleId());
        if (article == null) {
            throw new IllegalArgumentException("文章不存在");
        }

        if (approved) {
            // 采纳建议，创建新版本并更新文章内容
//            createVersionAndApplySuggestion(article, suggestion, reviewerId);
            
            // 记录贡献者（暂时不统计具体行数，只记录有贡献）
            articleContributorService.addContribution(
                article.getId(),
                suggestion.getProposerId(),
                1
            );

            // 更新建议状态
            suggestion.setStatus(SuggestionStatus.APPROVED.getCode());
            log.info("审核建议通过，suggestionId: {}, articleId: {}", suggestionId, article.getId());

        } else {
            // 拒绝建议
            suggestion.setStatus(SuggestionStatus.REJECTED.getCode());
            log.info("审核建议拒绝，suggestionId: {}, reason: {}", suggestionId, reason);
        }

        // 设置审核信息
        suggestion.setReviewerId(reviewerId);
        suggestion.setReviewTime(LocalDateTime.now());

        baseMapper.updateById(suggestion);
        
        // 📢 发送审核结果通知给建议提交者
        sendSuggestionReviewNotification(suggestion, approved);
    }
    
    /**
     * 发送建议提交通知给文章作者
     */
    private void sendSuggestionSubmitNotification(ArticleEditSuggestion suggestion, Long articleAuthorId) {
        try {
            // 获取建议提交者信息
            User proposer = userDao.selectById(suggestion.getProposerId());
            if (proposer == null) {
                log.warn("建议提交者不存在，userId: {}", suggestion.getProposerId());
                return;
            }
            
            UserSimpleVO proposerVO = new UserSimpleVO();
            proposerVO.setId(proposer.getId());
            proposerVO.setNickname(proposer.getNickname());
            proposerVO.setAvatar(proposer.getAvatar());
            proposerVO.setLastOnlineTime(proposer.getLastOnlineTime());
            
            // 构建 extra 数据
            Map<String, Object> extra = NotificationBuilder.buildSuggestionSubmitNotification(
                suggestion.getArticleId(),
                suggestion.getId(),
                proposerVO
            );
            
            // 创建通知
            notificationService.createNotification(
                articleAuthorId,
                NotificationType.SUGGESTION_SUBMIT.getCode(),
                suggestion.getId(),
                null,
                extra
            );
            
            log.info("发送建议提交通知，articleId: {}, authorId: {}", suggestion.getArticleId(), articleAuthorId);
            
        } catch (Exception e) {
            log.error("发送建议提交通知失败，suggestionId: {}", suggestion.getId(), e);
        }
    }
    
    /**
     * 发送审核结果通知给建议提交者
     */
    private void sendSuggestionReviewNotification(ArticleEditSuggestion suggestion, boolean approved) {
        try {
            // 获取建议提交者信息
            User proposer = userDao.selectById(suggestion.getProposerId());
            if (proposer == null) {
                log.warn("建议提交者不存在，userId: {}", suggestion.getProposerId());
                return;
            }
            
            String result = approved ? "通过" : "拒绝";
            
            // 构建 extra 数据
            Map<String, Object> extra = NotificationBuilder.buildSuggestionReviewNotification(
                suggestion.getArticleId(),
                suggestion.getId(),
                result
            );
            
            // 创建通知
            notificationService.createNotification(
                suggestion.getProposerId(),
                NotificationType.SUGGESTION_REVIEW.getCode(),
                suggestion.getId(),
                null,
                extra
            );
            
            log.info("发送审核结果通知，suggestionId: {}, proposerId: {}, result: {}", 
                     suggestion.getId(), suggestion.getProposerId(), result);
            
        } catch (Exception e) {
            log.error("发送审核结果通知失败，suggestionId: {}", suggestion.getId(), e);
        }
    }
}
