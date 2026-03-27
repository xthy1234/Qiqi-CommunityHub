package com.gcs.converter;

import com.gcs.entity.ArticleDraft;
import com.gcs.vo.ArticleDraftVO;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring")
public interface ArticleDraftConverter {
    
    ArticleDraftConverter INSTANCE = Mappers.getMapper(ArticleDraftConverter.class);
    
    @Mapping(target = "author", ignore = true)
    @Mapping(source = "createTime", target = "createdAt")
    @Mapping(source = "updateTime", target = "updatedAt")
    ArticleDraftVO toVO(ArticleDraft entity);
}
