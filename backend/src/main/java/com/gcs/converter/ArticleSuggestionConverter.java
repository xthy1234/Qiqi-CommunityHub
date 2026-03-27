package com.gcs.converter;

import com.gcs.entity.ArticleEditSuggestion;
import com.gcs.vo.ArticleSuggestionSimpleVO;
import com.gcs.vo.ArticleSuggestionVO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

/**
 * 文章修改建议 Converter 接口
 * 用于 Entity 与 VO 之间的转换
 */
@Mapper(componentModel = "spring")
public interface ArticleSuggestionConverter {
    
    ArticleSuggestionConverter INSTANCE = Mappers.getMapper(ArticleSuggestionConverter.class);
    
    // ==================== 详细信息 VO ====================
    
    /**
     * 将 Entity 转换为详细信息 VO（包含 content 等完整字段）
     */
    @Mapping(target = "articleTitle", ignore = true)
    @Mapping(target = "articleCoverUrl", ignore = true)
    @Mapping(target = "originalContent", ignore = true)
    @Mapping(target = "proposer", ignore = true)
    @Mapping(target = "reviewer", ignore = true)
    @Mapping(target = "addedLines", ignore = true)
    @Mapping(target = "removedLines", ignore = true)
    @Mapping(target = "extra", ignore = true)
    @Mapping(target = "appliedVersion", ignore = true)
    ArticleSuggestionVO toDetailVO(ArticleEditSuggestion entity);
    
    /**
     * 批量转换为详细信息 VO
     */
    java.util.List<ArticleSuggestionVO> toDetailVOList(java.util.List<ArticleEditSuggestion> entities);
    
    // ==================== 列表简略 VO ====================
    
    /**
     * 将 Entity 转换为列表 VO（仅包含基本信息，用于列表展示）
     */
    @Mapping(target = "articleTitle", ignore = true)
    @Mapping(target = "articleCoverUrl", ignore = true)
    @Mapping(target = "proposer", ignore = true)
    @Mapping(target = "reviewer", ignore = true)
    @Mapping(target = "appliedVersion", ignore = true)
    ArticleSuggestionSimpleVO toListVO(ArticleEditSuggestion entity);
    
    /**
     * 批量转换为列表 VO
     */
    java.util.List<ArticleSuggestionSimpleVO> toListVOList(java.util.List<ArticleEditSuggestion> entities);
}
