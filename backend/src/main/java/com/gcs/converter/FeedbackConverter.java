package com.gcs.converter;

import com.gcs.dto.FeedbackCreateDTO;
import com.gcs.dto.FeedbackUpdateDTO;
import com.gcs.dto.FeedbackDTO;
import com.gcs.entity.Feedback;
import com.gcs.vo.FeedbackVO;
import com.gcs.vo.FeedbackDetailVO;
import com.gcs.vo.FeedbackWithUserVO;
import com.gcs.vo.FeedbackStatisticsVO;
import org.mapstruct.IterableMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * Feedback 对象转换器
 */
@Mapper(componentModel = "spring")
public interface FeedbackConverter {
    
    FeedbackConverter INSTANCE = Mappers.getMapper(FeedbackConverter.class);
    
    // DTO to Entity
    Feedback toEntity(FeedbackDTO dto);
    
    Feedback toEntity(FeedbackCreateDTO dto);
    
    Feedback toEntity(FeedbackUpdateDTO dto);
    
    // Entity to VO - 使用 Named 注解明确方法名
    @Named("toVO")
    FeedbackVO toVO(Feedback entity);
    
    @Named("toDetailVO")
    FeedbackDetailVO toDetailVO(Feedback entity);
    
    @Named("toFeedbackWithUserVO")
    FeedbackWithUserVO toFeedbackWithUserVO(Feedback entity);
    
    @Named("toStatisticsVO")
    FeedbackStatisticsVO toStatisticsVO(Feedback entity);
    
    // List conversion - 明确指定使用的方法
    @IterableMapping(qualifiedByName = "toVO")
    List<FeedbackVO> toVOList(List<Feedback> entities);
    
    @IterableMapping(qualifiedByName = "toDetailVO")
    List<FeedbackDetailVO> toDetailVOList(List<Feedback> entities);
    
    @IterableMapping(qualifiedByName = "toFeedbackWithUserVO")
    List<FeedbackWithUserVO> toFeedbackWithUserVOList(List<Feedback> entities);
    
    // Update existing entity
    void updateEntity(FeedbackUpdateDTO dto, @MappingTarget Feedback entity);
}
