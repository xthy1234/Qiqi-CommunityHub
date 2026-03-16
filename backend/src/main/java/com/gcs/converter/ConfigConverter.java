package com.gcs.converter;

import com.gcs.dto.ConfigCreateDTO;
import com.gcs.dto.ConfigUpdateDTO;
import com.gcs.entity.Config;
import com.gcs.vo.ConfigVO;
import com.gcs.vo.ConfigDetailVO;
import org.mapstruct.IterableMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * Config 对象转换器
 */
@Mapper(componentModel = "spring")
public interface ConfigConverter {
    
    ConfigConverter INSTANCE = Mappers.getMapper(ConfigConverter.class);
    
    // DTO to Entity
    Config toEntity(ConfigCreateDTO dto);
    
    Config toEntity(ConfigUpdateDTO dto);
    
    // Entity to VO - 使用 Named 注解明确方法名
    @Named("toVO")
    ConfigVO toVO(Config entity);
    
    @Named("toDetailVO")
    ConfigDetailVO toDetailVO(Config entity);
    
    // List conversion - 明确指定使用的方法
    @IterableMapping(qualifiedByName = "toVO")
    List<ConfigVO> toVOList(List<Config> entities);
    
    @IterableMapping(qualifiedByName = "toDetailVO")
    List<ConfigDetailVO> toDetailVOList(List<Config> entities);
    
    // Update existing entity
    void updateEntity(ConfigUpdateDTO dto, @MappingTarget Config entity);
}
