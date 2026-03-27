package com.gcs.utils;

import com.gcs.entity.User;
import com.gcs.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
public class AuthUtils {
    
    @Autowired
    private UserService userService;
    
    /**
     * 检查用户是否为管理员
     */
    public boolean isAdmin(Long userId) {
        try {
            User user = userService.getById(userId);
            if (user == null) {
                return false;
            }
            // 可配置的角色 ID
            return user.getRoleId() != null && user.getRoleId() == 2L;
        } catch (Exception e) {
//            log.error("检查管理员权限失败，userId: {}", userId, e);
            return false;
        }
    }
    
    /**
     * 检查是否为资源所有者
     */
    public boolean isOwner(Long resourceId, Long ownerId, Long currentUserId) {
        return Objects.equals(ownerId, currentUserId);
    }
    
    /**
     * 检查是否有操作权限（管理员或所有者）
     */
    public boolean hasPermission(Long resourceId, Long ownerId, Long currentUserId) {
        return isAdmin(currentUserId) || isOwner(resourceId, ownerId, currentUserId);
    }
}
