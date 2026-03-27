package com.gcs.converter;

import com.gcs.dto.UserRegisterDTO;
import com.gcs.entity.User;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.springframework.util.StringUtils;

@Mapper(componentModel = "spring")
public abstract class UserRegisterConverter {
    
    public abstract User toEntity(UserRegisterDTO dto);
    
    @AfterMapping
    protected void afterMapping(UserRegisterDTO dto, @MappingTarget User user) {
        if (!StringUtils.hasText(dto.getNickname())) {
            String accountPrefix = dto.getAccount().split("@")[0];
            user.setNickname(accountPrefix);
        }
        
        if (!StringUtils.hasText(dto.getAvatar())) {
            user.setAvatar("http://localhost:8080/server/upload/default_avatar.jpg");
        }
        
        if (!StringUtils.hasText(dto.getEmail())) {
            user.setEmail(null);
        }
    }
}
