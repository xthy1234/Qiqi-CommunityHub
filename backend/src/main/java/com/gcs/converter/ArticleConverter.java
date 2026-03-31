package com.gcs.converter;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.gcs.dto.ArticleCreateDTO;
import com.gcs.dto.ArticleUpdateDTO;
import com.gcs.dto.ArticleDTO;
import com.gcs.dto.ArticleDraftDTO;
import com.gcs.entity.Article;
import com.gcs.entity.view.ArticleView;
import com.gcs.enums.AuditStatus;
import com.gcs.vo.ArticleVO;
import com.gcs.vo.ArticleDetailVO;
import com.gcs.vo.AdminArticleDetailVO;
import com.gcs.vo.ArticleAuditHistoryVO;
import com.gcs.vo.ArticleDashboardStatsVO;
import org.mapstruct.AfterMapping;
import org.mapstruct.IterableMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;
import java.util.List;
import java.util.Map;

/**
 * Article 对象转换器
 */
@Mapper(componentModel = "spring")
public interface ArticleConverter {
    
    // ==================== DTO to Entity ====================
    Article toEntity(ArticleDTO dto);
    
    Article toEntity(ArticleCreateDTO dto);
    
    Article toEntity(ArticleDraftDTO dto);
    
    Article toEntity(ArticleUpdateDTO dto);
    
    // ==================== Entity to VO (基础映射) ====================
    @Named("toVO")
    ArticleVO toVO(Article entity);

    @Named("toDetailVO")
    ArticleDetailVO toDetailVO(Article entity);
    
    AdminArticleDetailVO toAdminDetailVO(Article entity);

    ArticleAuditHistoryVO toAuditHistoryVO(Object entity);
    
    default ArticleDashboardStatsVO toDashboardStatsVO(Map<String, Object> data) {
        if (data == null) {
            return null;
        }
        
        ArticleDashboardStatsVO vo = new ArticleDashboardStatsVO();
        
        // 安全转换 Number 到 Integer
        vo.setTodayCount(toInteger(data.get("todayCount")));
        vo.setYesterdayCount(toInteger(data.get("yesterdayCount")));
        vo.setPendingCount(toInteger(data.get("pendingCount")));
        vo.setApprovedCount(toInteger(data.get("approvedCount")));
        vo.setRejectedCount(toInteger(data.get("rejectedCount")));
        vo.setTotalCount(toInteger(data.get("totalCount")));
        vo.setTotalViewCount(toInteger(data.get("totalViewCount")));
        
        // 其他字段
        vo.setTopArticles(data.get("topArticles"));
        vo.setTopAuthors(data.get("topAuthors"));
        
        @SuppressWarnings("unchecked")
        Map<String, Integer> categoryStats = (Map<String, Integer>) data.get("categoryStats");
        vo.setCategoryStats(categoryStats);
        
        vo.setDayOverDayGrowth(toDouble(data.get("dayOverDayGrowth")));
        
        return vo;
    }
    
    /**
     * 安全地将 Object 转换为 Integer（处理 Number 子类）
     */
    default Integer toInteger(Object value) {
        if (value == null) {
            return 0;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException e) {
            return 0;
        }
    }
    
    /**
     * 安全地将 Object 转换为 Double
     */
    default Double toDouble(Object value) {
        if (value == null) {
            return 0.0;
        }
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        try {
            return Double.parseDouble(value.toString());
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }
    
    // ==================== ArticleView to VO (特殊处理扩展字段) ====================
    @Named("viewToVO")
    @Mapping(target = "categoryName", source = "categoryName")
    @Mapping(target = "authorNickname", source = "authorNickname")
    @Mapping(target = "authorAvatar", source = "authorAvatar")
    ArticleVO viewToVO(ArticleView view);
    
    @Named("viewToDetailVO")
    @Mapping(target = "categoryName", source = "categoryName")
    @Mapping(target = "authorNickname", source = "authorNickname")
    @Mapping(target = "authorAvatar", source = "authorAvatar")
    ArticleDetailVO viewToDetailVO(ArticleView view);
    
    // ==================== List conversion ====================
    @IterableMapping(qualifiedByName = "toVO")
    List<ArticleVO> toVOList(List<Article> entities);
    
    @IterableMapping(qualifiedByName = "toDetailVO")
    List<ArticleDetailVO> toDetailVOList(List<Article> entities);
    
    @IterableMapping(qualifiedByName = "viewToVO")
    List<ArticleVO> viewToVOList(List<ArticleView> views);
    
    @IterableMapping(qualifiedByName = "viewToDetailVO")
    List<ArticleDetailVO> viewToDetailVOList(List<ArticleView> views);
    
    // ==================== Update existing entity ====================
    void updateEntity(ArticleUpdateDTO dto, @MappingTarget Article entity);
    
    // ==================== Custom mapping methods ====================
    /**
     * AuditStatus ↔ Integer 转换方法
     */
    default Integer auditStatusToInt(AuditStatus auditStatus) {
        return auditStatus != null ? auditStatus.getCode() : null;
    }
    
    default AuditStatus intToAuditStatus(Integer code) {
        return code != null ? AuditStatus.valueOf(code) : null;
    }
    
    /**
     * Map 转 ArticleDashboardStatsVO（统计数据处理）
     */
    default ArticleDashboardStatsVO mapToDashboardStatsVO(Map<String, Object> data) {
        if (data == null) {
            return null;
        }
        
        ArticleDashboardStatsVO vo = new ArticleDashboardStatsVO();
        
        // 安全转换 Number 到 Integer
        vo.setTodayCount(toInteger(data.get("todayCount")));
        vo.setYesterdayCount(toInteger(data.get("yesterdayCount")));
        vo.setPendingCount(toInteger(data.get("pendingCount")));
        vo.setApprovedCount(toInteger(data.get("approvedCount")));
        vo.setRejectedCount(toInteger(data.get("rejectedCount")));
        vo.setTotalCount(toInteger(data.get("totalCount")));
        vo.setTotalViewCount(toInteger(data.get("totalViewCount")));
        
        // 其他字段
        vo.setTopArticles(data.get("topArticles"));
        vo.setTopAuthors(data.get("topAuthors"));
        vo.setCategoryStats((Map<String, Integer>) data.get("categoryStats"));
        vo.setDayOverDayGrowth(toDouble(data.get("dayOverDayGrowth")));
        
        return vo;
    }
    
    // ==================== Custom mapping logic ====================
    @AfterMapping
    default void handlePublishTime(Article article, @MappingTarget ArticleDetailVO vo) {
        if (article.getPublishTime() == null && article.getCreateTime() != null) {
            vo.setPublishTime(java.util.Date.from(
                article.getCreateTime().atZone(java.time.ZoneId.systemDefault()).toInstant()
            ));
        }
        
        if (article.getUpdateTime() != null) {
            vo.setUpdateTime(article.getUpdateTime().toString());
        }
    }
    
    @AfterMapping
    default void handlePublishTimeForBaseVO(Article article, @MappingTarget ArticleVO vo) {
        if (article.getPublishTime() == null && article.getCreateTime() != null) {
            vo.setPublishTime(java.util.Date.from(
                article.getCreateTime().atZone(java.time.ZoneId.systemDefault()).toInstant()
            ));
        }
    }
    
    @AfterMapping
    default void handleAdminVO(Article article, @MappingTarget AdminArticleDetailVO vo) {
        // 处理发布时间
        if (article.getPublishTime() == null && article.getCreateTime() != null) {
            vo.setPublishTime(java.util.Date.from(
                article.getCreateTime().atZone(java.time.ZoneId.systemDefault()).toInstant()
            ));
        }
        
        // 处理审核状态枚举转 Integer
        if (article.getAuditStatus() != null) {
            vo.setAuditStatus(article.getAuditStatus().getCode());
        }
        
        // 设置版本号（从 currentVersion 解析）
        if (article.getCurrentVersion() != null) {
            // 这里只是简单设置，实际应该在 Controller 中从 ArticleVersion 获取详细的 major/minor version
            vo.setMajorVersion(1);
            vo.setMinorVersion(article.getCurrentVersion());
        } else {
            vo.setMajorVersion(1);
            vo.setMinorVersion(0);
        }
        
        // 设置推荐相关字段
        vo.setIsFeatured(article.getIsFeatured() != null ? article.getIsFeatured() : false);
        // isTop 字段暂时不存在于 Article 实体中，如果需要可以后续添加
        vo.setIsTop(false);
    }
}
