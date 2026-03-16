package com.gcs.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.gcs.dao.CommentDao;
import com.gcs.entity.Article;
import com.gcs.entity.Comment;
import com.gcs.entity.view.CommentView;
import com.gcs.service.ArticleService;
import com.gcs.service.CommentService;
import com.gcs.utils.PageUtils;
import com.gcs.utils.Query;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.gcs.enums.CommentStatus;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.util.StringUtils;

import lombok.extern.slf4j.Slf4j;

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
        
        // 设置默认值
        comment.setCreateTime(LocalDateTime.now());
        comment.setStatus(CommentStatus.SHOW); // 默认显示
        if (comment.getLikeCount() == null) {
            comment.setLikeCount(0);
        }
        // 如果 parentId 为 0，改为 null（表示一级评论）
        if (comment.getParentId() != null && comment.getParentId() == 0) {
            comment.setParentId(null);
        }
        
        boolean result = this.save(comment);
        
        // 如果是保存成功且是一级评论（parentId 为 null），则更新文章的评论数
        if (result && comment.getParentId() == null) {
            updateArticleCommentCount(comment.getContentId(), 1);
        }
        
        return result;
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
     * 删除评论（支持批量删除）
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
        
        // 统计要删除的一级评论数量（用于更新文章评论数）
        long primaryCommentCount = commentIds.stream()
            .filter(id -> {
                Comment comment = this.getById(id);
                return comment != null && comment.getParentId() == null;
            })
            .count();
        
        boolean result = this.removeByIds(commentIds);
        
        // 如果删除成功且有一级评论，则更新文章的评论数（减少）
        if (result && primaryCommentCount > 0) {
            // 获取这些评论的 contentId（假设都是同一篇文章）
            Comment firstComment = this.getById(commentIds.get(0));
            if (firstComment != null) {
                updateArticleCommentCount(firstComment.getContentId(), -(int)primaryCommentCount);
            }
        }
        
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
        
        // 只查询一级评论
        List<CommentView> rootComments = baseMapper.selectPrimaryComments(null, contentId);
        
        // 为每个一级评论加载子评论
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
            Article article = articleService.getById(contentId);
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
}
