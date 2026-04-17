package com.gcs.service.impl;

import com.gcs.entity.*;
import com.gcs.enums.ContentType;
import com.gcs.enums.InteractionActionType;
import com.gcs.utils.SessionUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import jakarta.servlet.http.HttpServletRequest;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.gcs.dao.ArticleViewLogDao;
import com.gcs.enums.AuditStatus;
import com.gcs.service.*;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;

import com.gcs.dao.ArticleDao;
import com.gcs.entity.view.ArticleView;
import com.gcs.utils.PageUtils;
import com.gcs.utils.Query;
import com.gcs.vo.ArticleSearchVO;
import com.gcs.vo.ArticleDashboardStatsVO;
import com.gcs.vo.ArticleAuditHistoryVO;
import com.gcs.converter.ArticleConverter;
import com.gcs.entity.ArticleAuditHistory;
import com.gcs.dao.ArticleAuditHistoryDao;
import com.gcs.service.UserService;
import java.time.LocalDate;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/**
 * 帖子服务实现类
 * 提供帖子相关的业务逻辑处理
 * @author 
 * @date 2026-04-16
 */
@Slf4j
@Service("articleService")
public class ArticleServiceImpl extends ServiceImpl<ArticleDao, Article> implements ArticleService {

    @Autowired
    private ArticleVersionService articleVersionService;

    @Autowired
    private ArticleViewLogDao articleViewLogDao;

    @Autowired
    private ArticleConverter articleConverter;
    
    @Autowired
    private ArticleAuditHistoryDao auditHistoryDao;
    
    @Lazy
    @Autowired
    private UserService userService;

    @Autowired
    private BlockRuleService blockRuleService;

    @Autowired
    private SessionUtils sessionUtils;
    
    @Autowired
    private InteractionService interactionService;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        //  通过 Spring 上下文获取当前请求并注入屏蔽条件
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                Long userId = sessionUtils.getCurrentUserId(request);
                if (userId != null) {
                    blockRuleService.injectBlockConditions(userId, params);
                }
            }
        } catch (Exception e) {
            // 防止在非 Web 环境（如定时任务）下调用报错
            log.debug("无法获取当前请求上下文，跳过屏蔽规则注入");
        }

        IPage<Article> articlePage = new Query<Article>(params).getPage();
        IPage<Article> resultPage = this.page(articlePage, new QueryWrapper<>());
        
        return new PageUtils(resultPage);
    }


    @Override
    public List<ArticleView> selectListView(Wrapper<Article> queryWrapper) {
        return baseMapper.selectListView(queryWrapper);
    }

    @Override
    public ArticleView selectView(Wrapper<Article> queryWrapper) {
        return baseMapper.selectView(queryWrapper);
    }

    @Override
    public ArticleView selectViewById(Long id) {
        if (id == null) {
            return null;
        }
        Map<String, Object> params = new HashMap<>();
        params.put("id", id);
        return baseMapper.selectViewById(params);
    }

    @Override
    public PageUtils queryPage(Map<String, Object> params, Wrapper<Article> queryWrapper) {
        IPage<ArticleView> articlePage = new Query<ArticleView>(params).getPage();
        IPage<ArticleView> resultPage = baseMapper.selectListView(articlePage, queryWrapper, params);

        long totalCount = baseMapper.selectCount(queryWrapper);
        articlePage.setTotal(totalCount);
        return new PageUtils(resultPage);
    }

    @Override
    public IPage<ArticleView> selectListViewPage(IPage<ArticleView> page, Wrapper<Article> queryWrapper) {
        Map<String, Object> params = new HashMap<>();
        
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                Long userId = sessionUtils.getCurrentUserId(request);
                if (userId != null) {
                    blockRuleService.injectBlockConditions(userId, params);
                }
            }
        } catch (Exception e) {
            log.debug("无法获取当前请求上下文，跳过屏蔽规则注入");
        }
        
        return baseMapper.selectListView(page, queryWrapper, params);
    }

    @Override
    public List<Map<String, Object>> selectValue(Map<String, Object> params, Wrapper<Article> queryWrapper) {
        return baseMapper.selectValue(params, queryWrapper);
    }

    @Override
    public List<Map<String, Object>> selectTimeStatValue(Map<String, Object> params, Wrapper<Article> queryWrapper) {
        return baseMapper.selectTimeStatValue(params, queryWrapper);
    }

    @Override
    public List<Map<String, Object>> selectGroup(Map<String, Object> params, Wrapper<Article> queryWrapper) {
        return baseMapper.selectGroup(params, queryWrapper);
    }

    @Override
    public List<ArticleSearchVO> searchByFullText(Map<String, Object> params) {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                Long userId = sessionUtils.getCurrentUserId(request);
                if (userId != null) {
                    blockRuleService.injectBlockConditions(userId, params);
                }
            }
        } catch (Exception e) {
            log.debug("无法获取当前请求上下文，跳过屏蔽规则注入");
        }
        
        return baseMapper.searchByFullText(params);
    }
    
    @Override
    public Article getArticleDetail(Long id) {
        if (id == null) {
            return null;
        }
        

        return selectViewById(id);
    }
    
    @Override
    public void insertArticle(Article article) {

        if (article.getPublishTime() == null && article.getAuditStatus() == AuditStatus.APPROVED) {
            article.setPublishTime(new java.util.Date());
        }
        

        if (article.getEditMode() == null) {
            article.setEditMode(0);
        }
        
        if (article.getAuditReply() == null) {
            article.setAuditReply("");
        }
        
        this.save(article);
    }
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveWithMinorVersion(Long articleId, Long userId, String title,
                                     Map<String, Object> content, String changeSummary) {
        Article article = this.getById(articleId);
        if (article == null) {
            throw new IllegalArgumentException("文章不存在");
        }


        articleVersionService.createMinorVersion(article, userId,
                changeSummary != null ? changeSummary : "手动保存");


        article.setTitle(title);
        article.setContent(content);
        

        List<ArticleVersion> versions = articleVersionService.getVersionHistory(articleId);
        if (versions != null && !versions.isEmpty()) {
            ArticleVersion latestVersion = versions.get(0);
            article.setCurrentVersion(latestVersion.getVersion());
        }
        
        this.updateById(article);

        log.info("保存文章并创建小版本，articleId: {}, newVersion: {}", 
                articleId, article.getCurrentVersion());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void incrementViewCount(Long id, String identifier) {
        if (id == null || identifier == null) {
            throw new IllegalArgumentException("文章 ID 和访问者标识不能为空");
        }
        
        try {

            int inserted = articleViewLogDao.insertIgnore(id, identifier);
            

            if (inserted > 0) {
                Article article = this.getById(id);
                if (article != null) {
                    Integer currentCount = article.getViewCount();
                    if (currentCount == null) {
                        currentCount = 0;
                    }
                    article.setViewCount(currentCount + 1);
                    this.updateById(article);
                    
                    log.info("增加文章浏览量成功，articleId: {}, viewerKey: {}, 当前浏览量：{}", 
                            id, identifier, article.getViewCount());
                }
            } else {
                log.debug("浏览记录已存在，跳过计数，articleId: {}, viewerKey: {}", id, identifier);
            }
        } catch (Exception e) {
            log.error("增加文章浏览量失败，articleId: {}, viewerKey: {}", id, identifier, e);
            throw new RuntimeException("增加浏览量失败", e);
        }
    }

    @Override
    public PageUtils adminQueryPage(Map<String, Object> params, Wrapper<Article> queryWrapper) {
        IPage<ArticleView> articlePage = new Query<ArticleView>(params).getPage();
        IPage<ArticleView> resultPage = baseMapper.selectListView(articlePage, queryWrapper, params);
        
        long totalCount = baseMapper.selectCount(queryWrapper);
        resultPage.setTotal(totalCount);
        return new PageUtils(resultPage);
    }
    
    @Override
    public ArticleDashboardStatsVO getDashboardStats() {
        ArticleDashboardStatsVO statsVO = new ArticleDashboardStatsVO();
        
        // 今日统计
        Integer todayCount = countToday();
        statsVO.setTodayCount(todayCount);
        
        // 昨日统计
        Integer yesterdayCount = countByDate(LocalDate.now().minusDays(1));
        statsVO.setYesterdayCount(yesterdayCount);
        
        // 审核状态统计
        statsVO.setPendingCount(countByAuditStatus(AuditStatus.PENDING.getCode()));
        statsVO.setApprovedCount(countByAuditStatus(AuditStatus.APPROVED.getCode()));
        statsVO.setRejectedCount(countByAuditStatus(AuditStatus.REJECTED.getCode()));
        
        // 总数统计
        statsVO.setTotalCount(Math.toIntExact(this.count()));
        
        // 总浏览量
        List<Map<String, Object>> totalViewResult = this.selectValue(
            Map.of("xColumn", "id", "yColumn", "view_count"), 
            new QueryWrapper<>()
        );
        Integer totalViewCount = totalViewResult.stream()
            .mapToInt(m -> ((Number) m.get("view_count")).intValue())
            .sum();
        statsVO.setTotalViewCount(totalViewCount);
        
        // 热门文章
        List<Article> topArticles = getTopViewedArticles(10);
        statsVO.setTopArticles(topArticles.stream()
            .map(a -> Map.of(
                "id", a.getId(),
                "title", a.getTitle(),
                "viewCount", a.getViewCount()
            ))
            .collect(Collectors.toList()));
        
        // 活跃作者
        List<Map<String, Object>> topAuthors = getTopAuthors(10);
        statsVO.setTopAuthors(topAuthors);
        
        // 分类统计
        List<Map<String, Object>> categoryStats = this.selectGroup(
            Map.of("column", "category_id"),
            new QueryWrapper<>()
        );
        Map<String, Integer> categoryMap = categoryStats.stream()
            .collect(Collectors.toMap(
                m -> m.get("category_id").toString(),
                m -> ((Number) m.get("count")).intValue()
            ));
        statsVO.setCategoryStats(categoryMap);
        
        // 日环比增长率
        if (yesterdayCount > 0) {
            Double growth = ((todayCount - yesterdayCount) * 100.0) / yesterdayCount;
            statsVO.setDayOverDayGrowth(growth);
        } else {
            statsVO.setDayOverDayGrowth(todayCount > 0 ? 100.0 : 0.0);
        }
        
        return statsVO;
    }
    
    private Integer countByDate(LocalDate date) {
        QueryWrapper<Article> wrapper = new QueryWrapper<>();
        wrapper.eq("deleted", false)
            .between("create_time", 
                date.atStartOfDay(),
                date.plusDays(1).atStartOfDay());
        return Math.toIntExact(this.count(wrapper));
    }
    
    @Override
    public Integer countToday() {
        return countByDate(LocalDate.now());
    }
    
    @Override
    public Integer countByAuditStatus(Integer auditStatus) {
        QueryWrapper<Article> wrapper = new QueryWrapper<>();
        wrapper.eq("deleted", false)
            .eq("audit_status", auditStatus);
        return Math.toIntExact(this.count(wrapper));
    }
    
    @Override
    public List<Article> getTopViewedArticles(Integer limit) {
        QueryWrapper<Article> wrapper = new QueryWrapper<>();
        wrapper.eq("deleted", false)
            .orderByDesc("view_count")
            .last("LIMIT " + limit);
        return this.list(wrapper);
    }
    
    @Override
    public List<Map<String, Object>> getTopAuthors(Integer limit) {
        QueryWrapper<Article> wrapper = new QueryWrapper<>();
        wrapper.select("author_id", "COUNT(*) as article_count")
            .eq("deleted", false)
            .groupBy("author_id")
            .orderByDesc("article_count")
            .last("LIMIT " + limit);
        return this.listMaps(wrapper);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean batchUpdateCategory(Long[] articleIds, Long categoryId) {
        if (articleIds == null || articleIds.length == 0) {
            return false;
        }
        
        for (Long id : articleIds) {
            Article article = this.getById(id);
            if (article != null) {
                article.setCategoryId(categoryId);
                this.updateById(article);
            }
        }
        
        log.info("批量修改文章分类成功，数量：{}, 新分类 ID: {}", articleIds.length, categoryId);
        return true;
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean setFeatured(Long articleId, Boolean isFeatured, Integer featuredLevel) {
        Article article = this.getById(articleId);
        if (article == null) {
            return false;
        }
        
        article.setIsFeatured(isFeatured);
        article.setFeaturedLevel(featuredLevel != null ? featuredLevel : 0);
        this.updateById(article);
        
        log.info("设置文章推荐状态，articleId: {}, isFeatured: {}, level: {}", 
            articleId, isFeatured, featuredLevel);
        return true;
    }
    
    @Override
    public List<ArticleAuditHistoryVO> getAuditHistory(Long articleId) {
        QueryWrapper<ArticleAuditHistory> wrapper = new QueryWrapper<>();
        wrapper.eq("article_id", articleId)
            .orderByDesc("create_time");
        
        List<ArticleAuditHistory> historyList = auditHistoryDao.selectList(wrapper);
        
        return historyList.stream()
            .map(history -> {
                ArticleAuditHistoryVO vo = new ArticleAuditHistoryVO();
                vo.setId(history.getId());
                vo.setArticleId(history.getArticleId());
                vo.setAuditorId(history.getReviewerId());
                
                // 补充审核员昵称
                if (history.getReviewerId() != null) {
                    User reviewer = userService.getById(history.getReviewerId());
                    if (reviewer != null) {
                        vo.setAuditorNickname(reviewer.getNickname());
                    }
                }
                
                vo.setOldStatus(history.getOldStatus());
                vo.setNewStatus(history.getNewStatus());
                vo.setReason(history.getReason());
                vo.setAuditTime(history.getCreateTime());
                
                return vo;
            })
            .collect(Collectors.toList());
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void recordAuditHistory(Long articleId, Long reviewerId, 
                                   Integer oldStatus, Integer newStatus, String reason) {
        try {
            ArticleAuditHistory history = new ArticleAuditHistory();
            history.setArticleId(articleId);
            history.setReviewerId(reviewerId);
            history.setOldStatus(oldStatus);
            history.setNewStatus(newStatus);
            history.setReason(reason != null ? reason : "");
            
            auditHistoryDao.insert(history);
            log.info("记录审核历史成功，articleId: {}, reviewerId: {}, oldStatus: {}, newStatus: {}", 
                articleId, reviewerId, oldStatus, newStatus);
        } catch (Exception e) {
            log.error("记录审核历史失败，articleId: {}, reviewerId: {}", articleId, reviewerId, e);
            // 不抛出异常，避免影响主流程
        }
    }
    
    /**
     * 点赞/取消点赞文章
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Integer toggleLike(Long articleId, Long userId) {
        if (articleId == null || userId == null) {
            throw new IllegalArgumentException("参数不能为空");
        }
        
        Article article = this.getById(articleId);
        if (article == null) {
            throw new IllegalArgumentException("文章不存在");
        }
        
        // 检查用户是否有有效的点赞记录
        boolean hasLiked = interactionService.hasValidInteraction(
            userId, 
            articleId, 
            InteractionActionType.LIKE,
            ContentType.ARTICLE
        );
        
        Integer currentLikeCount = article.getLikeCount();
        if (currentLikeCount == null) {
            currentLikeCount = 0;
        }
        
        if (hasLiked) {
            // 已点赞，取消点赞：将互动记录状态改为无效，点赞数 -1
            boolean removed = interactionService.removeInteraction(
                userId, 
                articleId, 
                InteractionActionType.LIKE, 
                ContentType.ARTICLE
            );
            
            if (removed) {
                currentLikeCount = Math.max(0, currentLikeCount - 1);
                article.setLikeCount(currentLikeCount);
                this.updateById(article);
                log.info("取消点赞文章成功，articleId: {}, userId: {}, 新点赞数: {}", articleId, userId, currentLikeCount);
            }
        } else {
            // 未点赞或之前取消了，添加/重新激活点赞
            Interaction interaction = new Interaction();
            interaction.setUserId(userId);
            interaction.setContentId(articleId);
            interaction.setActionType(InteractionActionType.LIKE);
            interaction.setTableName(ContentType.ARTICLE);
            interaction.setCreateTime(LocalDateTime.now());
            
            boolean added = interactionService.addInteraction(interaction);
            
            if (added) {
                currentLikeCount = currentLikeCount + 1;
                article.setLikeCount(currentLikeCount);
                this.updateById(article);
                log.info("点赞文章成功，articleId: {}, userId: {}, 新点赞数: {}", articleId, userId, currentLikeCount);
            }
        }
        
        return article.getLikeCount();
    }
    
    /**
     * 点踩/取消点踩文章
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Integer toggleDislike(Long articleId, Long userId) {
        if (articleId == null || userId == null) {
            throw new IllegalArgumentException("参数不能为空");
        }
        
        Article article = this.getById(articleId);
        if (article == null) {
            throw new IllegalArgumentException("文章不存在");
        }
        
        // 检查用户是否有有效的点踩记录
        boolean hasDisliked = interactionService.hasValidInteraction(
            userId, 
            articleId, 
            InteractionActionType.DISLIKE, 
            ContentType.ARTICLE
        );
        
        Integer currentDislikeCount = article.getDislikeCount();
        if (currentDislikeCount == null) {
            currentDislikeCount = 0;
        }
        
        if (hasDisliked) {
            // 已点踩，取消点踩：将互动记录状态改为无效，点踩数 -1
            boolean removed = interactionService.removeInteraction(
                userId, 
                articleId, 
                InteractionActionType.DISLIKE, 
                ContentType.ARTICLE
            );
            
            if (removed) {
                currentDislikeCount = Math.max(0, currentDislikeCount - 1);
                article.setDislikeCount(currentDislikeCount);
                this.updateById(article);
                log.info("取消点踩文章成功，articleId: {}, userId: {}, 新点踩数: {}", articleId, userId, currentDislikeCount);
            }
        } else {
            // 未点踩或之前取消了，添加/重新激活点踩
            Interaction interaction = new Interaction();
            interaction.setUserId(userId);
            interaction.setContentId(articleId);
            interaction.setActionType(InteractionActionType.DISLIKE);
            interaction.setTableName(ContentType.ARTICLE);
            interaction.setCreateTime(LocalDateTime.now());
            
            boolean added = interactionService.addInteraction(interaction);
            
            if (added) {
                currentDislikeCount = currentDislikeCount + 1;
                article.setDislikeCount(currentDislikeCount);
                this.updateById(article);
                log.info("点踩文章成功，articleId: {}, userId: {}, 新点踩数: {}", articleId, userId, currentDislikeCount);
            }
        }
        
        return article.getDislikeCount();
    }
    
    /**
     * 收藏/取消收藏文章
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Integer toggleFavorite(Long articleId, Long userId) {
        if (articleId == null || userId == null) {
            throw new IllegalArgumentException("参数不能为空");
        }
        
        Article article = this.getById(articleId);
        if (article == null) {
            throw new IllegalArgumentException("文章不存在");
        }
        
        // 检查用户是否有有效的收藏记录
        boolean hasFavorited = interactionService.hasValidInteraction(
            userId, 
            articleId, 
            InteractionActionType.FAVORITE, 
            ContentType.ARTICLE
        );
        
        Integer currentFavoriteCount = article.getFavoriteCount();
        if (currentFavoriteCount == null) {
            currentFavoriteCount = 0;
        }
        
        if (hasFavorited) {
            // 已收藏，取消收藏：将互动记录状态改为无效，收藏数 -1
            boolean removed = interactionService.removeInteraction(
                userId, 
                articleId, 
                InteractionActionType.FAVORITE, 
                ContentType.ARTICLE
            );
            
            if (removed) {
                currentFavoriteCount = Math.max(0, currentFavoriteCount - 1);
                article.setFavoriteCount(currentFavoriteCount);
                this.updateById(article);
                log.info("取消收藏文章成功，articleId: {}, userId: {}, 新收藏数: {}", articleId, userId, currentFavoriteCount);
            }
        } else {
            // 未收藏或之前取消了，添加/重新激活收藏
            Interaction interaction = new Interaction();
            interaction.setUserId(userId);
            interaction.setContentId(articleId);
            interaction.setActionType(InteractionActionType.FAVORITE);
            interaction.setTableName(ContentType.ARTICLE);
            interaction.setCreateTime(LocalDateTime.now());
            
            boolean added = interactionService.addInteraction(interaction);
            
            if (added) {
                currentFavoriteCount = currentFavoriteCount + 1;
                article.setFavoriteCount(currentFavoriteCount);
                this.updateById(article);
                log.info("收藏文章成功，articleId: {}, userId: {}, 新收藏数: {}", articleId, userId, currentFavoriteCount);
            }
        }
        
        return article.getFavoriteCount();
    }
}
