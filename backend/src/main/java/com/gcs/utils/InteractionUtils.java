package com.gcs.utils;

import com.gcs.entity.Interaction;
import com.gcs.enums.ContentType;
import com.gcs.enums.InteractionActionType;
import com.gcs.service.InteractionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class InteractionUtils {
    
    @Autowired
    private InteractionService interactionService;
    
    /**
     * 检查用户是否已点赞
     */
    public Boolean hasLiked(Long userId, Long contentId, ContentType contentType) {
        return hasInteraction(userId, contentId, InteractionActionType.LIKE, contentType);
    }
    
    /**
     * 检查用户是否已收藏
     */
    public Boolean hasFavorited(Long userId, Long contentId, ContentType contentType) {
        return hasInteraction(userId, contentId, InteractionActionType.FAVORITE, contentType);
    }
    
    /**
     * 检查用户是否已点踩
     */
    public Boolean hasDisliked(Long userId, Long contentId, ContentType contentType) {
        return hasInteraction(userId, contentId, InteractionActionType.DISLIKE, contentType);
    }
    
    /**
     * 通用互动检查方法
     */
    private Boolean hasInteraction(Long userId, Long contentId, 
                                   InteractionActionType actionType, ContentType contentType) {
        if (userId == null || contentId == null) {
            return false;
        }
        
        try {
            List<Interaction> interactions = interactionService.getUserInteractionsList(
                userId, actionType, contentType);
            
            if (interactions == null || interactions.isEmpty()) {
                return false;
            }
            
            return interactions.stream()
                .anyMatch(i -> i.getContentId().equals(contentId));
        } catch (Exception e) {
//            log.error("检查互动状态失败", e);
            return false;
        }
    }
}
