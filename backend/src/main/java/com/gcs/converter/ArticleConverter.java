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
import org.mapstruct.AfterMapping;
import org.mapstruct.IterableMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;
import java.util.List;

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
}
