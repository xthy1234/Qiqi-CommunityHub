package com.gcs.converter;

import com.gcs.dto.InteractionCreateDTO;
import com.gcs.dto.InteractionUpdateDTO;
import com.gcs.dto.InteractionLikeDTO;
import com.gcs.dto.InteractionDTO;
import com.gcs.entity.Interaction;
import com.gcs.vo.InteractionVO;
import com.gcs.vo.InteractionDetailVO;
import com.gcs.vo.InteractionStatisticsVO;
import com.gcs.vo.InteractionUserActionVO;
import org.mapstruct.IterableMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * Interaction 对象转换器
 */
@Mapper(componentModel = "spring")
public interface InteractionConverter {
    
    InteractionConverter INSTANCE = Mappers.getMapper(InteractionConverter.class);
    
    // DTO to Entity
    Interaction toEntity(InteractionDTO dto);
    
    Interaction toEntity(InteractionCreateDTO dto);
    
    Interaction toEntity(InteractionLikeDTO dto);
    
    // Entity to VO - 使用 Named 注解明确方法名
    @Named("toVO")
    InteractionVO toVO(Interaction entity);
    
    @Named("toDetailVO")
    InteractionDetailVO toDetailVO(Interaction entity);
    
    @Named("toStatisticsVO")
    InteractionStatisticsVO toStatisticsVO(Interaction entity);
    
    @Named("toUserActionVO")
    InteractionUserActionVO toUserActionVO(Interaction entity);
    
    // List conversion - 明确指定使用的方法
    @IterableMapping(qualifiedByName = "toVO")
    List<InteractionVO> toVOList(List<Interaction> entities);
    
    @IterableMapping(qualifiedByName = "toDetailVO")
    List<InteractionDetailVO> toDetailVOList(List<Interaction> entities);
    
    // Update existing entity
    void updateEntity(InteractionUpdateDTO dto, @MappingTarget Interaction entity);
}
