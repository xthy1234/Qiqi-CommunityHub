package com.gcs.converter;

import com.gcs.dto.SwiperCreateDTO;
import com.gcs.dto.SwiperUpdateDTO;
import com.gcs.entity.Swiper;
import com.gcs.vo.SwiperVO;
import com.gcs.vo.SwiperDetailVO;
import org.mapstruct.IterableMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * Swiper 对象转换器
 */
@Mapper(componentModel = "spring")
public interface SwiperConverter {
    
    SwiperConverter INSTANCE = Mappers.getMapper(SwiperConverter.class);
    
    // DTO to Entity
    Swiper toEntity(SwiperCreateDTO dto);
    
    Swiper toEntity(SwiperUpdateDTO dto);
    
    // Entity to VO - 使用 Named 注解明确方法名
    @Named("toVO")
    SwiperVO toVO(Swiper entity);
    
    @Named("toDetailVO")
    SwiperDetailVO toDetailVO(Swiper entity);
    
    // List conversion - 明确指定使用的方法
    @IterableMapping(qualifiedByName = "toVO")
    List<SwiperVO> toVOList(List<Swiper> entities);
    
    @IterableMapping(qualifiedByName = "toDetailVO")
    List<SwiperDetailVO> toDetailVOList(List<Swiper> entities);
    
    // Update existing entity
    void updateEntity(SwiperUpdateDTO dto, @MappingTarget Swiper entity);
}
