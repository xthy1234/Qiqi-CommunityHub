package com.gcs.converter;

import com.gcs.dto.AdminUserUpdateDTO;
import com.gcs.dto.UserRegisterDTO;
import com.gcs.dto.UserUpdateDTO;
import com.gcs.dto.UserDTO;
import com.gcs.entity.User;
import com.gcs.vo.*;
import org.mapstruct.IterableMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
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
    
    // ==================== DTO to Entity ====================
    User toEntity(UserDTO dto);
    
    User toEntity(UserRegisterDTO dto);
    
    User toEntity(UserUpdateDTO dto);
    
    User toEntity(AdminUserUpdateDTO dto);
    
    // ==================== Entity to VO ====================
    @Named("toVO")
    UserVO toVO(User entity);
    
    @Named("toDetailVO")
    UserDetailVO toDetailVO(User entity);
    
    @Named("toProfileVO")
    UserProfileVO toProfileVO(User entity);
    
    /**
     * User 转 UserPublicProfileVO
     */
    @Named("toPublicProfileVO")
    @Mapping(target = "followerCount", ignore = true)
    @Mapping(target = "followingCount", ignore = true)
    @Mapping(target = "articleCount", ignore = true)
    @Mapping(target = "isFollowed", ignore = true)
    UserPublicProfileVO toPublicProfileVO(User entity);
    
    /**
     * User 转 UserPublicListVO
     */
    @Named("toPublicListVO")
    @Mapping(target = "followerCount", ignore = true)
    @Mapping(target = "followingCount", ignore = true)
    @Mapping(target = "articleCount", ignore = true)
    @Mapping(target = "isFollowing", ignore = true)
    @Mapping(target = "isSelf", ignore = true)
    UserPublicListVO toPublicListVO(User entity);
    
    /**
     * User 列表转 UserPublicListVO 列表
     */
    List<UserPublicListVO> toPublicListVOList(List<User> entities);
    
    @Named("toSimpleVO")
    @Mapping(target = "lastOnlineTime", source = "lastOnlineTime")
    UserSimpleVO toSimpleVO(User entity);
    
    // ==================== List conversion ====================
    @IterableMapping(qualifiedByName = "toVO")
    List<UserVO> toVOList(List<User> entities);
    
    @IterableMapping(qualifiedByName = "toDetailVO")
    List<UserDetailVO> toDetailVOList(List<User> entities);
    
    @IterableMapping(qualifiedByName = "toProfileVO")
    List<UserProfileVO> toProfileVOList(List<User> entities);
    
    @IterableMapping(qualifiedByName = "toPublicProfileVO")
    List<UserPublicProfileVO> toPublicProfileVOList(List<User> entities);
    
    @IterableMapping(qualifiedByName = "toSimpleVO")
    List<UserSimpleVO> toSimpleVOList(List<User> entities);
    
    // ==================== Update existing entity ====================
    @org.mapstruct.Mapping(target = "id", ignore = true)
    void updateEntity(UserUpdateDTO dto, @MappingTarget User entity);
    
    @org.mapstruct.Mapping(target = "id", ignore = true)
    void updateEntity(AdminUserUpdateDTO dto, @MappingTarget User entity);
}
