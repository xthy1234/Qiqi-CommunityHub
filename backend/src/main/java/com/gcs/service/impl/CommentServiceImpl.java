package com.gcs.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.gcs.dao.ArticleDao;
import com.gcs.dao.CommentDao;
import com.gcs.dao.UserDao;
import com.gcs.entity.Article;
import com.gcs.entity.Comment;
import com.gcs.entity.Interaction;
import com.gcs.entity.User;
import com.gcs.entity.view.CommentView;
import com.gcs.enums.CommentStatus;
import com.gcs.enums.ContentType;
import com.gcs.enums.InteractionActionType;
import com.gcs.enums.NotificationType;
import com.gcs.service.*;
import com.gcs.utils.NotificationBuilder;
import com.gcs.utils.PageUtils;
import com.gcs.utils.Query;
import com.gcs.vo.ArticleCommentVO;
import com.gcs.vo.UserSimpleVO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 评论服务实现类
 * 提供评论相关的业务逻辑处理
 * @author 
 * @date 2026-04-16
 */
@Slf4j
@Service("commentService")
public class CommentServiceImpl extends ServiceImpl<CommentDao, Comment> implements CommentService {

    @Autowired
    private ArticleService articleService;

    @Autowired
    private UserService userService;

    @Autowired
    private NotificationService notificationService;
    
    @Autowired
    private PointsService pointsService;
    
    @Autowired
    private ArticleDao articleDao;
    
    @Autowired
    private UserDao userDao;

    @Autowired
    private InteractionService interactionService;
    /**
     * 分页查询评论列表
     *
     * @param params 查询参数
     * @return 分页结果
     */
    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        validateParams(params);
        
        IPage<Comment> commentPage = new Query<Comment>(params).getPage();
        IPage<Comment> resultPage = this.page(commentPage, new QueryWrapper<>());
        
        return new PageUtils(resultPage);
    }

    /**
     * 带条件的分页查询评论列表
     *
     * @param params 查询参数
     * @param queryWrapper 查询条件包装器
     * @return 分页结果
     */
    @Override
    public PageUtils queryPage(Map<String, Object> params, Wrapper<Comment> queryWrapper) {
        validateQueryParams(params, queryWrapper);
        
        IPage<CommentView> commentViewPage = new Query<CommentView>(params).getPage();
        List<CommentView> commentViews = baseMapper.selectListView(commentViewPage, queryWrapper);
        commentViewPage.setRecords(commentViews);
        
        return new PageUtils(commentViewPage);
    }

    /**
     * 查询评论列表视图
     *
     * @param queryWrapper 查询条件包装器
     * @return 评论视图列表
     */
    @Override
    public List<CommentView> selectListView(Wrapper<Comment> queryWrapper) {
        validateWrapper(queryWrapper);
        return baseMapper.selectListView(queryWrapper);
    }

    /**
     * 查询单个评论视图
     *
     * @param queryWrapper 查询条件包装器
     * @return 评论视图
     */
    @Override
    public CommentView selectView(Wrapper<Comment> queryWrapper) {
        validateWrapper(queryWrapper);
        return baseMapper.selectView(queryWrapper);
    }

    /**
     * 创建评论
     *
     * @param comment 评论信息
     * @return 创建结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean createComment(Comment comment) {
        validateCommentForCreate(comment);
        

        comment.setCreateTime(LocalDateTime.now());
        comment.setStatus(CommentStatus.SHOW);
        if (comment.getLikeCount() == null) {
            comment.setLikeCount(0);
        }

        if (comment.getParentId() != null && comment.getParentId() == 0) {
            comment.setParentId(null);
        }
        
        boolean result = this.save(comment);
        
        // 所有评论（包括主评论和子评论）都会计入文章评论数
        if (result) {
            updateArticleCommentCount(comment.getContentId(), 1);
            
            // 仅主评论发送通知给文章作者
            if (comment.getParentId() == null) {
                sendCommentNotification(comment);
                
                // 添加积分
                pointsService.addPoints(comment.getUserId(), "post_comment", comment.getId(), "发表评论");

                Article article = articleDao.selectById(comment.getContentId());
                if (article != null && !article.getAuthorId().equals(comment.getUserId())) {
                    pointsService.addPoints(article.getAuthorId(), "comment_received", comment.getId(), "文章被评论");
                }
            } else if (comment.getReplyId() != null) {
                // 如果是回复某用户，发送通知给被回复用户
                sendReplyNotification(comment);
            }
        }
        
        return result;
    }
    
    /**
     * 发送回复通知给被回复用户
     */
    private void sendReplyNotification(Comment comment) {
        try {
            if (comment.getReplyId() == null || comment.getReplyId().equals(comment.getUserId())) {
                return; // 不通知自己
            }
            
            // 查询被回复用户是否存在
            User replyToUser = userDao.selectById(comment.getReplyId());
            if (replyToUser == null) {
                log.warn("被回复用户不存在，replyId: {}", comment.getReplyId());
                return;
            }
            
            // 构建评论者信息
            User commenter = userDao.selectById(comment.getUserId());
            if (commenter == null) {
                log.warn("评论用户不存在，userId: {}", comment.getUserId());
                return;
            }
            
            UserSimpleVO commenterVO = new UserSimpleVO();
            commenterVO.setId(commenter.getId());
            commenterVO.setNickname(commenter.getNickname());
            commenterVO.setAvatar(commenter.getAvatar());
            commenterVO.setLastOnlineTime(commenter.getLastOnlineTime());
            
            // 构建 extra 数据
            Map<String, Object> extra = NotificationBuilder.buildCommentNotification(
                comment.getContentId(),
                comment.getId(),
                commenterVO,
                comment.getContent()
            );
            
            // 创建通知
            notificationService.createNotification(
                comment.getReplyId(),
                NotificationType.COMMENT.getCode(),
                comment.getId(),
                null,
                extra
            );
            
            log.info("发送回复通知成功，commentId: {}, replyToUserId: {}", comment.getId(), comment.getReplyId());
            
        } catch (Exception e) {
            log.error("发送回复通知失败，commentId: {}", comment.getId(), e);
        }
    }

    /**
     * 更新评论
     *
     * @param comment 评论信息
     * @return 更新结果
     */
    @Override
    public boolean updateComment(Comment comment) {
        validateCommentForUpdate(comment);
        
        comment.setUpdateTime(LocalDateTime.now());
        return this.updateById(comment);
    }

    /**
     * 删除评论（支持批量删除，级联软删除子评论）
     *
     * @param commentIds 评论 ID 列表
     * @return 删除结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteComments(List<Long> commentIds) {
        if (CollectionUtils.isEmpty(commentIds)) {
            throw new IllegalArgumentException("评论 ID 列表不能为空");
        }
        
        // 获取第一条评论以确定文章 ID
        Comment firstComment = this.getById(commentIds.get(0));
        if (firstComment == null) {
            throw new IllegalArgumentException("评论不存在");
        }
        
        Long contentId = firstComment.getContentId();
        
        // 收集所有需要删除的评论ID（包括子评论）
        List<Long> allDeleteIds = new java.util.ArrayList<>(commentIds);
        
        // 查找并添加所有子评论ID
        for (Long commentId : commentIds) {
            QueryWrapper<Comment> childQuery = new QueryWrapper<>();
            childQuery.eq("parent_id", commentId)
                     .eq("is_deleted", false);
            List<Comment> children = this.list(childQuery);
            if (!CollectionUtils.isEmpty(children)) {
                allDeleteIds.addAll(children.stream().map(Comment::getId).collect(Collectors.toList()));
            }
        }
        
        // 批量软删除（MyBatis-Plus 的 @TableLogic 会自动处理）
        boolean result = this.removeByIds(allDeleteIds);
        
        // 更新文章评论数（所有评论都计数，包括子评论）
        if (result && !allDeleteIds.isEmpty()) {
            updateArticleCommentCount(contentId, -allDeleteIds.size());
        }
        
        log.info("删除评论成功，删除 {} 条评论（含子评论），文章ID: {}", allDeleteIds.size(), contentId);
        
        return result;
    }

    /**
     * 根据内容 ID 查询评论列表
     *
     * @param contentId 内容 ID
     * @return 评论列表
     */
    @Override
    public List<CommentView> getCommentsByContentId(Long contentId) {
        if (contentId == null) {
            throw new IllegalArgumentException("内容ID不能为空");
        }
        
        return baseMapper.selectByContentId(contentId);
    }

    /**
     * 分页获取评论内容列表
     *
     * @param contentId 内容 ID
     * @param params 查询参数（包含 page、limit 等）
     * @return 分页结果
     */
    @Override
    public PageUtils getCommentsByContentIdPage(Long contentId, Map<String, Object> params) {
        if (contentId == null) {
            throw new IllegalArgumentException("内容 ID 不能为空");
        }
        
        int page = params.get("page") != null ? Integer.parseInt(params.get("page").toString()) : 1;
        int limit = params.get("limit") != null ? Integer.parseInt(params.get("limit").toString()) : 20;
        
        IPage<CommentView> commentPage = new Query<CommentView>(params).getPage();
        List<CommentView> comments = baseMapper.selectByContentIdPage(commentPage, contentId);
        
        return new PageUtils(commentPage);
    }

    /**
     * 获取评论树结构
     *
     * @param contentId 内容 ID
     * @return 评论树
     */
    @Override
    public List<CommentView> getCommentTree(Long contentId) {
        if (contentId == null) {
            throw new IllegalArgumentException("内容 ID 不能为空");
        }
        

        List<CommentView> rootComments = baseMapper.selectPrimaryComments(null, contentId);
        

        for (CommentView rootComment : rootComments) {
            List<CommentView> children = baseMapper.selectChildComments(rootComment.getId());
            rootComment.setChildren(children);
        }
        
        return rootComments;
    }

    /**
     * 分页获取评论树结构
     *
     * @param contentId 内容 ID
     * @param params 查询参数（包含 page、limit 等）
     * @return 分页的评论树
     */
    @Override
    public PageUtils getCommentTreePage(Long contentId, Map<String, Object> params) {
        if (contentId == null) {
            throw new IllegalArgumentException("内容 ID 不能为空");
        }
        
        int page = params.get("page") != null ? Integer.parseInt(params.get("page").toString()) : 1;
        int limit = params.get("limit") != null ? Integer.parseInt(params.get("limit").toString()) : 20;
        
        // 先查询一级评论的分页列表
        IPage<CommentView> commentPage = new Query<CommentView>(params).getPage();
        List<CommentView> rootComments = baseMapper.selectPrimaryComments(commentPage, contentId);
        
        // 为每个一级评论加载子评论
        for (CommentView rootComment : rootComments) {
            List<CommentView> children = baseMapper.selectChildComments(rootComment.getId());
            rootComment.setChildren(children);
        }
        
        commentPage.setRecords(rootComments);
        return new PageUtils(commentPage);
    }

    /**
     * 统计内容的评论数量
     *
     * @param contentId 内容 ID
     * @return 评论数量
     */
    @Override
    public Integer countCommentsByContentId(Long contentId) {
        if (contentId == null) {
            return 0;
        }
        
        return baseMapper.countByContentId(contentId);
    }

    /**
     * 启用/禁用评论
     *
     * @param commentId 评论 ID
     * @param status 状态（0:禁用 1:启用）
     * @return 操作结果
     */
    @Override
    public boolean updateStatus(Long commentId, Integer status) {
        if (commentId == null || status == null) {
            throw new IllegalArgumentException("参数不能为空");
        }

        Comment comment = this.getById(commentId);
        if (comment == null) {
            throw new RuntimeException("评论不存在");
        }
        CommentStatus commentStatus = CommentStatus.valueOf(status);
        comment.setStatus(commentStatus);
        comment.setUpdateTime(LocalDateTime.now());
        
        return this.updateById(comment);
    }
    
    @Override
    public IPage<Comment> adminQueryPage(Map<String, Object> params, QueryWrapper<Comment> queryWrapper) {
        int page = params.get("page") != null ? Integer.parseInt(params.get("page").toString()) : 1;
        int limit = params.get("limit") != null ? Integer.parseInt(params.get("limit").toString()) : 10;
        
        IPage<Comment> commentPage = new Query<Comment>(params).getPage();
        IPage<Comment> resultPage = this.page(commentPage, queryWrapper);
        
        log.info("管理员查询评论列表，总记录数：{}, 页码：{}, 每页数量：{}", resultPage.getTotal(), page, limit);
        
        return resultPage;
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean batchUpdateStatus(Long[] commentIds, Integer status) {
        if (commentIds == null || commentIds.length == 0) {
            throw new IllegalArgumentException("评论 ID 列表不能为空");
        }
        
        if (status == null) {
            throw new IllegalArgumentException("状态不能为空");
        }
        
        try {
            CommentStatus commentStatus = CommentStatus.valueOf(status);
            
            for (Long id : commentIds) {
                Comment comment = this.getById(id);
                if (comment != null) {
                    comment.setStatus(commentStatus);
                    comment.setUpdateTime(LocalDateTime.now());
                    this.updateById(comment);
                }
            }
            
            log.info("批量更新评论状态成功，数量：{}, 新状态：{}", commentIds.length, status);
            return true;
        } catch (Exception e) {
            log.error("批量更新评论状态失败", e);
            return false;
        }
    }
    
    /**
     * 为评论列表补充用户信息
     * @deprecated 用户信息已通过联表查询获取，不再需要此方法
     */
    @Deprecated
    public void enrichCommentsWithUserInfo(List<Comment> comments) {
        // 已废弃：用户头像和昵称不再存储在 comment 表中
        // 前端应通过 userId 关联 users 表实时查询，或使用 CommentView（已包含联表数据）
    }

    // ==================== 私有验证方法 ====================

    /**
     * 验证查询参数
     */
    private void validateParams(Map<String, Object> params) {
        // 使用Map的isEmpty()方法或者判断是否为null
        if (params == null || params.isEmpty()) {
            throw new IllegalArgumentException("查询参数不能为空");
        }
    }

    /**
     * 验证查询条件包装器
     */
    private void validateWrapper(Wrapper<Comment> queryWrapper) {
        if (Objects.isNull(queryWrapper)) {
            throw new IllegalArgumentException("查询条件不能为空");
        }
    }

    /**
     * 验证查询参数和条件
     */
    private void validateQueryParams(Map<String, Object> params, Wrapper<Comment> queryWrapper) {
        validateParams(params);
        validateWrapper(queryWrapper);
    }

    /**
     * 验证创建评论参数
     */
    private void validateCommentForCreate(Comment comment) {
        if (comment == null) {
            throw new IllegalArgumentException("评论信息不能为空");
        }
        if (comment.getContentId() == null) {
            throw new IllegalArgumentException("关联内容ID不能为空");
        }
        if (comment.getUserId() == null) {
            throw new IllegalArgumentException("用户ID不能为空");
        }
        if (!StringUtils.hasText(comment.getContent())) {
            throw new IllegalArgumentException("评论内容不能为空");
        }
    }

    /**
     * 验证更新评论参数
     */
    private void validateCommentForUpdate(Comment comment) {
        if (comment == null || comment.getId() == null) {
            throw new IllegalArgumentException("评论信息不完整");
        }
    }

    /**
     * 更新文章的评论数量
     *
     * @param contentId 文章 ID
     * @param delta 变化数量（正数增加，负数减少）
     */
    private void updateArticleCommentCount(Long contentId, int delta) {
        if (contentId == null || delta == 0) {
            return;
        }
        
        try {
            Article article = articleDao.selectById(contentId);
            if (article != null) {
                Integer currentCount = article.getCommentCount();
                if (currentCount == null) {
                    currentCount = 0;
                }
                article.setCommentCount(currentCount + delta);
                articleService.updateById(article);
                log.info("更新文章评论数，articleId: {}, 当前数量：{}", contentId, article.getCommentCount());
            }
        } catch (Exception e) {
            log.error("更新文章评论数失败，contentId: {}", contentId, e);
            // 不抛出异常，避免影响评论创建
        }
    }
    
    /**
     * 发送评论通知给文章作者
     */
    private void sendCommentNotification(Comment comment) {
        try {
            // 查询文章信息
            Article article = articleDao.selectById(comment.getContentId());
            if (article == null) {
                log.warn("文章不存在，无法发送通知，articleId: {}", comment.getContentId());
                return;
            }
            
            // 如果评论者就是文章作者，不需要通知自己
            if (article.getAuthorId().equals(comment.getUserId())) {
                return;
            }
            
            // 构建评论者信息
            User commenter = userDao.selectById(comment.getUserId());
            if (commenter == null) {
                log.warn("评论用户不存在，userId: {}", comment.getUserId());
                return;
            }
            
            UserSimpleVO commenterVO = new UserSimpleVO();
            commenterVO.setId(commenter.getId());
            commenterVO.setNickname(commenter.getNickname());
            commenterVO.setAvatar(commenter.getAvatar());
            commenterVO.setLastOnlineTime(commenter.getLastOnlineTime());
            
            // 构建 extra 数据
            Map<String, Object> extra = NotificationBuilder.buildCommentNotification(
                comment.getContentId(),
                comment.getId(),
                commenterVO,
                comment.getContent()
            );
            
            // 创建通知
            notificationService.createNotification(
                article.getAuthorId(),
                NotificationType.COMMENT.getCode(),
                comment.getId(),
                null,
                extra
            );
            
            log.info("发送评论通知成功，articleId: {}, authorId: {}", comment.getContentId(), article.getAuthorId());
            
        } catch (Exception e) {
            log.error("发送评论通知失败，commentId: {}", comment.getId(), e);
        }
    }
    
    /**
     * 获取文章评论列表（分页，主评论 + 前3条高赞子评论）
     */
    @Override
    public PageUtils getArticleComments(Long articleId, Integer page, Integer size) {
        if (articleId == null) {
            throw new IllegalArgumentException("文章ID不能为空");
        }
        if (page == null || page < 1) {
            page = 1;
        }
        if (size == null || size < 1) {
            size = 10;
        }
        
        // 查询主评论（按点赞数降序，时间降序）
        QueryWrapper<Comment> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("content_id", articleId)
                   .isNull("parent_id")
                   .eq("status", CommentStatus.SHOW)
                   .eq("is_deleted", false)
                   .orderByDesc("like_count")
                   .orderByDesc("create_time");
        
        IPage<Comment> commentPage = new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(page, size);
        IPage<Comment> resultPage = this.page(commentPage, queryWrapper);
        
        List<Comment> primaryComments = resultPage.getRecords();
        if (CollectionUtils.isEmpty(primaryComments)) {
            return new PageUtils(Collections.emptyList(), 0, size, page);
        }
        
        // 批量查询每条主评论的前3条高赞子评论
        List<Long> primaryIds = primaryComments.stream().map(Comment::getId).collect(Collectors.toList());
        Map<Long, List<Comment>> repliesMap = batchGetTopReplies(primaryIds, 3);
        
        // 收集所有用户 ID（评论者 + 被回复者）
        Set<Long> allUserIds = primaryComments.stream().map(Comment::getUserId).collect(Collectors.toSet());
        repliesMap.values().forEach(replies -> 
            replies.forEach(reply -> {
                allUserIds.add(reply.getUserId());
                if (reply.getReplyId() != null) {
                    allUserIds.add(reply.getReplyId());
                }
            })
        );
        
        // 批量查询用户信息
        Map<Long, User> userMap = batchGetUsers(allUserIds);
        
        // 转换为 ArticleCommentVO
        List<ArticleCommentVO> voList = primaryComments.stream().map(comment -> 
            convertToArticleCommentVO(comment, userMap, repliesMap)
        ).collect(Collectors.toList());
        
        // 统计每条主评论的子评论总数（包括所有子评论，不仅仅是前3条）
        Map<Long, Integer> replyCountMap = batchCountReplies(primaryIds);
        voList.forEach(vo -> {
            vo.setReplyCount(replyCountMap.getOrDefault(vo.getId(), 0));
        });
        
        return new PageUtils(voList, resultPage.getTotal(), size, page);
    }
    
    /**
     * 将 Comment 转换为 ArticleCommentVO
     */
    private ArticleCommentVO convertToArticleCommentVO(Comment comment, Map<Long, User> userMap, 
                                                        Map<Long, List<Comment>> repliesMap) {
        ArticleCommentVO vo = new ArticleCommentVO();
        vo.setId(comment.getId());
        vo.setContent(comment.getContent());
        vo.setReplyId(comment.getReplyId());
        vo.setLikeCount(comment.getLikeCount() != null ? comment.getLikeCount() : 0);
        vo.setDislikeCount(comment.getDislikeCount() != null ? comment.getDislikeCount() : 0);
        vo.setCreateTime(comment.getCreateTime());
        vo.setUpdateTime(comment.getUpdateTime());
        vo.setIsEdited(comment.getUpdateTime() != null && !comment.getUpdateTime().equals(comment.getCreateTime()));
        
        // 设置评论者信息
        User commenter = userMap.get(comment.getUserId());
        if (commenter != null) {
            UserSimpleVO userVO = new UserSimpleVO();
            userVO.setId(commenter.getId());
            userVO.setNickname(commenter.getNickname());
            userVO.setAvatar(commenter.getAvatar());
            userVO.setLastOnlineTime(commenter.getLastOnlineTime());
            vo.setUser(userVO);
        }
        
        // 设置被回复用户信息
        if (comment.getReplyId() != null) {
            User replyToUser = userMap.get(comment.getReplyId());
            if (replyToUser != null) {
                UserSimpleVO replyToUserVO = new UserSimpleVO();
                replyToUserVO.setId(replyToUser.getId());
                replyToUserVO.setNickname(replyToUser.getNickname());
                vo.setReplyToUser(replyToUserVO);
            }
        }
        
        // 转换子评论（前3条高赞）
        List<Comment> replies = repliesMap.getOrDefault(comment.getId(), java.util.Collections.emptyList());
        if (!replies.isEmpty()) {
            List<ArticleCommentVO> replyVOs = replies.stream()
                .map(reply -> convertToArticleCommentVO(reply, userMap, java.util.Collections.emptyMap()))
                .collect(Collectors.toList());
            vo.setTopReplies(replyVOs);
        }
        
        return vo;
    }
    
    /**
     * 批量查询用户信息
     */
    private Map<Long, User> batchGetUsers(Set<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return java.util.Collections.emptyMap();
        }
        
        List<User> users = userDao.selectBatchIds(userIds);
        return users.stream().collect(Collectors.toMap(User::getId, user -> user));
    }
    
    /**
     * 获取主评论下的所有子评论（分页，按时间升序）
     */
    @Override
    public PageUtils getRepliesByParentId(Long parentId, Integer page, Integer size) {
        if (parentId == null) {
            throw new IllegalArgumentException("父评论ID不能为空");
        }
        if (page == null || page < 1) {
            page = 1;
        }
        if (size == null || size < 1) {
            size = 10;
        }
        
        QueryWrapper<Comment> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("parent_id", parentId)
                   .eq("status", CommentStatus.SHOW)
                   .eq("is_deleted", false)
                   .orderByAsc("create_time");
        
        IPage<Comment> commentPage = new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(page, size);
        IPage<Comment> resultPage = this.page(commentPage, queryWrapper);
        
        List<Comment> replies = resultPage.getRecords();
        if (CollectionUtils.isEmpty(replies)) {
            return new PageUtils(Collections.emptyList(), 0, size, page);
        }
        
        // 收集所有用户 ID
        Set<Long> userIds = replies.stream()
            .map(Comment::getUserId)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
        
        replies.forEach(reply -> {
            if (reply.getReplyId() != null) {
                userIds.add(reply.getReplyId());
            }
        });
        
        // 批量查询用户信息
        Map<Long, User> userMap = batchGetUsers(userIds);
        
        // 转换为 ArticleCommentVO
        List<ArticleCommentVO> voList = replies.stream()
            .map(reply -> convertToArticleCommentVO(reply, userMap, java.util.Collections.emptyMap()))
            .collect(Collectors.toList());
        
        return new PageUtils(voList, resultPage.getTotal(), size, page);
    }

    /**
     * 统计主评论的子评论数量
     */
    @Override
    public Integer countRepliesByParentId(Long parentId) {
        if (parentId == null) {
            return 0;
        }
        
        QueryWrapper<Comment> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("parent_id", parentId)
                   .eq("status", CommentStatus.SHOW)
                   .eq("is_deleted", false);
        
        return Math.toIntExact(this.count(queryWrapper));
    }
    
    /**
     * 点赞/取消点赞评论
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Integer toggleLike(Long commentId, Long userId) {
        if (commentId == null || userId == null) {
            throw new IllegalArgumentException("参数不能为空");
        }
        
        Comment comment = this.getById(commentId);
        if (comment == null) {
            throw new IllegalArgumentException("评论不存在");
        }
        
        // 检查用户是否有有效的点赞记录
        boolean hasLiked = interactionService.hasValidInteraction(
            userId, 
            commentId, 
            InteractionActionType.LIKE,
            ContentType.COMMENT
        );
        
        Integer currentLikeCount = comment.getLikeCount();
        if (currentLikeCount == null) {
            currentLikeCount = 0;
        }
        
        if (hasLiked) {
            // 已点赞，取消点赞：将互动记录状态改为无效，点赞数 -1
            boolean removed = interactionService.removeInteraction(
                userId, 
                commentId, 
                InteractionActionType.LIKE, 
                ContentType.COMMENT
            );
            
            if (removed) {
                currentLikeCount = Math.max(0, currentLikeCount - 1);
                comment.setLikeCount(currentLikeCount);
                this.updateById(comment);
                log.info("取消点赞成功，commentId: {}, userId: {}, 新点赞数: {}", commentId, userId, currentLikeCount);
            }
        } else {
            // 未点赞或之前取消了，添加/重新激活点赞：插入或激活互动记录，点赞数 +1
            Interaction interaction = new Interaction();
            interaction.setUserId(userId);
            interaction.setContentId(commentId);
            interaction.setActionType(InteractionActionType.LIKE);
            interaction.setTableName(ContentType.COMMENT);
            interaction.setCreateTime(LocalDateTime.now());
            
            boolean added = interactionService.addInteraction(interaction);
            
            if (added) {
                currentLikeCount = currentLikeCount + 1;
                comment.setLikeCount(currentLikeCount);
                this.updateById(comment);
                log.info("点赞成功，commentId: {}, userId: {}, 新点赞数: {}", commentId, userId, currentLikeCount);
            }
        }
        
        return comment.getLikeCount();
    }
    
    /**
     * 点踩/取消点踩评论
     */
    @Transactional(rollbackFor = Exception.class)
    public Integer toggleDislike(Long commentId, Long userId) {
        if (commentId == null || userId == null) {
            throw new IllegalArgumentException("参数不能为空");
        }
        
        Comment comment = this.getById(commentId);
        if (comment == null) {
            throw new IllegalArgumentException("评论不存在");
        }
        
        // 检查用户是否有有效的点踩记录
        boolean hasDisliked = interactionService.hasValidInteraction(
            userId, 
            commentId, 
            InteractionActionType.DISLIKE, 
            ContentType.COMMENT
        );
        
        Integer currentDislikeCount = comment.getDislikeCount();
        if (currentDislikeCount == null) {
            currentDislikeCount = 0;
        }
        
        if (hasDisliked) {
            // 已点踩，取消点踩：将互动记录状态改为无效，点踩数 -1
            boolean removed = interactionService.removeInteraction(
                userId, 
                commentId, 
                InteractionActionType.DISLIKE, 
                ContentType.COMMENT
            );
            
            if (removed) {
                currentDislikeCount = Math.max(0, currentDislikeCount - 1);
                comment.setDislikeCount(currentDislikeCount);
                this.updateById(comment);
                log.info("取消点踩成功，commentId: {}, userId: {}, 新点踩数: {}", commentId, userId, currentDislikeCount);
            }
        } else {
            // 未点踩或之前取消了，添加/重新激活点踩：插入或激活互动记录，点踩数 +1
            Interaction interaction = new Interaction();
            interaction.setUserId(userId);
            interaction.setContentId(commentId);
            interaction.setActionType(InteractionActionType.DISLIKE);
            interaction.setTableName(ContentType.COMMENT);
            interaction.setCreateTime(LocalDateTime.now());
            
            boolean added = interactionService.addInteraction(interaction);
            
            if (added) {
                currentDislikeCount = currentDislikeCount + 1;
                comment.setDislikeCount(currentDislikeCount);
                this.updateById(comment);
                log.info("点踩成功，commentId: {}, userId: {}, 新点踩数: {}", commentId, userId, currentDislikeCount);
            }
        }
        
        return comment.getDislikeCount();
    }
    
    /**
     * 批量填充被回复用户信息（用于显示"@xxx"）
     */
    @Override
    public void enrichReplyUserInfo(List<CommentView> comments) {
        if (CollectionUtils.isEmpty(comments)) {
            return;
        }
        
        // 收集所有 replyId
        Set<Long> replyUserIds = comments.stream()
            .map(CommentView::getReplyId)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
        
        if (replyUserIds.isEmpty()) {
            return;
        }
        
        // 批量查询被回复用户信息
        List<User> replyUsers = userDao.selectBatchIds(replyUserIds);
        Map<Long, User> replyUserMap = replyUsers.stream()
            .collect(Collectors.toMap(User::getId, user -> user));
        
        // 为每个评论设置被回复用户信息（可以在 CommentView 中新增字段存储）
        // 注意：当前 CommentView 没有 replyToUser 字段，如需显示"@xxx"，前端可通过 replyId 自行查询
        log.debug("批量填充被回复用户信息，共 {} 条评论，{} 个被回复用户", comments.size(), replyUserMap.size());
    }
    
    /**
     * 批量获取每条主评论的前N条高赞子评论
     */
    private Map<Long, List<Comment>> batchGetTopReplies(List<Long> primaryIds, int topN) {
        if (CollectionUtils.isEmpty(primaryIds)) {
            return java.util.Collections.emptyMap();
        }
        
        // 查询所有相关子评论（按点赞数降序）
        QueryWrapper<Comment> queryWrapper = new QueryWrapper<>();
        queryWrapper.in("parent_id", primaryIds)
                   .eq("status", CommentStatus.SHOW)
                   .eq("is_deleted", false)
                   .orderByDesc("like_count")
                   .orderByDesc("create_time");
        
        List<Comment> allReplies = this.list(queryWrapper);
        
        // 按 parent_id 分组，每组取前 topN 条
        return allReplies.stream()
            .map(CommentView::new)
            .collect(Collectors.groupingBy(
                CommentView::getParentId,
                Collectors.collectingAndThen(
                    Collectors.toList(),
                    list -> list.stream().limit(topN).collect(Collectors.toList())
                )
            ));
    }
    
    /**
     * 批量统计每条主评论的子评论数量
     */
    private Map<Long, Integer> batchCountReplies(List<Long> primaryIds) {
        if (CollectionUtils.isEmpty(primaryIds)) {
            return java.util.Collections.emptyMap();
        }
        
        QueryWrapper<Comment> queryWrapper = new QueryWrapper<>();
        queryWrapper.select("parent_id", "count(*) as count")
                   .in("parent_id", primaryIds)
                   .eq("status", CommentStatus.SHOW)
                   .eq("is_deleted", false)
                   .groupBy("parent_id");
        
        // 使用原生 SQL 查询
        List<Map<String, Object>> results = baseMapper.selectMaps(queryWrapper);
        
        return results.stream()
            .collect(Collectors.toMap(
                row -> ((Number) row.get("parent_id")).longValue(),
                row -> ((Number) row.get("count")).intValue()
            ));
    }
}
