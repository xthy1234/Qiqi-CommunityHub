package com.gcs.utils;

import com.gcs.converter.UserConverter;
import com.gcs.entity.User;
import com.gcs.service.UserService;
import com.gcs.vo.UserSimpleVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 用户信息工具类
 * 提供常用的用户信息转换和查询方法
 */
@Slf4j
@Component
public class UserUtils {
    
    private static UserService userService;
    private static UserConverter userConverter;
    
    @Autowired
    public void setUserService(UserService userService) {
        UserUtils.userService = userService;
    }
    
    @Autowired
    public void setUserConverter(UserConverter userConverter) {
        UserUtils.userConverter = userConverter;
    }
    
    /**
     * 根据用户 ID 获取 UserSimpleVO
     * @param userId 用户 ID
     * @return UserSimpleVO，如果用户不存在返回 null
     */
    public static UserSimpleVO getSimpleVOById(Long userId) {
        if (userId == null) {
            return null;
        }
        
        try {
            User user = userService.getById(userId);
            return user != null ? userConverter.toSimpleVO(user) : null;
        } catch (Exception e) {
            log.error("获取用户简单信息失败，userId: {}", userId, e);
            return null;
        }
    }
    
    /**
     * 将 User 实体转换为 UserSimpleVO
     * @param user 用户实体
     * @return UserSimpleVO
     */
    public static UserSimpleVO toSimpleVO(User user) {
        return user != null ? userConverter.toSimpleVO(user) : null;
    }
    
    /**
     * 批量将 User 实体列表转换为 UserSimpleVO 列表
     * @param users 用户实体列表
     * @return UserSimpleVO 列表
     */
    public static List<UserSimpleVO> toSimpleVOList(List<User> users) {
        if (users == null || users.isEmpty()) {
            return List.of();
        }
        return users.stream()
            .map(userConverter::toSimpleVO)
            .collect(Collectors.toList());
    }
    
    /**
     * 根据用户 ID 列表批量获取 UserSimpleVO 列表
     * @param userIds 用户 ID 列表
     * @return UserSimpleVO 列表
     */
    public static List<UserSimpleVO> getSimpleVOByIds(List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return List.of();
        }
        
        try {
            List<User> users = userService.listByIds(userIds);
            return toSimpleVOList(users);
        } catch (Exception e) {
            log.error("批量获取用户简单信息失败，userIds: {}", userIds, e);
            return List.of();
        }
    }
}
