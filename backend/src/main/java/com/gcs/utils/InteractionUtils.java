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
        if (userId == null || contentId == null) {
            return false;
        }
        
        try {
            return interactionService.hasValidInteraction(
                userId, contentId, InteractionActionType.LIKE, contentType);
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * 检查用户是否已收藏
     */
    public Boolean hasFavorited(Long userId, Long contentId, ContentType contentType) {
        if (userId == null || contentId == null) {
            return false;
        }
        
        try {
            return interactionService.hasValidInteraction(
                userId, contentId, InteractionActionType.FAVORITE, contentType);
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * 检查用户是否已点踩
     */
    public Boolean hasDisliked(Long userId, Long contentId, ContentType contentType) {
        if (userId == null || contentId == null) {
            return false;
        }
        
        try {
            return interactionService.hasValidInteraction(
                userId, contentId, InteractionActionType.DISLIKE, contentType);
        } catch (Exception e) {
            return false;
        }
    }
}
