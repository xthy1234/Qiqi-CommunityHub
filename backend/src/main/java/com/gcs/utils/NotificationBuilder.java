package com.gcs.utils;

import com.gcs.vo.UserSimpleVO;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

/**
 * 通知数据构建工具类
 * 用于构建标准化的通知 extra 字段
 */
@Slf4j
public class NotificationBuilder {
    
    /**
     * 构建评论通知的 extra 数据
     */
    public static Map<String, Object> buildCommentNotification(Long articleId, Long commentId, 
                                                               UserSimpleVO commenter, String content) {
        Map<String, Object> extra = new HashMap<>();
        extra.put("articleId", articleId);
        extra.put("commentId", commentId);
        extra.put("commenter", convertUserToMap(commenter));
        extra.put("content", content);
        return extra;
    }
    
    /**
     * 构建点赞通知的 extra 数据
     */
    public static Map<String, Object> buildLikeNotification(String targetType, Long articleId, 
                                                           Long commentId, UserSimpleVO liker) {
        Map<String, Object> extra = new HashMap<>();
        extra.put("targetType", targetType); // "article" 或 "comment"
        extra.put("articleId", articleId);
        if (commentId != null) {
            extra.put("commentId", commentId);
        }
        extra.put("liker", convertUserToMap(liker));
        return extra;
    }
    
    /**
     * 构建关注通知的 extra 数据
     */
    public static Map<String, Object> buildFollowNotification(UserSimpleVO follower) {
        Map<String, Object> extra = new HashMap<>();
        extra.put("follower", convertUserToMap(follower));
        return extra;
    }
    
    /**
     * 构建回复通知的 extra 数据
     */
    public static Map<String, Object> buildReplyNotification(Long articleId, Long commentId, 
                                                            UserSimpleVO replier, String content) {
        Map<String, Object> extra = new HashMap<>();
        extra.put("articleId", articleId);
        extra.put("commentId", commentId);
        extra.put("replier", convertUserToMap(replier));
        extra.put("content", content);
        return extra;
    }
    
    /**
     * 构建文章审核通知的 extra 数据
     */
    public static Map<String, Object> buildArticleAuditNotification(Long articleId, String articleTitle, 
                                                                   String result) {
        Map<String, Object> extra = new HashMap<>();
        extra.put("articleId", articleId);
        extra.put("articleTitle", articleTitle);
        extra.put("result", result); // "通过" 或 "拒绝"
        return extra;
    }
    
    /**
     * 构建圈子邀请通知的 extra 数据
     */
    public static Map<String, Object> buildCircleInviteNotification(Long circleId, String circleName, 
                                                                   UserSimpleVO inviter) {
        Map<String, Object> extra = new HashMap<>();
        extra.put("circleId", circleId);
        extra.put("circleName", circleName);
        extra.put("inviter", convertUserToMap(inviter));
        return extra;
    }
    
    /**
     * 构建被移出圈子通知的 extra 数据
     */
    public static Map<String, Object> buildCircleRemovedNotification(Long circleId, String circleName, 
                                                                    UserSimpleVO operator) {
        Map<String, Object> extra = new HashMap<>();
        extra.put("circleId", circleId);
        extra.put("circleName", circleName);
        extra.put("operator", convertUserToMap(operator));
        return extra;
    }
    
    /**
     * 构建圈子申请加入通知的 extra 数据
     */
    public static Map<String, Object> buildCircleJoinNotification(Long circleId, String circleName, 
                                                                 UserSimpleVO applicant) {
        Map<String, Object> extra = new HashMap<>();
        extra.put("circleId", circleId);
        extra.put("circleName", circleName);
        extra.put("applicant", convertUserToMap(applicant));
        return extra;
    }
    
    /**
     * 构建新成员加入通知的 extra 数据
     */
    public static Map<String, Object> buildMemberJoinNotification(Long circleId, String circleName, 
                                                                 UserSimpleVO newMember) {
        Map<String, Object> extra = new HashMap<>();
        extra.put("circleId", circleId);
        extra.put("circleName", circleName);
        extra.put("newMember", convertUserToMap(newMember));
        return extra;
    }
    
    /**
     * 构建成员退出通知的 extra 数据
     */
    public static Map<String, Object> buildMemberQuitNotification(Long circleId, String circleName, 
                                                                 UserSimpleVO quitter) {
        Map<String, Object> extra = new HashMap<>();
        extra.put("circleId", circleId);
        extra.put("circleName", circleName);
        extra.put("quitter", convertUserToMap(quitter));
        return extra;
    }
    
    /**
     * 构建成员被移除通知的 extra 数据
     */
    public static Map<String, Object> buildMemberRemovedNotification(Long circleId, String circleName, 
                                                                    UserSimpleVO removedUser, 
                                                                    UserSimpleVO operator) {
        Map<String, Object> extra = new HashMap<>();
        extra.put("circleId", circleId);
        extra.put("circleName", circleName);
        extra.put("removedUser", convertUserToMap(removedUser));
        extra.put("operator", convertUserToMap(operator));
        return extra;
    }
    
    /**
     * 构建建议提交通知的 extra 数据
     */
    public static Map<String, Object> buildSuggestionSubmitNotification(Long articleId, Long suggestionId, 
                                                                       UserSimpleVO proposer) {
        Map<String, Object> extra = new HashMap<>();
        extra.put("articleId", articleId);
        extra.put("suggestionId", suggestionId);
        extra.put("proposer", convertUserToMap(proposer));
        return extra;
    }
    
    /**
     * 构建建议审核结果通知的 extra 数据
     */
    public static Map<String, Object> buildSuggestionReviewNotification(Long articleId, Long suggestionId, 
                                                                       String result) {
        Map<String, Object> extra = new HashMap<>();
        extra.put("articleId", articleId);
        extra.put("suggestionId", suggestionId);
        extra.put("result", result); // "通过" 或 "拒绝"
        return extra;
    }
    
    /**
     * 构建系统通知的 extra 数据
     */
    public static Map<String, Object> buildSystemMessageNotification(String title, String content, 
                                                                    String link) {
        Map<String, Object> extra = new HashMap<>();
        extra.put("title", title);
        extra.put("content", content);
        if (link != null) {
            extra.put("link", link);
        }
        return extra;
    }
    
    /**
     * 将 UserSimpleVO 转换为 Map
     */
    private static Map<String, Object> convertUserToMap(UserSimpleVO user) {
        if (user == null) {
            return null;
        }
        
        Map<String, Object> map = new HashMap<>();
        map.put("id", user.getId());
        map.put("nickname", user.getNickname());
        map.put("avatar", user.getAvatar());
        // 暂时不包含 lastOnlineTime，避免序列化问题
        // map.put("lastOnlineTime", user.getLastOnlineTime());
        return map;
    }
}
