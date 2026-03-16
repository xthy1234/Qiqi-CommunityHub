package com.gcs.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.gcs.entity.Comment;
import com.gcs.entity.view.CommentView;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 评论数据访问接口
 * 提供评论相关的数据库操作
 * @author 
 * @email 
 * @date 2026-04-16 11:22:10
 */
public interface CommentDao extends BaseMapper<Comment> {
	
    /**
     * 查询评论列表视图
     * @param wrapper 查询条件包装器
     * @return 评论视图列表
     */
    List<CommentView> selectListView(@Param("ew") Wrapper<Comment> wrapper);

    /**
     * 分页查询评论列表视图
     * @param page 分页对象
     * @param wrapper 查询条件包装器
     * @return 评论视图列表
     */
    List<CommentView> selectListView(IPage<CommentView> page, @Param("ew") Wrapper<Comment> wrapper);
	
    /**
     * 查询单个评论视图
     * @param wrapper 查询条件包装器
     * @return 评论视图
     */
    CommentView selectView(@Param("ew") Wrapper<Comment> wrapper);

    /**
     * 根据内容 ID 查询评论列表
     * @param contentId 内容 ID
     * @return 评论列表
     */
    List<CommentView> selectByContentId(@Param("contentId") Long contentId);

    /**
     * 分页查询内容评论列表（仅一级评论）
     * @param page 分页对象
     * @param contentId 内容 ID
     * @return 评论列表
     */
    List<CommentView> selectByContentIdPage(IPage<CommentView> page, @Param("contentId") Long contentId);

    /**
     * 分页查询评论树结构（仅一级评论）
     * @param page 分页对象
     * @param contentId 内容 ID
     * @return 评论树
     */
    List<CommentView> selectPrimaryComments(IPage<CommentView> page, @Param("contentId") Long contentId);

    /**
     * 查询指定父评论的子评论
     * @param parentId 父评论 ID
     * @return 子评论列表
     */
    List<CommentView> selectChildComments(@Param("parentId") Long parentId);

    /**
     * 查询评论树结构
     * @param contentId 内容 ID
     * @return 评论树
     */
    List<CommentView> selectCommentTree(@Param("contentId") Long contentId);

    /**
     * 统计内容的评论数量
     * @param contentId 内容 ID
     * @return 评论数量
     */
    Integer countByContentId(@Param("contentId") Long contentId);
}
