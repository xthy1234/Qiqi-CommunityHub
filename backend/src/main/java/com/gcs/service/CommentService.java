package com.gcs.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;
import com.gcs.entity.view.CommentView;
import com.gcs.utils.PageUtils;
import com.gcs.entity.Comment;
import java.util.List;
import java.util.Map;

/**
 * 评论服务接口
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

    /**
     * 管理员查询评论列表（包含所有状态）
     */
    IPage<Comment> adminQueryPage(Map<String, Object> params, QueryWrapper<Comment> queryWrapper);
    
    /**
     * 批量更新评论状态
     */
    boolean batchUpdateStatus(Long[] commentIds, Integer status);
    
    /**
     * 获取文章评论列表（分页，主评论 + 前3条高赞子评论）
     * 
     * @param articleId 文章ID
     * @param page 页码
     * @param size 每页数量
     * @return 分页结果（包含主评论及其热门回复）
     */
    PageUtils getArticleComments(Long articleId, Integer page, Integer size);
    
    /**
     * 获取主评论下的所有子评论（分页，按时间升序）
     * 
     * @param parentId 主评论ID
     * @param page 页码
     * @param size 每页数量
     * @return 分页的子评论列表
     */
    PageUtils getRepliesByParentId(Long parentId, Integer page, Integer size);
    
    /**
     * 统计主评论的子评论数量
     * 
     * @param parentId 主评论ID
     * @return 子评论数量
     */
    Integer countRepliesByParentId(Long parentId);
    
    /**
     * 点赞/取消点赞评论
     * 
     * @param commentId 评论ID
     * @param userId 用户ID
     * @return 最新点赞数
     */
    Integer toggleLike(Long commentId, Long userId);
    
    /**
     * 点踩/取消点踩评论
     * 
     * @param commentId 评论ID
     * @param userId 用户ID
     * @return 最新点踩数
     */
    Integer toggleDislike(Long commentId, Long userId);
    
    /**
     * 批量填充被回复用户信息（用于显示"@xxx"）
     * 
     * @param comments 评论列表
     */
    void enrichReplyUserInfo(List<CommentView> comments);

}

