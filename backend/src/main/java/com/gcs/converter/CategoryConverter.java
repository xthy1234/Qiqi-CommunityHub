package com.gcs.converter;

import com.gcs.dto.CategoryCreateDTO;
import com.gcs.dto.CategoryUpdateDTO;
import com.gcs.dto.CategoryDTO;
import com.gcs.entity.Category;
import com.gcs.vo.CategoryVO;
import com.gcs.vo.CategoryDetailVO;
import com.gcs.vo.CategoryTreeVO;
import org.mapstruct.IterableMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * Category 对象转换器
 */
@Mapper(componentModel = "spring")
public interface CategoryConverter {
    
    CategoryConverter INSTANCE = Mappers.getMapper(CategoryConverter.class);
    
    // DTO to Entity
    Category toEntity(CategoryDTO dto);
    
    Category toEntity(CategoryCreateDTO dto);
    
    Category toEntity(CategoryUpdateDTO dto);
    
    // Entity to VO - 使用 Named 注解明确方法名
    @Named("toVO")
    CategoryVO toVO(Category entity);
    
    @Named("toDetailVO")
    CategoryDetailVO toDetailVO(Category entity);
    
    @Named("toTreeVO")
    CategoryTreeVO toTreeVO(Category entity);
    
    // List conversion - 明确指定使用的方法
    @IterableMapping(qualifiedByName = "toVO")
    List<CategoryVO> toVOList(List<Category> entities);
    
    @IterableMapping(qualifiedByName = "toDetailVO")
    List<CategoryDetailVO> toDetailVOList(List<Category> entities);
    
    @IterableMapping(qualifiedByName = "toTreeVO")
    List<CategoryTreeVO> toTreeVOList(List<Category> entities);
    
    // Update existing entity
    void updateEntity(CategoryUpdateDTO dto, @MappingTarget Category entity);
}
