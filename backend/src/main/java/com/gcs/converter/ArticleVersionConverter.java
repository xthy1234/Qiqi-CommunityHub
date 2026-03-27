package com.gcs.converter;

import com.gcs.entity.ArticleVersion;
import com.gcs.vo.ArticleVersionSimpleVO;
import com.gcs.vo.ArticleVersionVO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

/**
 * 文章版本 Converter 接口
 * 用于 Entity 与 VO 之间的转换
 */
@Mapper(componentModel = "spring")
public interface ArticleVersionConverter {
    
    ArticleVersionConverter INSTANCE = Mappers.getMapper(ArticleVersionConverter.class);
    
    // ==================== 简略 VO ====================
    
    /**
     * 将 Entity 转换为简略 VO（用于列表展示）
     */
    @Mapping(target = "operator", ignore = true)
    ArticleVersionSimpleVO toSimpleVO(ArticleVersion entity);
    
    /**
     * 批量转换为简略 VO
     */
    java.util.List<ArticleVersionSimpleVO> toSimpleVOList(java.util.List<ArticleVersion> entities);
    
    // ==================== 详细 VO ====================
    
    /**
     * 将 Entity 转换为详细 VO（用于详情页和对比）
     */
    @Mapping(target = "operator", ignore = true)
    @Mapping(target = "isLatest", ignore = true)
    ArticleVersionVO toDetailVO(ArticleVersion entity);
    
    /**
     * 批量转换为详细 VO
     */
    java.util.List<ArticleVersionVO> toDetailVOList(java.util.List<ArticleVersion> entities);

    // 在转换方法中添加格式化版本号
    private String formatVersion(Integer major, Integer minor) {
        return major + "." + minor;
    }

}
