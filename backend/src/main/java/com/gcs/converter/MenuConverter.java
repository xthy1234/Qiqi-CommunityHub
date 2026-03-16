package com.gcs.converter;

import com.gcs.dto.MenuCreateDTO;
import com.gcs.dto.MenuUpdateDTO;
import com.gcs.entity.Menu;
import com.gcs.vo.MenuVO;
import com.gcs.vo.MenuDetailVO;
import com.gcs.vo.MenuTreeVO;
import org.mapstruct.IterableMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * Menu 对象转换器
 */
@Mapper(componentModel = "spring")
public interface MenuConverter {
    
    MenuConverter INSTANCE = Mappers.getMapper(MenuConverter.class);
    
    // DTO to Entity
    Menu toEntity(MenuCreateDTO dto);
    
    Menu toEntity(MenuUpdateDTO dto);
    
    // Entity to VO - 使用 Named 注解明确方法名
    @Named("toVO")
    MenuVO toVO(Menu entity);
    
    @Named("toDetailVO")
    MenuDetailVO toDetailVO(Menu entity);
    
    @Named("toTreeVO")
    MenuTreeVO toTreeVO(Menu entity);
    
    // List conversion - 明确指定使用的方法
    @IterableMapping(qualifiedByName = "toVO")
    List<MenuVO> toVOList(List<Menu> entities);
    
    @IterableMapping(qualifiedByName = "toDetailVO")
    List<MenuDetailVO> toDetailVOList(List<Menu> entities);
    
    @IterableMapping(qualifiedByName = "toTreeVO")
    List<MenuTreeVO> toTreeVOList(List<Menu> entities);
    
    // Update existing entity
    void updateEntity(MenuUpdateDTO dto, @MappingTarget Menu entity);
}
