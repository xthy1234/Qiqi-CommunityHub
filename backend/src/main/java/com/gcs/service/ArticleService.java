package com.gcs.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.extension.service.IService;
import com.gcs.entity.Article;
import com.gcs.entity.Interaction;
import com.gcs.entity.view.ArticleView;
import com.gcs.utils.PageUtils;
import com.gcs.vo.AdminArticleDetailVO;
import com.gcs.vo.ArticleDashboardStatsVO;
import com.gcs.vo.ArticleAuditHistoryVO;
import com.gcs.vo.ArticleSearchVO;

import java.util.List;
import java.util.Map;

/**
 * 帖子服务接口
 *
 * @author
 * @date 2026-04-16
 */
public interface ArticleService extends IService<Article> {

    /**
     * 分页查询帖子列表
     *
     * @param params 查询参数
     * @return 分页结果
     */
    PageUtils queryPage(Map<String, Object> params);

    /**
     * 分页查询文章列表视图
     */
    IPage<ArticleView> selectListViewPage(IPage<ArticleView> page, Wrapper<Article> queryWrapper);

    /**
     * 查询帖子列表视图
     *
     * @param queryWrapper 查询条件包装器
     * @return 帖子视图列表
     */
    List<ArticleView> selectListView(Wrapper<Article> queryWrapper);

    /**
     * 查询单个帖子视图
     *
     * @param queryWrapper 查询条件包装器
     * @return 帖子视图
     */
    ArticleView selectView(Wrapper<Article> queryWrapper);

    /**
     * 根据 ID 查询帖子视图
     * @param id 帖子 ID
     * @return 帖子视图
     */
    ArticleView selectViewById(Long id);

    /**
     * 带条件的分页查询帖子列表
     *
     * @param params 查询参数
     * @param queryWrapper 查询条件包装器
     * @return 分页结果
     */
    PageUtils queryPage(Map<String, Object> params, Wrapper<Article> queryWrapper);

    /**
     * 统计值查询
     *
     * @param params 查询参数
     * @param queryWrapper 查询条件包装器
     * @return 统计结果列表
     */
    List<Map<String, Object>> selectValue(Map<String, Object> params, Wrapper<Article> queryWrapper);

    /**
     * 时间统计值查询
     *
     * @param params 查询参数
     * @param queryWrapper 查询条件包装器
     * @return 时间统计数据列表
     */
    List<Map<String, Object>> selectTimeStatValue(Map<String, Object> params, Wrapper<Article> queryWrapper);

    /**
     * 分组查询
     *
     * @param params 查询参数
     * @param queryWrapper 查询条件包装器
     * @return 分组结果列表
     */
    List<Map<String, Object>> selectGroup(Map<String, Object> params, Wrapper<Article> queryWrapper);

    /**
     * 全文搜索文章
     *
     * @param params 查询参数（包含 keyword、categoryId、startDate、endDate、limit 等）
     * @return 搜索结果列表（按相关度排序）
     */
    List<ArticleSearchVO> searchByFullText(Map<String, Object> params);
    
    /**
     * 获取文章详情（返回 Entity，由 Controller 转换为 VO）
     * @param id 文章 ID
     * @return 文章实体（可能是 ArticleView）
     */
    Article getArticleDetail(Long id);

    void insertArticle(Article article);

    /**
     * 保存文章（创建小版本）
     */
    void saveWithMinorVersion(Long articleId, Long userId, String title,
                              Map<String, Object> content, String changeSummary);

    void incrementViewCount(Long id, String identifier);
    
    /**
     * 管理员查询所有文章（包含全部审核状态）
     * @param params 查询参数
     * @param queryWrapper 查询条件
     * @return 分页结果
     */
    PageUtils adminQueryPage(Map<String, Object> params, Wrapper<Article> queryWrapper);
    
    /**
     * 获取管理后台统计数据
     * @return 统计数据 VO
     */
    ArticleDashboardStatsVO getDashboardStats();
    
    /**
     * 统计今日新增文章数
     * @return 今日新增数量
     */
    Integer countToday();
    
    /**
     * 按审核状态统计数量
     * @param auditStatus 审核状态
     * @return 数量
     */
    Integer countByAuditStatus(Integer auditStatus);
    
    /**
     * 获取热门文章（按浏览量排序）
     * @param limit 数量限制
     * @return 文章列表
     */
    List<Article> getTopViewedArticles(Integer limit);
    
    /**
     * 获取活跃作者（按文章数量排序）
     * @param limit 数量限制
     * @return 作者 ID 和文章数的 Map
     */
    List<Map<String, Object>> getTopAuthors(Integer limit);
    
    /**
     * 批量修改文章分类
     * @param articleIds 文章 ID 数组
     * @param categoryId 新分类 ID
     * @return 是否成功
     */
    boolean batchUpdateCategory(Long[] articleIds, Long categoryId);
    
    /**
     * 设置文章置顶/推荐
     * @param articleId 文章 ID
     * @param isFeatured 是否推荐
     * @param featuredLevel 推荐等级（0-普通，1-推荐，2-热门）
     * @return 是否成功
     */
    boolean setFeatured(Long articleId, Boolean isFeatured, Integer featuredLevel);
    
    /**
     * 获取文章审核历史
     * @param articleId 文章 ID
     * @return 审核历史列表
     */
    List<ArticleAuditHistoryVO> getAuditHistory(Long articleId);
    
    /**
     * 记录审核历史（内部方法）
     * @param articleId 文章 ID
     * @param reviewerId 审核员 ID
     * @param oldStatus 原审核状态
     * @param newStatus 新审核状态
     * @param reason 审核原因
     */
    void recordAuditHistory(Long articleId, Long reviewerId, Integer oldStatus, Integer newStatus, String reason);
    
    /**
     * 点赞/取消点赞文章
     * @param articleId 文章 ID
     * @param userId 用户 ID
     * @return 最新点赞数
     */
    Integer toggleLike(Long articleId, Long userId);
    
    /**
     * 点踩/取消点踩文章
     * @param articleId 文章 ID
     * @param userId 用户 ID
     * @return 最新点踩数
     */
    Integer toggleDislike(Long articleId, Long userId);
    
    /**
     * 收藏/取消收藏文章
     * @param articleId 文章 ID
     * @param userId 用户 ID
     * @return 最新收藏数
     */
    Integer toggleFavorite(Long articleId, Long userId);
    
    /**
     * 获取用户的收藏文章列表
     * @param userId 用户 ID
     * @return 收藏的互动记录列表
     */
    List<Interaction> getUserFavorites(Long userId);
}
