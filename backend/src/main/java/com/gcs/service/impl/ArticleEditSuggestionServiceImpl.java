package com.gcs.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.gcs.converter.ArticleSuggestionConverter;
import com.gcs.dao.ArticleDao;
import com.gcs.dao.ArticleEditSuggestionDao;
import com.gcs.dao.UserDao;
import com.gcs.entity.ArticleEditSuggestion;
import com.gcs.entity.Article;
import com.gcs.entity.User;
import com.gcs.entity.view.ArticleView;
import com.gcs.service.ArticleEditSuggestionService;
import com.gcs.service.NotificationService;
import com.gcs.service.ArticleVersionService;
import com.gcs.service.ArticleContributorService;
import com.gcs.enums.NotificationType;
import com.gcs.enums.SuggestionStatus;
import com.gcs.utils.NotificationBuilder;
import com.gcs.vo.UserSimpleVO;
import com.gcs.vo.ArticleSuggestionVO;
import com.gcs.vo.ArticleSuggestionSimpleVO;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

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
    
    @Autowired
    private ArticleSuggestionConverter articleSuggestionConverter;

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
        
        // ✅ 记录建议是基于哪个版本提出的
        Integer currentVersion = article.getVersion() != null ? article.getVersion() : 0;
        suggestion.setVersion(currentVersion);
        
        // ✅ 初始化 extra 信息
        Map<String, Object> extra = new HashMap<>();
        extra.put("baseVersion", currentVersion);
        suggestion.setExtra(extra);

        baseMapper.insert(suggestion);

        log.info("提交修改建议成功，suggestionId: {}, articleId: {}, baseVersion: {}", 
                 suggestion.getId(), articleId, currentVersion);
        
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
            // ✅ 采纳建议，创建新版本并更新文章内容
            createVersionAndApplySuggestion(article, suggestion, reviewerId);
            
            // 记录贡献者（统计贡献次数）
            articleContributorService.addContribution(
                article.getId(),
                suggestion.getProposerId(),
                1
            );

            // 更新建议状态
            suggestion.setStatus(SuggestionStatus.APPROVED.getCode());
            log.info("审核建议通过，suggestionId: {}, articleId: {}", suggestionId, article.getId());

        } else {
            // ❌ 拒绝建议
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
     * 创建新版本并应用建议内容
     */
    private void createVersionAndApplySuggestion(Article article, 
                                                 ArticleEditSuggestion suggestion, 
                                                 Long operatorId) {
        try {
            // 1️⃣ 获取当前版本号并 +1
            Integer currentVersion = article.getVersion() != null ? article.getVersion() : 0;
            Integer newVersion = currentVersion + 1;
            
            // 2️⃣ 创建文章新版本（保存快照）
            Integer versionNumber = articleVersionService.createVersion(
                article,
                operatorId,
                "采纳修改建议 #" + suggestion.getId() + ": " + suggestion.getChangeSummary()
            );
            
            // 3️⃣ 更新文章的当前内容为建议的内容
            article.setContent(suggestion.getContent());
            article.setVersion(newVersion);  // 更新文章版本号
            article.setUpdateTime(LocalDateTime.now());
            articleDao.updateById(article);
            
            // 4️⃣ 在建议的 extra 中记录版本信息
            Map<String, Object> extra = suggestion.getExtra() != null 
                ? new HashMap<>(suggestion.getExtra()) 
                : new HashMap<>();
            extra.put("appliedVersion", newVersion);
            extra.put("previousVersion", currentVersion);
            suggestion.setExtra(extra);
            baseMapper.updateById(suggestion);
            
            log.info("✅ 已创建文章新版本 v{} 并应用建议，articleId: {}, suggestionId: {}", 
                     newVersion, article.getId(), suggestion.getId());
            
        } catch (Exception e) {
            log.error("创建新版本失败，articleId: {}, suggestionId: {}", 
                     article.getId(), suggestion.getId(), e);
            throw new RuntimeException("创建新版本失败：" + e.getMessage());
        }
    }

    // ==================== 新增方法：构建完整的 VO ====================
    
    @Override
    public IPage<ArticleSuggestionSimpleVO> getSuggestionsWithDetails(Long articleId, Integer status,
                                                                      Integer page, Integer limit) {
        // 先查询基础数据
        IPage<ArticleEditSuggestion> suggestionPage = getSuggestions(articleId, status, page, limit);
        
        // ✅ 使用 Converter 转换为列表 VO 列表
        List<ArticleSuggestionSimpleVO> voList = suggestionPage.getRecords().stream()
            .map(this::buildListVO)
            .collect(Collectors.toList());
        
        // 构建返回的分页对象
        Page<ArticleSuggestionSimpleVO> voPage = new Page<>(page, limit);
        voPage.setRecords(voList);
        voPage.setTotal(suggestionPage.getTotal());
        
        return voPage;
    }

    @Override
    public ArticleSuggestionVO getSuggestionDetailWithVo(Long suggestionId) {
        // 查询基础数据
        ArticleEditSuggestion suggestion = getSuggestionDetail(suggestionId);
        
        // ✅ 使用 Converter 构建完整的详细信息 VO
        return buildDetailVO(suggestion);
    }

    @Override
    public IPage<ArticleSuggestionSimpleVO> getMySuggestionsWithDetails(Long proposerId, Integer status,
                                                                        Integer page, Integer limit) {
        // 先查询基础数据
        IPage<ArticleEditSuggestion> suggestionPage = getSuggestionsByProposer(proposerId, status, page, limit);
        
        // ✅ 使用 Converter 转换为列表 VO 列表
        List<ArticleSuggestionSimpleVO> voList = suggestionPage.getRecords().stream()
            .map(this::buildListVO)
            .collect(Collectors.toList());
        
        // 构建返回的分页对象
        Page<ArticleSuggestionSimpleVO> voPage = new Page<>(page, limit);
        voPage.setRecords(voList);
        voPage.setTotal(suggestionPage.getTotal());
        
        return voPage;
    }

    @Override
    public IPage<ArticleSuggestionSimpleVO> getReceivedSuggestionsWithDetails(Long authorId, Integer status,
                                                                              Integer page, Integer limit) {
        // 先查询基础数据
        IPage<ArticleEditSuggestion> suggestionPage = getSuggestionsByAuthor(authorId, status, page, limit);
        
        // ✅ 使用 Converter 转换为列表 VO 列表
        List<ArticleSuggestionSimpleVO> voList = suggestionPage.getRecords().stream()
            .map(this::buildListVO)
            .collect(Collectors.toList());
        
        // 构建返回的分页对象
        Page<ArticleSuggestionSimpleVO> voPage = new Page<>(page, limit);
        voPage.setRecords(voList);
        voPage.setTotal(suggestionPage.getTotal());
        
        return voPage;
    }

    /**
     * 构建列表 VO（包含基本信息，用于列表展示）
     */
    private ArticleSuggestionSimpleVO buildListVO(ArticleEditSuggestion suggestion) {
        if (suggestion == null) {
            return null;
        }
        
        // ✅ 第一步：使用 Converter 进行基础转换
        ArticleSuggestionSimpleVO vo = articleSuggestionConverter.toListVO(suggestion);
        
        // ✅ 第二步：补充关联数据
        
        // 补充文章信息
        Article article = articleDao.selectById(suggestion.getArticleId());
        if (article != null) {
            vo.setArticleTitle(article.getTitle());
            vo.setArticleCoverUrl(article.getCoverUrl());
        }
        
        // 补充建议者信息
        User proposer = userDao.selectById(suggestion.getProposerId());
        if (proposer != null) {
            UserSimpleVO proposerVO = buildUserSimpleVO(proposer);
            vo.setProposer(proposerVO);
        }
        
        // 补充审核者信息
        if (suggestion.getReviewerId() != null) {
            User reviewer = userDao.selectById(suggestion.getReviewerId());
            if (reviewer != null) {
                UserSimpleVO reviewerVO = buildUserSimpleVO(reviewer);
                vo.setReviewer(reviewerVO);
            }
        }
        
        // ✅ 补充版本信息
        if (suggestion.getExtra() != null && suggestion.getExtra().containsKey("appliedVersion")) {
            vo.setAppliedVersion((Integer) suggestion.getExtra().get("appliedVersion"));
        }
        
        return vo;
    }

    /**
     * 构建详细信息 VO（包含 content 等完整字段，用于详情页和审核）
     */
    private ArticleSuggestionVO buildDetailVO(ArticleEditSuggestion suggestion) {
        if (suggestion == null) {
            return null;
        }
        
        // ✅ 第一步：使用 Converter 进行基础转换
        ArticleSuggestionVO vo = articleSuggestionConverter.toDetailVO(suggestion);
        
        // ✅ 第二步：补充关联数据
        
        // ✅ 使用 selectViewById 确保 JSONB 字段正确映射
        Map<String, Object> params = new HashMap<>();
        params.put("id", suggestion.getArticleId());
        ArticleView articleView = articleDao.selectViewById(params);
        
        if (articleView != null) {
            vo.setArticleTitle(articleView.getTitle());
            vo.setArticleCoverUrl(articleView.getCoverUrl());
            // ✅ 从 articleView 中获取 content（已经是正确的 Map 类型）
            vo.setOriginalContent(articleView.getContent());
        }
        
        // 补充建议者信息
        User proposer = userDao.selectById(suggestion.getProposerId());
        if (proposer != null) {
            UserSimpleVO proposerVO = buildUserSimpleVO(proposer);
            vo.setProposer(proposerVO);
        }
        
        // 补充审核者信息
        if (suggestion.getReviewerId() != null) {
            User reviewer = userDao.selectById(suggestion.getReviewerId());
            if (reviewer != null) {
                UserSimpleVO reviewerVO = buildUserSimpleVO(reviewer);
                vo.setReviewer(reviewerVO);
            }
        }
        
        // ✅ 补充版本信息和 extra
        if (suggestion.getExtra() != null) {
            Map<String, Object> extra = new HashMap<>(suggestion.getExtra());
            Integer addedLines = (Integer) extra.get("addedLines");
            Integer removedLines = (Integer) extra.get("removedLines");
            if (addedLines != null) vo.setAddedLines(addedLines);
            if (removedLines != null) vo.setRemovedLines(removedLines);
            
            // ✅ 提取 appliedVersion
            if (extra.containsKey("appliedVersion")) {
                vo.setAppliedVersion((Integer) extra.get("appliedVersion"));
            }
            
            vo.setExtra(extra);
        }
        
        return vo;
    }

    /**
     * 构建 UserSimpleVO
     */
    private UserSimpleVO buildUserSimpleVO(User user) {
        if (user == null) {
            return null;
        }
        
        UserSimpleVO vo = new UserSimpleVO();
        vo.setId(user.getId());
        vo.setNickname(user.getNickname());
        vo.setAvatar(user.getAvatar());
        vo.setLastOnlineTime(user.getLastOnlineTime());
        
        return vo;
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
