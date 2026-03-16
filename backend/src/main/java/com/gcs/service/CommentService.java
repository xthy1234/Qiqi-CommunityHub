package com.gcs.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.extension.service.IService;
import com.gcs.utils.PageUtils;
import com.gcs.entity.Comment;
import com.gcs.entity.view.CommentView;

import java.util.List;
import java.util.Map;

/**
 * 评论服务接口
 * 提供评论相关的业务操作
 * @author 
 * @date 2026-04-16
 */
public interface CommentService extends IService<Comment> {

    /**
     * 分页查询评论列表
     *
     * @param params 查询参数
     * @return 分页结果
     */
    PageUtils queryPage(Map<String, Object> params);

    /**
     * 查询评论列表视图
     *
     * @param queryWrapper 查询条件包装器
     * @return 评论视图列表
     */
    List<CommentView> selectListView(Wrapper<Comment> queryWrapper);

    /**
     * 查询单个评论视图
     *
     * @param queryWrapper 查询条件包装器
     * @return 评论视图
     */
    CommentView selectView(Wrapper<Comment> queryWrapper);

    /**
     * 带条件的分页查询评论列表
     *
     * @param params 查询参数
     * @param queryWrapper 查询条件包装器
     * @return 分页结果
     */
    PageUtils queryPage(Map<String, Object> params, Wrapper<Comment> queryWrapper);

    /**
     * 创建评论
     *
     * @param comment 评论信息
     * @return 创建结果
     */
    boolean createComment(Comment comment);

    /**
     * 更新评论
     *
     * @param comment 评论信息
     * @return 更新结果
     */
    boolean updateComment(Comment comment);

    /**
     * 删除评论（支持批量删除）
     *
     * @param commentIds 评论ID列表
     * @return 删除结果
     */
    boolean deleteComments(List<Long> commentIds);

    /**
     * 根据内容ID查询评论列表
     *
     * @param contentId 内容ID
     * @return 评论列表
     */
    List<CommentView> getCommentsByContentId(Long contentId);

    /**
     * 分页获取评论内容列表
     *
     * @param contentId 内容 ID
     * @param params 查询参数（包含 page、limit 等）
     * @return 分页结果
     */
    PageUtils getCommentsByContentIdPage(Long contentId, Map<String, Object> params);

    /**
     * 获取评论树结构
     *
     * @param contentId 内容 ID
     * @return 评论树
     */
    List<CommentView> getCommentTree(Long contentId);

    /**
     * 分页获取评论树结构
     *
     * @param contentId 内容 ID
     * @param params 查询参数（包含 page、limit 等）
     * @return 分页的评论树
     */
    PageUtils getCommentTreePage(Long contentId, Map<String, Object> params);

    /**
     * 统计内容的评论数量
     *
     * @param contentId 内容 ID
     * @return 评论数量
     */
    Integer countCommentsByContentId(Long contentId);

    /**
     * 启用/禁用评论
     *
     * @param commentId 评论ID
     * @param status 状态（0:禁用 1:启用）
     * @return 操作结果
     */
    boolean updateStatus(Long commentId, Integer status);
}

