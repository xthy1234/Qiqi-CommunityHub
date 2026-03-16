package com.gcs.converter;

import com.gcs.dto.RoleCreateDTO;
import com.gcs.dto.RoleUpdateDTO;
import com.gcs.entity.Role;
import com.gcs.vo.RoleVO;
import com.gcs.vo.RoleDetailVO;
import org.mapstruct.IterableMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * Role 对象转换器
 */
@Mapper(componentModel = "spring")
public interface RoleConverter {
    
    RoleConverter INSTANCE = Mappers.getMapper(RoleConverter.class);
    
    // DTO to Entity
    Role toEntity(RoleCreateDTO dto);
    
    Role toEntity(RoleUpdateDTO dto);
    
    // Entity to VO - 使用 Named 注解明确方法名
    @Named("toVO")
    RoleVO toVO(Role entity);
    
    @Named("toDetailVO")
    RoleDetailVO toDetailVO(Role entity);
    
    // List conversion - 明确指定使用的方法
    @IterableMapping(qualifiedByName = "toVO")
    List<RoleVO> toVOList(List<Role> entities);
    
    @IterableMapping(qualifiedByName = "toDetailVO")
    List<RoleDetailVO> toDetailVOList(List<Role> entities);
    
    // Update existing entity
    void updateEntity(RoleUpdateDTO dto, @MappingTarget Role entity);
}
