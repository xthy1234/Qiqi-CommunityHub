package com.gcs.converter;

import com.gcs.dto.ArticleCreateDTO;
import com.gcs.dto.ArticleUpdateDTO;
import com.gcs.dto.ArticleDTO;
import com.gcs.entity.Article;
import com.gcs.vo.ArticleVO;
import com.gcs.vo.ArticleDetailVO;
import org.mapstruct.IterableMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * Article 对象转换器
 */
@Mapper(componentModel = "spring")
public interface ArticleConverter {
    
    ArticleConverter INSTANCE = Mappers.getMapper(ArticleConverter.class);
    
    // DTO to Entity
    Article toEntity(ArticleDTO dto);
    
    Article toEntity(ArticleCreateDTO dto);
    
    Article toEntity(ArticleUpdateDTO dto);
    
    // Entity to VO - 使用 Named 注解明确方法名
    @Named("toVO")
    ArticleVO toVO(Article entity);
    
    @Named("toDetailVO")
    ArticleDetailVO toDetailVO(Article entity);
    
    // List conversion - 明确指定使用的方法
    @IterableMapping(qualifiedByName = "toVO")
    List<ArticleVO> toVOList(List<Article> entities);
    
    @IterableMapping(qualifiedByName = "toDetailVO")
    List<ArticleDetailVO> toDetailVOList(List<Article> entities);
    
    // Update existing entity
    void updateEntity(ArticleUpdateDTO dto, @MappingTarget Article entity);
}
