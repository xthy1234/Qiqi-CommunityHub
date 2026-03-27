package com.gcs.controller;

import java.util.Arrays;
import java.util.Map;
import java.util.List;
import java.util.stream.Collectors;

import com.gcs.entity.Article;
import com.gcs.entity.Comment;
import com.gcs.entity.User;
import com.gcs.entity.view.CommentView;
import com.gcs.enums.ContentType;
import com.gcs.enums.InteractionActionType;
import com.gcs.service.ArticleService;
import com.gcs.service.CommentService;
import com.gcs.service.InteractionService;
import com.gcs.service.UserService;

import com.gcs.utils.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.gcs.annotation.IgnoreAuth;
import com.gcs.dto.CommentCreateDTO;
import com.gcs.dto.CommentUpdateDTO;
import com.gcs.dto.CommentReplyDTO;

import com.gcs.vo.CommentVO;
import com.gcs.vo.CommentDetailVO;
import com.gcs.vo.CommentTreeVO;
import com.gcs.vo.CommentWithUserVO;

import lombok.extern.slf4j.Slf4j;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

/**
 * 评论控制器
 * 提供评论相关的 RESTful API 接口
 * @author 
 * @email 
 * @date 2026-04-16 11:22:10
 */
@Slf4j
@Tag(name = "评论管理", description = "评论相关的 RESTful API 接口")
@RestController
@RequestMapping("/comments")
public class CommentController {
    
    @Autowired
    private CommentService commentService;

    @Autowired
    private UserService userService;

    @Autowired
    private ArticleService articleService;

    @Autowired
    private InteractionService interactionService;
    
    @Autowired
    private SessionUtils sessionUtils;
    
    @Autowired
    private AuthUtils authUtils;
    
    @Autowired
    private InteractionUtils interactionUtils;

    /**
     * 获取评论分页列表
     */
    @Operation(summary = "获取评论分页列表", description = "分页查询评论列表，支持条件筛选")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "获取成功"),
        @ApiResponse(responseCode = "500", description = "获取失败")
    })
    @GetMapping
    @IgnoreAuth
    public R getPage(
        @Parameter(description = "查询参数") @RequestParam Map<String, Object> params, 
        @Parameter(description = "评论查询条件") Comment comment) {
        try {
            QueryWrapper<Comment> queryWrapper = new QueryWrapper<>();
            PageUtils page = commentService.queryPage(params, 
                MPUtil.sort(MPUtil.between(MPUtil.likeOrEq(queryWrapper, comment), params), params));
            
            // 将 Comment 转换为 CommentVO
            List<CommentVO> voList = ((List<Comment>) page.getList())
                .stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
            page.setList(voList);
            
            return R.ok().put("data", page);
        } catch (Exception e) {
            log.error("获取评论分页列表失败", e);
            return R.error("获取数据失败");
        }
    }
    
    /**
     * 获取评论详情
     */
    @Operation(summary = "获取评论详情", description = "根据评论 ID 获取评论详情")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "获取成功"),
        @ApiResponse(responseCode = "404", description = "评论不存在"),
        @ApiResponse(responseCode = "500", description = "获取失败")
    })
    @GetMapping("/{id}")
    public R getCommentInfo(
        @Parameter(description = "评论 ID", required = true) @PathVariable("id") Long id) {
        try {
            Comment comment = commentService.getById(id);
            if (comment == null) {
                return R.error("评论不存在");
            }
            
            // 将 Comment 转换为 CommentDetailVO
            CommentDetailVO vo = convertToDetailVO(comment);
            return R.ok().put("data", vo);
        } catch (Exception e) {
            log.error("获取评论详情失败，ID: {}", id, e);
            return R.error("获取详情失败");
        }
    }

    /**
     * 根据内容 ID 获取评论列表
     */
    @Operation(summary = "根据内容 ID 获取评论列表", description = "根据内容 ID 获取该内容的全部评论列表")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "获取成功"),
        @ApiResponse(responseCode = "500", description = "获取失败")
    })
    @GetMapping("/content/{contentId}")
    @IgnoreAuth
    public R getCommentsByContentId(
        @Parameter(description = "内容 ID", required = true) @PathVariable("contentId") Long contentId) {
        try {
            List<CommentView> comments = commentService.getCommentsByContentId(contentId);
            
            // 将 CommentView 转换为 CommentDetailVO
            List<CommentDetailVO> voList = comments.stream()
                .map(this::convertViewToDetailVO)
                .collect(Collectors.toList());
            
            return R.ok().put("data", voList);
        } catch (Exception e) {
            log.error("获取内容评论失败，contentId: {}", contentId, e);
            return R.error("获取评论失败");
        }
    }

    /**
     * 根据内容 ID 获取评论列表（分页）
     */
    @Operation(summary = "根据内容 ID 获取评论列表（分页）", description = "根据内容 ID 分页获取该内容的全部评论列表")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "获取成功"),
        @ApiResponse(responseCode = "500", description = "获取失败")
    })
    @GetMapping("/content/{contentId}/page")
    @IgnoreAuth
    public R getCommentsByContentIdPage(
        @Parameter(description = "内容 ID", required = true) @PathVariable("contentId") Long contentId,
        @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer page,
        @Parameter(description = "每页数量") @RequestParam(defaultValue = "20") Integer limit) {
        try {
            Map<String, Object> params = Map.of(
                "page", page.toString(),
                "limit", limit.toString()
            );

            PageUtils pageUtils = commentService.getCommentsByContentIdPage(contentId, params);
            
            // 将 CommentView 转换为 CommentDetailVO
            List<CommentDetailVO> voList = ((List<CommentView>) pageUtils.getList())
                .stream()
                .map(this::convertViewToDetailVO)
                .collect(Collectors.toList());
            
            // 重新构建返回数据
            PageUtils resultPage = new PageUtils(voList, pageUtils.getTotalCount(), limit, page);
            
            return R.ok().put("data", resultPage);
        } catch (Exception e) {
            log.error("分页获取内容评论失败，contentId: {}", contentId, e);
            return R.error("获取评论失败");
        }
    }

    /**
     * 获取评论树结构
     */
    @Operation(summary = "获取评论树结构", description = "根据内容 ID 获取评论的树形层级结构")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "获取成功"),
        @ApiResponse(responseCode = "500", description = "获取失败")
    })
    @GetMapping("/tree/{contentId}")
    @IgnoreAuth
    public R getCommentTree(
        @Parameter(description = "内容 ID", required = true) @PathVariable("contentId") Long contentId,
        HttpServletRequest request) {
        try {
            List<CommentView> commentTree = commentService.getCommentTree(contentId);
            
            // 获取当前登录用户 ID（如果已登录）
            Long currentUserId = sessionUtils.getCurrentUserId(request);
            
            // 将 CommentView 转换为 CommentTreeVO
            List<CommentTreeVO> treeVOList = commentTree.stream()
                .map(commentView -> convertViewToTreeVO(commentView, currentUserId))
                .collect(Collectors.toList());
            
            return R.ok().put("data", treeVOList);
        } catch (Exception e) {
            log.error("获取评论树失败，contentId: {}", contentId, e);
            return R.error("获取评论树失败");
        }
    }

    /**
     * 获取评论树结构（分页，修复重复问题）
     */
    @Operation(summary = "获取评论树结构（分页）", description = "根据内容 ID 分页获取评论的树形层级结构")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "获取成功"),
        @ApiResponse(responseCode = "500", description = "获取失败")
    })
    @GetMapping("/tree/{contentId}/page")
    @IgnoreAuth
    public R getCommentTreePage(
        @Parameter(description = "内容 ID", required = true) @PathVariable("contentId") Long contentId,
        @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer page,
        @Parameter(description = "每页数量") @RequestParam(defaultValue = "20") Integer limit,
        HttpServletRequest request) {
        try {
            Map<String, Object> params = Map.of(
                "page", page.toString(),
                "limit", limit.toString()
            );

            PageUtils pageUtils = commentService.getCommentTreePage(contentId, params);
            
            // 获取当前登录用户 ID（如果已登录）
            Long currentUserId = sessionUtils.getCurrentUserId(request);
            
            // 将 CommentView 转换为 CommentTreeVO
            List<CommentTreeVO> treeVOList = ((List<CommentView>) pageUtils.getList())
                .stream()
                .map(commentView -> convertViewToTreeVO(commentView, currentUserId))
                .collect(Collectors.toList());
            
            // 重新构建返回数据
            PageUtils resultPage = new PageUtils(treeVOList, pageUtils.getTotalCount(), limit, page);
            
            return R.ok().put("data", resultPage);
        } catch (Exception e) {
            log.error("分页获取评论树失败，contentId: {}", contentId, e);
            return R.error("获取评论树失败");
        }
    }

    /**
     * 统计内容评论数量
     */
    @Operation(summary = "统计内容评论数量", description = "根据内容 ID 统计该内容的评论总数")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "统计成功"),
        @ApiResponse(responseCode = "500", description = "统计失败")
    })
    @GetMapping("/count/{contentId}")
    @IgnoreAuth
    public R countComments(
        @Parameter(description = "内容 ID", required = true) @PathVariable("contentId") Long contentId) {
        try {
            Integer count = commentService.countCommentsByContentId(contentId);
            return R.ok().put("data", count);
        } catch (Exception e) {
            log.error("统计评论数量失败，contentId: {}", contentId, e);
            return R.error("统计失败");
        }
    }

    /**
     * 创建评论
     */
    @Operation(summary = "创建评论", description = "创建新的评论记录")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "创建成功"),
        @ApiResponse(responseCode = "400", description = "创建失败")
    })
    @PostMapping
    public R createComment(
        @Parameter(description = "评论信息", required = true) @Valid @RequestBody CommentCreateDTO createDTO,
        HttpServletRequest request) {
        try {
            // 从 Session 获取用户 ID（必须登录）
            String userIdStr = sessionUtils.getSessionAttribute(request, "userId");
            if (userIdStr == null) {
                return R.error("请先登录后再发表评论");
            }
            
            Comment comment = convertToEntity(createDTO);
            
            // 设置用户 ID
            comment.setUserId(Long.parseLong(userIdStr));
            
            boolean result = commentService.createComment(comment);
            if (result) {
                return R.ok("评论创建成功");
            } else {
                return R.error("创建失败");
            }
        } catch (Exception e) {
            log.error("保存评论失败", e);
            return R.error(e.getMessage());
        }
    }

    /**
     * 回复评论
     */
    @Operation(summary = "回复评论", description = "对已有评论进行回复")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "回复成功"),
        @ApiResponse(responseCode = "400", description = "回复失败")
    })
    @PostMapping("/reply")
    public R replyComment(
        @Parameter(description = "回复信息", required = true) @Valid @RequestBody CommentReplyDTO replyDTO,
        HttpServletRequest request) {
        try {
            // 从 Session 获取用户 ID（必须登录）
            String userIdStr = sessionUtils.getSessionAttribute(request, "userId");
            if (userIdStr == null) {
                return R.error("请先登录后再回复评论");
            }
            
            Comment comment = new Comment();
            comment.setContentId(replyDTO.getContentId());
            comment.setParentId(replyDTO.getParentId());
            comment.setContent(replyDTO.getReplyContent());
            
            // 设置用户 ID
            comment.setUserId(Long.parseLong(userIdStr));
            
            boolean result = commentService.createComment(comment);
            if (result) {
                return R.ok("回复成功");
            } else {
                return R.error("回复失败");
            }
        } catch (Exception e) {
            log.error("回复评论失败", e);
            return R.error(e.getMessage());
        }
    }

    /**
     * 更新评论
     */
    @Operation(summary = "更新评论", description = "根据评论 ID 更新评论信息")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "更新成功"),
        @ApiResponse(responseCode = "400", description = "更新失败")
    })
    @PutMapping("/{id}")
    @Transactional
    public R updateComment(
        @Parameter(description = "评论 ID", required = true) @PathVariable("id") Long id, 
        @Parameter(description = "评论信息", required = true) @Valid @RequestBody CommentUpdateDTO updateDTO) {
        try {
            Comment comment = commentService.getById(id);
            if (comment == null) {
                return R.error("评论不存在");
            }
            
            // 使用转换器更新实体
            convertToUpdateEntity(updateDTO, comment);
            boolean result = commentService.updateComment(comment);
            if (result) {
                return R.ok("更新成功");
            } else {
                return R.error("更新失败");
            }
        } catch (Exception e) {
            log.error("修改评论失败，ID: {}", id, e);
            return R.error(e.getMessage());
        }
    }

    /**
     * 部分更新评论
     */
    @Operation(summary = "部分更新评论", description = "根据评论 ID 部分更新评论信息")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "更新成功"),
        @ApiResponse(responseCode = "400", description = "更新失败")
    })
    @PatchMapping("/{id}")
    @Transactional
    public R partialUpdateComment(
        @Parameter(description = "评论 ID", required = true) @PathVariable("id") Long id, 
        @Parameter(description = "评论信息") @RequestBody CommentUpdateDTO updateDTO) {
        try {
            Comment comment = commentService.getById(id);
            if (comment == null) {
                return R.error("评论不存在");
            }
            
            // 使用转换器更新实体
            convertToUpdateEntity(updateDTO, comment);
            boolean result = commentService.updateComment(comment);
            if (result) {
                return R.ok("更新成功");
            } else {
                return R.error("更新失败");
            }
        } catch (Exception e) {
            log.error("部分更新评论失败，ID: {}", id, e);
            return R.error(e.getMessage());
        }
    }

    /**
     * 启用/禁用评论
     */
    @Operation(summary = "启用/禁用评论", description = "根据评论 ID 启用或禁用评论")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "状态更新成功"),
        @ApiResponse(responseCode = "400", description = "状态更新失败")
    })
    @PatchMapping("/{id}/status")
    public R updateStatus(
        @Parameter(description = "评论 ID", required = true) @PathVariable("id") Long commentId,
        @Parameter(description = "状态 (0:显示，1:隐藏)", required = true) @RequestParam Integer status) {
        try {
            boolean result = commentService.updateStatus(commentId, status);
            if (result) {
                return R.ok("状态更新成功");
            } else {
                return R.error("状态更新失败");
            }
        } catch (Exception e) {
            log.error("更新评论状态失败，ID: {}", commentId, e);
            return R.error(e.getMessage());
        }
    }

    /**
     * 删除评论（单个）
     */
    @Operation(summary = "删除评论", description = "根据评论 ID 删除单个评论")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "删除成功"),
        @ApiResponse(responseCode = "403", description = "无权限删除"),
        @ApiResponse(responseCode = "404", description = "评论不存在"),
        @ApiResponse(responseCode = "500", description = "删除失败")
    })
    @DeleteMapping("/{id}")
    public R deleteComment(
        @Parameter(description = "评论 ID", required = true) @PathVariable("id") Long id,
        HttpServletRequest request) {
        try {
            // 获取当前用户信息
            Long currentUserId = sessionUtils.getCurrentUserId(request);
            if (currentUserId == null) {
                return R.error("请先登录");
            }
            
            // 查询评论信息
            Comment comment = commentService.getById(id);
            if (comment == null) {
                return R.error("评论不存在");
            }
            
            // 判断是否为管理员
            boolean isAdmin = authUtils.isAdmin(currentUserId);
            
            // 判断是否为文章作者
            boolean isArticleAuthor = false;
            if (comment.getContentId() != null) {
                Article article = articleService.getById(comment.getContentId());
                if (article != null && article.getAuthorId().equals(currentUserId)) {
                    isArticleAuthor = true;
                }
            }
            
            // 判断是否为评论作者
            boolean isCommentAuthor = comment.getUserId().equals(currentUserId);
            
            // 权限验证：管理员、文章作者或评论作者才能删除
            if (!isAdmin && !isArticleAuthor && !isCommentAuthor) {
                return R.error("无权限删除此评论");
            }
            
            commentService.removeById(id);
            log.info("删除评论成功，评论 ID: {}, 用户 ID: {}", id, currentUserId);
            return R.ok("删除成功");
        } catch (Exception e) {
            log.error("删除评论失败，ID: {}", id, e);
            return R.error("删除失败");
        }
    }

    /**
     * 批量删除评论
     */
    @Operation(summary = "批量删除评论", description = "批量删除多个评论")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "删除成功"),
        @ApiResponse(responseCode = "400", description = "请选择要删除的评论"),
        @ApiResponse(responseCode = "403", description = "无权限删除"),
        @ApiResponse(responseCode = "500", description = "删除失败")
    })
    @PostMapping("/batch-delete")
    public R batchDeleteComments(
        @Parameter(description = "评论 ID 数组", required = true) @RequestBody Long[] ids,
        HttpServletRequest request) {
        try {
            if (ids == null || ids.length == 0) {
                return R.error("请选择要删除的评论");
            }
            
            // 获取当前用户信息
            Long currentUserId = sessionUtils.getCurrentUserId(request);
            if (currentUserId == null) {
                return R.error("请先登录");
            }
            
            // 判断是否为管理员
            boolean isAdmin = authUtils.isAdmin(currentUserId);
            
            // 如果不是管理员，需要逐个验证权限
            if (!isAdmin) {
                for (Long id : ids) {
                    Comment comment = commentService.getById(id);
                    if (comment == null) {
                        return R.error("评论不存在，ID: " + id);
                    }
                    
                    // 判断是否为文章作者
                    boolean isArticleAuthor = false;
                    if (comment.getContentId() != null) {
                        Article article = articleService.getById(comment.getContentId());
                        if (article != null && article.getAuthorId().equals(currentUserId)) {
                            isArticleAuthor = true;
                        }
                    }
                    
                    // 判断是否为评论作者
                    boolean isCommentAuthor = comment.getUserId().equals(currentUserId);
                    
                    // 既不是管理员，也不是文章作者或评论作者，无权限删除
                    if (!isArticleAuthor && !isCommentAuthor) {
                        return R.error("无权限删除评论，ID: " + id);
                    }
                }
            }
            
            // 执行批量删除
            List<Long> commentIds = Arrays.stream(ids).collect(Collectors.toList());
            boolean result = commentService.deleteComments(commentIds);
            if (result) {
                log.info("批量删除评论成功，数量：{}, 用户 ID: {}", ids.length, currentUserId);
                return R.ok("删除成功");
            } else {
                return R.error("删除失败");
            }
        } catch (Exception e) {
            log.error("批量删除评论失败", e);
            return R.error("删除失败");
        }
    }
    
    // ==================== 私有转换方法 ====================
    
    /**
     * 将 Comment 转换为 CommentVO
     */
    private CommentVO convertToVO(Comment comment) {
        CommentVO vo = new CommentVO();
        vo.setId(comment.getId());
        vo.setContentId(comment.getContentId());
        vo.setUserId(comment.getUserId());
        vo.setUserAvatar(comment.getUserAvatar());
        vo.setUserNickname(comment.getUserNickname());
        vo.setContent(comment.getContent());
        vo.setParentId(comment.getParentId());
        vo.setLikeCount(comment.getLikeCount());
        vo.setStatus(comment.getStatus());
        vo.setCreateTime(comment.getCreateTime());
        return vo;
    }
    
    /**
     * 将 Comment 转换为 CommentDetailVO
     */
    private CommentDetailVO convertToDetailVO(Comment comment) {
        CommentDetailVO vo = new CommentDetailVO();
        vo.setId(comment.getId());
        vo.setContentId(comment.getContentId());
        vo.setUserId(comment.getUserId());
        vo.setUserAvatar(comment.getUserAvatar());
        vo.setUserNickname(comment.getUserNickname());
        vo.setContent(comment.getContent());
        vo.setReplyContent(comment.getReplyContent());
        vo.setParentId(comment.getParentId());
        vo.setLikeCount(comment.getLikeCount());
        vo.setStatus(comment.getStatus());
        vo.setCreateTime(comment.getCreateTime());
        vo.setUpdateTime(comment.getUpdateTime());
        return vo;
    }
    
    /**
     * 将 CommentView 转换为 CommentDetailVO
     */
    private CommentDetailVO convertViewToDetailVO(CommentView view) {
        CommentDetailVO vo = new CommentDetailVO();
        vo.setId(view.getId());
        vo.setContentId(view.getContentId());
        vo.setUserId(view.getUserId());
        vo.setUserAvatar(view.getUserAvatar());
        vo.setUserNickname(view.getUserNickname());
        vo.setContent(view.getContent());
        vo.setReplyContent(view.getReplyContent());
        vo.setParentId(view.getParentId());
        vo.setLikeCount(view.getLikeCount());
        vo.setStatus(view.getStatus());
        vo.setCreateTime(view.getCreateTime());
        return vo;
    }
    
    /**
     * 将 CommentView 转换为 CommentTreeVO（带用户互动状态）
     */
    private CommentTreeVO convertViewToTreeVO(CommentView view, Long currentUserId) {
        CommentTreeVO vo = new CommentTreeVO();
        vo.setId(view.getId());
        vo.setUserId(view.getUserId());
        vo.setUserAvatar(view.getUserAvatar());
        vo.setUserNickname(view.getUserNickname());
        vo.setContent(view.getContent());
        vo.setReplyContent(view.getReplyContent());
        vo.setParentId(view.getParentId());
        vo.setLikeCount(view.getLikeCount());
        vo.setCreateTime(view.getCreateTime());
        
        log.debug("转换评论树 VO: commentId={}, currentUserId={}", view.getId(), currentUserId);
        
        // 查询当前用户的互动状态
        if (currentUserId != null) {
            Boolean isLiked = interactionUtils.hasLiked(currentUserId, view.getId(), ContentType.COMMENT);
            Boolean isDisliked = interactionUtils.hasDisliked(currentUserId, view.getId(), ContentType.COMMENT);
            vo.setIsLiked(isLiked);
            vo.setIsDisliked(isDisliked);
            log.debug("设置互动状态：commentId={}, isLiked={}, isDisliked={}", view.getId(), isLiked, isDisliked);
        } else {
            vo.setIsLiked(false);
            vo.setIsDisliked(false);
            log.debug("用户未登录，设置互动状态为 false: commentId={}", view.getId());
        }
        
        // 设置层级
        if (view.getParentId() == null) {
            vo.setLevel(0);
        } else {
            vo.setLevel(1);
        }
        
        // 递归转换子评论
        if (view.getChildren() != null) {
            List<CommentTreeVO> childrenVOList = view.getChildren().stream()
                .map(child -> convertViewToTreeVO(child, currentUserId))
                .collect(Collectors.toList());
            
            // 设置子评论的层级
            for (int i = 0; i < childrenVOList.size(); i++) {
                childrenVOList.get(i).setLevel(vo.getLevel() + 1);
            }
            
            vo.setChildren(childrenVOList);
        }
        
        return vo;
    }
    
    /**
     * 将 CommentCreateDTO 转换为 Comment
     */
    private Comment convertToEntity(CommentCreateDTO dto) {
        Comment comment = new Comment();
        comment.setContentId(dto.getContentId());
        comment.setContent(dto.getContent());
        // 如果 parentId 为 0 或 null，表示一级评论，设置为 null
        comment.setParentId(dto.getParentId() != null && dto.getParentId() != 0 ? dto.getParentId() : null);
        return comment;
    }
    
    /**
     * 将 CommentUpdateDTO 转换为 Comment（更新）
     */
    private void convertToUpdateEntity(CommentUpdateDTO dto, Comment comment) {
        comment.setContent(dto.getContent());
        if (dto.getReplyContent() != null) {
            comment.setReplyContent(dto.getReplyContent());
        }
        if (dto.getStatus() != null) {
            comment.setStatus(dto.getStatus());
        }
    }
}
