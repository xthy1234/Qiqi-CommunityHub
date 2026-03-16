package com.gcs.converter;

import com.gcs.dto.UserRegisterDTO;
import com.gcs.dto.UserUpdateDTO;
import com.gcs.dto.UserDTO;
import com.gcs.entity.User;
import com.gcs.vo.UserVO;
import com.gcs.vo.UserDetailVO;
import com.gcs.vo.UserProfileVO;
import org.mapstruct.IterableMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * User 对象转换器
 */
@Mapper(componentModel = "spring")
public interface UserConverter {
    
    UserConverter INSTANCE = Mappers.getMapper(UserConverter.class);
    
    // DTO to Entity
    User toEntity(UserDTO dto);
    
    User toEntity(UserRegisterDTO dto);
    
    User toEntity(UserUpdateDTO dto);
    
    // Entity to VO - 使用 Named 注解明确方法名
    @Named("toVO")
    UserVO toVO(User entity);
    
    @Named("toDetailVO")
    UserDetailVO toDetailVO(User entity);
    
    @Named("toProfileVO")
    UserProfileVO toProfileVO(User entity);
    
    // List conversion - 明确指定使用的方法
    @IterableMapping(qualifiedByName = "toVO")
    List<UserVO> toVOList(List<User> entities);
    
    @IterableMapping(qualifiedByName = "toDetailVO")
    List<UserDetailVO> toDetailVOList(List<User> entities);
    
    @IterableMapping(qualifiedByName = "toProfileVO")
    List<UserProfileVO> toProfileVOList(List<User> entities);
    
    // Update existing entity
    void updateEntity(UserUpdateDTO dto, @MappingTarget User entity);
}
