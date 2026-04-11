package com.gcs.converter;



import com.gcs.dto.VideoAnnotationCreateDTO;
import com.gcs.dto.VideoAnnotationUpdateDTO;
import com.gcs.entity.ArticleVideoAnnotation;
import com.gcs.vo.UserSimpleVO;
import com.gcs.vo.VideoAnnotationVO;
import com.gcs.vo.VideoAnnotationDetailVO;
import org.mapstruct.IterableMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 视频注释转换器
 * @author 
 * @date 2026-04-05
 */
@Mapper(componentModel = "spring")
public interface VideoAnnotationConverter {

    VideoAnnotationConverter INSTANCE = Mappers.getMapper(VideoAnnotationConverter.class);

    // DTO to Entity
    ArticleVideoAnnotation toEntity(VideoAnnotationCreateDTO dto);

    // Entity to VO
    @Named("toVO")
    VideoAnnotationVO toVO(ArticleVideoAnnotation entity);

    @Named("toDetailVO")
    @Mapping(target = "creator", expression = "java(convertToUserSimpleVO(entity))")
    VideoAnnotationDetailVO toDetailVO(ArticleVideoAnnotation entity);

    // List conversion
    @IterableMapping(qualifiedByName = "toVO")
    List<VideoAnnotationVO> toVOList(List<ArticleVideoAnnotation> entities);

    @IterableMapping(qualifiedByName = "toDetailVO")
    List<VideoAnnotationDetailVO> toDetailVOList(List<ArticleVideoAnnotation> entities);

    // Update existing entity
    void updateEntity(VideoAnnotationUpdateDTO dto, @MappingTarget ArticleVideoAnnotation entity);

    // 手动转换方法
    default UserSimpleVO convertToUserSimpleVO(ArticleVideoAnnotation annotation) {
        if (annotation == null || annotation.getCreatorId() == null) {
            return null;
        }
        UserSimpleVO userVO = new UserSimpleVO();
        userVO.setId(annotation.getCreatorId());
        return userVO;
    }
}
