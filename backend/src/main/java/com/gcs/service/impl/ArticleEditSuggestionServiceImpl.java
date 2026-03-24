package com.gcs.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.gcs.dao.ArticleDao;
import com.gcs.dao.ArticleEditSuggestionDao;
import com.gcs.dao.ArticleVersionDao;
import com.gcs.entity.Article;
import com.gcs.entity.ArticleEditSuggestion;
import com.gcs.entity.ArticleVersion;
import com.gcs.enums.SuggestionStatus;
import com.gcs.service.ArticleContributorService;
import com.gcs.service.ArticleEditSuggestionService;
import com.gcs.service.ArticleVersionService;
import com.gcs.utils.JsonDiffUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
        ArticleEditSuggestion suggestion = baseMapper.selectById(suggestionId);
        if (suggestion == null) {
            throw new IllegalArgumentException("建议不存在");
        }

        if (suggestion.getStatus() != SuggestionStatus.PENDING.getCode()) {
            throw new IllegalArgumentException("建议已审核，无法重复操作");
        }

        Article article = articleDao.selectById(suggestion.getArticleId());
        if (article == null) {
            throw new IllegalArgumentException("文章不存在");
        }

        if (approved) {
            // 更新文章内容
            article.setTitle(suggestion.getTitle());
            article.setContent(suggestion.getContent());
            articleDao.updateById(article);

            // 创建新版本
            String changeSummary = suggestion.getChangeSummary() != null 
                ? suggestion.getChangeSummary() 
                : "审核通过修改建议";
            articleVersionService.createVersion(article, reviewerId, changeSummary);

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
    }

    @Override
    public Integer countPendingSuggestions(Long articleId) {
        return articleEditSuggestionDao.countPendingSuggestions(articleId);
    }
}
