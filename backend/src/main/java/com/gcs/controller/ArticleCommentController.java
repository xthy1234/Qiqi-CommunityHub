package com.gcs.controller;

import com.gcs.annotation.IgnoreAuth;
import com.gcs.entity.view.CommentView;
import com.gcs.enums.ContentType;
import com.gcs.service.CommentService;
import com.gcs.utils.InteractionUtils;
import com.gcs.utils.PageUtils;
import com.gcs.utils.R;
import com.gcs.utils.SessionUtils;
import com.gcs.vo.ArticleCommentVO;
import com.gcs.vo.CommentTreeVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 文章评论控制器
 * 提供文章评论相关的 RESTful API 接口
 */
@Slf4j
@Tag(name = "文章评论管理", description = "文章评论相关的 RESTful API 接口")
@RestController
@RequestMapping("/articles")
public class ArticleCommentController {
    
    @Autowired
    private CommentService commentService;
    
    @Autowired
    private SessionUtils sessionUtils;
    
    @Autowired
    private InteractionUtils interactionUtils;
    
    /**
     * 获取文章评论列表（分页，主评论 + 前3条高赞子评论）
     */
    @Operation(summary = "获取文章评论列表", description = "分页获取文章的评论列表，每条主评论包含点赞数最高的前3条子评论")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "获取成功"),
        @ApiResponse(responseCode = "500", description = "获取失败")
    })
    @GetMapping("/{articleId}/comments")
    @IgnoreAuth
    public R getArticleComments(
        @Parameter(description = "文章 ID", required = true) @PathVariable("articleId") Long articleId,
        @Parameter(description = "评论类型：list-列表，tree-树形") @RequestParam(defaultValue = "list") String type,
        @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer page,
        @Parameter(description = "每页数量") @RequestParam(defaultValue = "10") Integer size,
        HttpServletRequest request) {
        try {
            // 根据 type 参数返回不同格式
            if ("tree".equalsIgnoreCase(type)) {
                return getCommentTreeInternal(articleId, page, size, request);
            }
            
            PageUtils pageUtils = commentService.getArticleComments(articleId, page, size);
            
            // 转换为 VO 并添加互动状态
            Long currentUserId = sessionUtils.getCurrentUserId(request);
            List<ArticleCommentVO> commentVOs = (List<ArticleCommentVO>) pageUtils.getList();
            List<ArticleCommentVO> voList = commentVOs.stream()
                .map(commentVO -> addInteractionStatus(commentVO, currentUserId))
                .collect(Collectors.toList());
            
            PageUtils resultPage = new PageUtils(voList, pageUtils.getTotalCount(), size, page);
            return R.ok().put("data", resultPage);
        } catch (Exception e) {
            log.error("获取文章评论失败，articleId: {}", articleId, e);
            return R.error("获取评论失败");
        }
    }
    
    /**
     * 内部方法：获取评论树
     */
    @SuppressWarnings("unchecked")
    private R getCommentTreeInternal(Long articleId, Integer page, Integer size, HttpServletRequest request) {
        Map<String, Object> params = Map.of(
            "page", page.toString(),
            "limit", size.toString()
        );
        
        PageUtils pageUtils = commentService.getCommentTreePage(articleId, params);
        Long currentUserId = sessionUtils.getCurrentUserId(request);
        
        List<CommentView> commentViews = (List<CommentView>) pageUtils.getList();
        List<CommentTreeVO> treeVOList = commentViews.stream()
            .map(commentView -> convertViewToTreeVO(commentView, currentUserId))
            .collect(Collectors.toList());
        
        PageUtils resultPage = new PageUtils(treeVOList, pageUtils.getTotalCount(), size, page);
        return R.ok().put("data", resultPage);
    }

    /**
     * 统计文章评论数量
     */
    @Operation(summary = "统计文章评论数量", description = "根据文章 ID 统计该文章的评论总数")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "统计成功"),
        @ApiResponse(responseCode = "500", description = "统计失败")
    })
    @GetMapping("/{articleId}/comments/count")
    @IgnoreAuth
    public R countArticleComments(
        @Parameter(description = "文章 ID", required = true) @PathVariable("articleId") Long articleId) {
        try {
            Integer count = commentService.countCommentsByContentId(articleId);
            return R.ok().put("data", count);
        } catch (Exception e) {
            log.error("统计评论数量失败，articleId: {}", articleId, e);
            return R.error("统计失败");
        }
    }

    /**
     * 为 ArticleCommentVO 添加互动状态字段
     */
    private ArticleCommentVO addInteractionStatus(ArticleCommentVO vo, Long currentUserId) {
        if (currentUserId != null) {
            Boolean isLiked = interactionUtils.hasLiked(currentUserId, vo.getId(), ContentType.COMMENT);
            Boolean isDisliked = interactionUtils.hasDisliked(currentUserId, vo.getId(), ContentType.COMMENT);
            vo.setIsLiked(isLiked);
            vo.setIsDisliked(isDisliked);
        } else {
            vo.setIsLiked(false);
            vo.setIsDisliked(false);
        }
        
        // 为子评论也添加互动状态
        if (vo.getTopReplies() != null) {
            vo.getTopReplies().forEach(reply -> {
                if (currentUserId != null) {
                    Boolean isLiked = interactionUtils.hasLiked(currentUserId, reply.getId(), ContentType.COMMENT);
                    Boolean isDisliked = interactionUtils.hasDisliked(currentUserId, reply.getId(), ContentType.COMMENT);
                    reply.setIsLiked(isLiked);
                    reply.setIsDisliked(isDisliked);
                } else {
                    reply.setIsLiked(false);
                    reply.setIsDisliked(false);
                }
            });
        }
        
        return vo;
    }

    /**
     * 将 CommentView 转换为 CommentTreeVO（带用户互动状态）
     */
    private CommentTreeVO convertViewToTreeVO(CommentView view, Long currentUserId) {
        CommentTreeVO vo = new CommentTreeVO();
        vo.setId(view.getId());
        vo.setUserId(view.getUserId());
        vo.setContent(view.getContent());
        vo.setReplyId(view.getReplyId());
        vo.setParentId(view.getParentId());
        vo.setLikeCount(view.getLikeCount());
        vo.setDislikeCount(view.getDislikeCount());
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
}
