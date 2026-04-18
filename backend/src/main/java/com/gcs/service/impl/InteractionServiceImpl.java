package com.gcs.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.gcs.entity.Article;
import com.gcs.entity.Comment;
import com.gcs.enums.ContentType;
import com.gcs.enums.InteractionActionType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;


import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.gcs.utils.PageUtils;
import com.gcs.utils.Query;

import com.gcs.dao.InteractionDao;
import com.gcs.entity.Interaction;
import com.gcs.service.InteractionService;
import com.gcs.entity.view.InteractionView;
import com.gcs.service.NotificationService;
import com.gcs.dao.ArticleDao;
import com.gcs.dao.CommentDao;
import com.gcs.dao.UserDao;
import com.gcs.entity.Article;
import com.gcs.entity.Comment;
import com.gcs.entity.User;
import com.gcs.enums.NotificationType;
import com.gcs.utils.NotificationBuilder;
import com.gcs.vo.UserSimpleVO;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import lombok.extern.slf4j.Slf4j;

import com.gcs.enums.InteractionStatus;

/**
 * 互动记录服务实现类
 */
@Slf4j
@Service("interactionService")
public class InteractionServiceImpl extends ServiceImpl<InteractionDao, Interaction> implements InteractionService {

    @Autowired
    private NotificationService notificationService;
    
    @Autowired
    private ArticleDao articleDao;
    
    @Autowired
    private CommentDao commentDao;
    
    @Autowired
    private UserDao userDao;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        validateParams(params);

        IPage<Interaction> interactionPage = new Query<Interaction>(params).getPage();
        IPage<Interaction> resultPage = this.page(interactionPage, new QueryWrapper<>());

        return new PageUtils(resultPage);
    }

    @Override
    public PageUtils queryPage(Map<String, Object> params, Wrapper<Interaction> queryWrapper) {
        validateQueryParams(params, queryWrapper);

        IPage<InteractionView> interactionViewPage = new Query<InteractionView>(params).getPage();
        List<InteractionView> interactions = ((InteractionDao) baseMapper).selectListView(interactionViewPage, queryWrapper);
        interactionViewPage.setRecords(interactions);

        return new PageUtils(interactionViewPage);
    }

    @Override
    public List<InteractionView> selectListView(Wrapper<Interaction> queryWrapper) {
        validateWrapper(queryWrapper);
        return ((InteractionDao) baseMapper).selectListView(null, queryWrapper);
    }

    @Override
    public InteractionView selectView(Wrapper<Interaction> queryWrapper) {
        validateWrapper(queryWrapper);
        return ((InteractionDao) baseMapper).selectView(queryWrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean addInteraction(Interaction interaction) {
        validateInteractionForCreate(interaction);
        
        log.info("开始添加互动记录：userId={}, contentId={}, actionType={}, tableName={}", 
                interaction.getUserId(), interaction.getContentId(), interaction.getActionType(), 
                interaction.getTableName());

        Set<Long> cancelledOppositeIds = new HashSet<>();
        
        if (interaction.getActionType() == InteractionActionType.LIKE || 
            interaction.getActionType() == InteractionActionType.DISLIKE) {
            log.info("检测到点赞/点踩操作，执行取消相反操作");
            cancelledOppositeIds = cancelOppositeAction(interaction.getUserId(), interaction.getContentId(), 
                                interaction.getTableName(), interaction.getActionType());
        }
        
        List<Interaction> existingList = baseMapper.selectByUserAndContentList(
            interaction.getUserId(), 
            interaction.getContentId(), 
            interaction.getActionType(),
            interaction.getTableName()
        );
        
        log.info("查询互动记录：userId={}, contentId={}, actionType={}, tableName={}, 结果数量={}", 
                interaction.getUserId(), interaction.getContentId(), interaction.getActionType(), 
                interaction.getTableName(), existingList != null ? existingList.size() : 0);
        
        if (existingList != null && !existingList.isEmpty()) {
            log.info("找到 {} 条历史记录，开始遍历检查", existingList.size());

            for (Interaction existing : existingList) {
                log.info("检查记录：id={}, isDeleted={}, status={}", 
                        existing.getId(), existing.getIsDeleted(), existing.getStatus());
                
                if (!Boolean.TRUE.equals(existing.getIsDeleted())) {
                    if (existing.getStatus() == InteractionStatus.VALID) {
                        log.info("互动记录已存在且有效，跳过操作");
                        return true;
                    } else if (!cancelledOppositeIds.contains(existing.getId())) {
                        log.info("重新激活互动记录：id={}, userId={}, contentId={}, actionType={}, 原status={}", 
                                existing.getId(), existing.getUserId(), existing.getContentId(), 
                                existing.getActionType(), existing.getStatus());
                        
                        existing.setStatus(InteractionStatus.VALID);
                        boolean updated = this.updateById(existing);
                        
                        if (updated) {
                            updateContentLikeCount(existing.getContentId(), existing.getTableName(), 
                                                 existing.getActionType(), +1);
                            log.info("重新激活互动记录成功");
                        }
                        return updated;
                    } else {
                        log.info("跳过已被取消的相反操作记录：id={}", existing.getId());
                    }
                }
            }
        }
        
        log.info("未找到现有记录，创建新的互动记录");
        interaction.setStatus(InteractionStatus.VALID);
        interaction.setIsDeleted(false);
        
        boolean saved = this.save(interaction);
        
        if (saved) {
            updateContentLikeCount(interaction.getContentId(), interaction.getTableName(), 
                                 interaction.getActionType(), +1);
            sendLikeNotification(interaction);
            log.info("创建新的互动记录成功");
        }
        
        return saved;
    }

    private Set<Long> cancelOppositeAction(Long userId, Long contentId, ContentType tableName,
                                           InteractionActionType currentActionType) {
        Set<Long> cancelledIds = new HashSet<>();
        
        if (userId == null || contentId == null || currentActionType == null) {
            return cancelledIds;
        }
        
        // 确定相反的操作类型
        InteractionActionType oppositeActionType = null;
        if (currentActionType == InteractionActionType.LIKE) {
            oppositeActionType = InteractionActionType.DISLIKE;
        } else if (currentActionType == InteractionActionType.DISLIKE) {
            oppositeActionType = InteractionActionType.LIKE;
        }
        
        if (oppositeActionType == null) {
            return cancelledIds;
        }
        
        // 查找是否存在相反的操作（不限制 status，只要求 is_deleted=false）
        List<Interaction> oppositeList = baseMapper.selectByUserAndContentList(userId, contentId, oppositeActionType, tableName);
        if (oppositeList != null && !oppositeList.isEmpty()) {
            for (Interaction opposite : oppositeList) {
                if (!Boolean.TRUE.equals(opposite.getIsDeleted())) {
                    // 如果相反的操作是有效的（status=0），则取消它
                    if (opposite.getStatus() == InteractionStatus.VALID) {
                        opposite.setStatus(InteractionStatus.INVALID);
                        this.updateById(opposite);
                        
                        // 状态从有效变为无效，减少相应的计数
                        updateContentLikeCount(contentId, tableName, oppositeActionType, -1);
                        
                        cancelledIds.add(opposite.getId());
                        
                        log.info("自动取消相反操作：userId={}, contentId={}, oppositeActionType={}, cancelledId={}", 
                                userId, contentId, oppositeActionType, opposite.getId());
                    }
                    // 只处理第一个找到的有效记录
                    break;
                }
            }
        }
        
        return cancelledIds;
    }

    @Override
    public boolean removeInteraction(Long userId, Long contentId, InteractionActionType actionType, ContentType tableName) {
        if (userId == null || contentId == null || actionType == null || tableName == null) {
            throw new IllegalArgumentException("参数不能为空");
        }

        //  正确：传递 tableName 参数
        Interaction interaction = baseMapper.selectByUserAndContent(userId, contentId, actionType, tableName);
        if (interaction != null) {
            // 将状态标记为无效（status 从 0 变为 1）
            interaction.setStatus(InteractionStatus.INVALID);
            interaction.setUpdateTime(java.time.LocalDateTime.now());
            boolean updated = this.updateById(interaction);

            if (updated && isLikeAction(actionType)) {
                // 状态从有效变为无效，减少相应的计数
                updateContentLikeCount(contentId, interaction.getTableName(), actionType, -1);
            }

            return updated;
        }

        return true;
    }

    @Override
    public boolean existsInteraction(Long userId, Long contentId, InteractionActionType actionType,ContentType contentType) {
        if (userId == null || contentId == null || actionType== null||contentType== null) {
            return false;
        }

        // 查询所有历史记录（不限制 status）
        List<Interaction> existingList = baseMapper.selectByUserAndContentList(userId, contentId, actionType,contentType);
        
        if (existingList != null && !existingList.isEmpty()) {
            // 只要存在 is_deleted=false 的记录，就认为已操作过
            for (Interaction existing : existingList) {
                if (!Boolean.TRUE.equals(existing.getIsDeleted())) {
                    return true;
                }
            }
        }
        
        return false;
    }

    @Override
    public boolean hasValidInteraction(Long userId, Long contentId, InteractionActionType actionType, ContentType tableName) {
        if (userId == null || contentId == null || actionType== null || tableName == null) {
            return false;
        }

        Interaction interaction = baseMapper.selectByUserAndContent(userId, contentId, actionType, tableName);
        return interaction != null;
    }

    @Override
    public List<Interaction> getUserInteractionsList(Long userId, InteractionActionType actionType, ContentType tableName) {
        if (userId == null || actionType == null || tableName == null) {
            return new ArrayList<>();
        }

        QueryWrapper<Interaction> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_id", userId)
                   .eq("action_type", actionType)
                   .eq("table_name", tableName.getCode())
                   .eq("status", InteractionStatus.VALID.getCode())
                   .eq("is_deleted", false);
        
        return this.list(queryWrapper);
    }

    @Override
    public PageUtils getUserInteractions(Long userId, InteractionActionType actionType, Map<String, Object> params) {
        if (userId == null) {
            throw new IllegalArgumentException("用户 ID 不能为空");
        }

        QueryWrapper<Interaction> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_id", userId);
        queryWrapper.eq("status", 0); // 查询有效的互动记录
        queryWrapper.eq("is_deleted", false); // 未删除的记录

        if (actionType != null) {
            queryWrapper.eq("action_type", actionType);
        }

        return queryPage(params, queryWrapper);
    }

    @Override
    public Integer countUserInteractions(Long userId, InteractionActionType actionType) {
        if (userId == null) {
            return 0;
        }

        return baseMapper.countByUser(userId, actionType);
    }

    @Override
    public boolean batchRemoveInteractions(Long userId, List<Long> interactionIds) {
        if (userId == null || CollectionUtils.isEmpty(interactionIds)) {
            throw new IllegalArgumentException("参数不能为空");
        }

        int result = baseMapper.deleteBatchByUser(userId, interactionIds);
        return result > 0;
    }

    private boolean isLikeAction(InteractionActionType actionType) {
        return actionType == InteractionActionType.LIKE || actionType == InteractionActionType.DISLIKE;
    }

    /**
     * 判断是否需要更新计数的操作
     */
    private boolean isCountableAction(InteractionActionType actionType) {
        return actionType == InteractionActionType.LIKE || 
               actionType == InteractionActionType.DISLIKE ||
               actionType == InteractionActionType.FAVORITE ||
               actionType == InteractionActionType.SHARE;
    }

    private void updateContentLikeCount(Long contentId, ContentType tableName, InteractionActionType actionType, int delta) {
        if (contentId == null || !isCountableAction(actionType)) {
            return;
        }

        try {
            if (tableName == ContentType.ARTICLE) {
                updateArticleInteractionCount(contentId, actionType, delta);
            } else if (tableName == ContentType.COMMENT) {
                updateCommentInteractionCount(contentId, actionType, delta);
            }
        } catch (Exception e) {
            log.error("更新互动计数失败，ID: {}, 类型：{}, 变化：{}", contentId, actionType, delta, e);
        }
    }

    /**
     * 更新文章的互动计数（点赞、点踩、收藏、分享）
     */
    private void updateArticleInteractionCount(Long articleId, InteractionActionType actionType, int delta) {
        Article article = articleDao.selectById(articleId);
        if (article != null) {
            switch (actionType) {
                case LIKE:
                    if (delta > 0) {
                        article.addLike();
                    } else {
                        article.removeLike();
                    }
                    break;
                    
                case DISLIKE:
                    if (delta > 0) {
                        article.addDislike();
                    } else {
                        article.removeDislike();
                    }
                    break;
                    
                case FAVORITE:
                    if (delta > 0) {
                        article.addFavorite();
                    } else {
                        article.removeFavorite();
                    }
                    break;
                    
                case SHARE:
                    if (delta > 0) {
                        article.addShare();
                    } else {
                        article.removeShare();
                    }
                    break;
            }

            articleDao.updateById(article);
            log.info("更新文章互动计数成功，ID: {}, 类型：{}, 变化：{}", articleId, actionType, delta);
        }
    }

    /**
     * 更新评论的互动计数（点赞和点踩）
     */
    private void updateCommentInteractionCount(Long commentId, InteractionActionType actionType, int delta) {
        Comment comment = commentDao.selectById(commentId);
        if (comment == null) {
            return;
        }

        // 根据操作类型更新相应的计数
        switch (actionType) {
            case LIKE:
                Integer likeCount = comment.getLikeCount() != null ? comment.getLikeCount() : 0;
                comment.setLikeCount(likeCount + delta);
                
                // 确保计数不会小于 0
                if (delta < 0) {
                    comment.setLikeCount(Math.max(0, comment.getLikeCount()));
                }
                break;
                
            case DISLIKE:
                Integer dislikeCount = comment.getDislikeCount() != null ? comment.getDislikeCount() : 0;
                comment.setDislikeCount(dislikeCount + delta);
                
                // 确保计数不会小于 0
                if (delta < 0) {
                    comment.setDislikeCount(Math.max(0, comment.getDislikeCount()));
                }
                break;
                
            default:
                // 其他操作类型（收藏、分享等）不适用于评论
                log.warn("评论不支持的操作类型: {}", actionType);
                return;
        }

        commentDao.updateById(comment);
        log.info("更新评论互动计数成功，ID: {}, 类型：{}, 变化：{}", commentId, actionType, delta);
    }

    private void validateParams(Map<String, Object> params) {
        if (params == null || params.isEmpty()) {
            throw new IllegalArgumentException("查询参数不能为空");
        }
    }

    private void validateWrapper(Wrapper<Interaction> queryWrapper) {
        if (Objects.isNull(queryWrapper)) {
            throw new IllegalArgumentException("查询条件不能为空");
        }
    }

    private void validateQueryParams(Map<String, Object> params, Wrapper<Interaction> queryWrapper) {
        validateParams(params);
        validateWrapper(queryWrapper);
    }

    private void validateInteractionForCreate(Interaction interaction) {
        if (interaction == null) {
            throw new IllegalArgumentException("互动信息不能为空");
        }
        if (interaction.getUserId() == null) {
            throw new IllegalArgumentException("用户 ID 不能为空");
        }
        if (interaction.getContentId() == null) {
            throw new IllegalArgumentException("内容 ID 不能为空");
        }
        if (interaction.getActionType()==null) {
            throw new IllegalArgumentException("互动类型不能为空");
        }
        if (interaction.getTableName()==null) {
            throw new IllegalArgumentException("表名不能为空");
        }
    }
    
    /**
     * 发送点赞通知
     */
    private void sendLikeNotification(Interaction interaction) {
        try {
            // 只处理点赞操作
            if (interaction.getActionType() != InteractionActionType.LIKE) {
                return;
            }
            
            Long targetUserId = null;
            UserSimpleVO likerVO = null;
            
            // 获取点赞者信息
            User liker = userDao.selectById(interaction.getUserId());
            if (liker == null) {
                log.warn("点赞用户不存在，userId: {}", interaction.getUserId());
                return;
            }
            
            likerVO = new UserSimpleVO();
            likerVO.setId(liker.getId());
            likerVO.setNickname(liker.getNickname());
            likerVO.setAvatar(liker.getAvatar());
            likerVO.setLastOnlineTime(liker.getLastOnlineTime());
            
            // 根据内容类型确定通知对象
            if (interaction.getTableName() == com.gcs.enums.ContentType.ARTICLE) {
                // 点赞文章
                Article article = articleDao.selectById(interaction.getContentId());
                if (article == null || article.getAuthorId().equals(interaction.getUserId())) {
                    return;
                }
                
                targetUserId = article.getAuthorId();
                
                Map<String, Object> extra = NotificationBuilder.buildLikeNotification(
                    "article",
                    interaction.getContentId(),
                    null,
                    likerVO
                );
                
                notificationService.createNotification(
                    targetUserId,
                    NotificationType.LIKE.getCode(),
                    interaction.getId(),
                    null,
                    extra
                );
                
                log.info("发送文章点赞通知，articleId: {}, authorId: {}", interaction.getContentId(), targetUserId);
                
            } else if (interaction.getTableName() == com.gcs.enums.ContentType.COMMENT) {
                // 点赞评论
                Comment comment = commentDao.selectById(interaction.getContentId());
                if (comment == null || comment.getUserId().equals(interaction.getUserId())) {
                    return; // 评论不存在或点赞自己，不发送通知
                }
                
                targetUserId = comment.getUserId();
                
                // 需要找到评论所属的文章 ID
                Article article = articleDao.selectById(comment.getContentId());
                Long articleId = article != null ? article.getId() : null;
                
                Map<String, Object> extra = NotificationBuilder.buildLikeNotification(
                    "comment",
                    articleId,
                    interaction.getContentId(),
                    likerVO
                );
                
                notificationService.createNotification(
                    targetUserId,
                    NotificationType.LIKE.getCode(),
                    interaction.getId(),
                    null,
                    extra
                );
                
                log.info("发送评论点赞通知，commentId: {}, authorId: {}", interaction.getContentId(), targetUserId);
            }
            
        } catch (Exception e) {
            log.error("发送点赞通知失败，interactionId: {}", interaction.getId(), e);
            // 不抛出异常，避免影响互动记录创建
        }
    }
}
