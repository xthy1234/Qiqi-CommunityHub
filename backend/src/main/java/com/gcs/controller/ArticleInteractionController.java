package com.gcs.controller;

import com.gcs.annotation.IgnoreAuth;
import com.gcs.entity.Article;
import com.gcs.entity.Interaction;
import com.gcs.enums.ContentType;
import com.gcs.enums.InteractionActionType;
import com.gcs.service.ArticleService;
import com.gcs.service.InteractionService;
import com.gcs.utils.AuthUtils;
import com.gcs.utils.R;
import com.gcs.utils.SessionUtils;
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
import java.util.stream.Collectors;

/**
 * 文章互动控制器
 * 提供点赞、点踩、收藏等用户互动相关的 RESTful API 接口
 */
@Slf4j
@Tag(name = "文章互动管理", description = "文章点赞、点踩、收藏等互动操作")
@RestController
@RequestMapping("/articles")
public class ArticleInteractionController {

    @Autowired
    private ArticleService articleService;

    @Autowired
    private InteractionService interactionService;

    @Autowired
    private SessionUtils sessionUtils;

    @Autowired
    private AuthUtils authUtils;

    /**
     * 点赞文章
     */
    @Operation(summary = "点赞文章", description = "对文章进行点赞操作，如果已点赞则取消点赞")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "操作成功"),
            @ApiResponse(responseCode = "400", description = "操作失败"),
            @ApiResponse(responseCode = "401", description = "未登录")
    })
    @PostMapping("/{articleId}/likes")
    public R likeArticle(
            @Parameter(description = "文章 ID", required = true) @PathVariable("articleId") Long articleId,
            HttpServletRequest request) {
        try {
            Long userId = sessionUtils.getCurrentUserId(request);
            if (userId == null) {
                return R.error("请先登录");
            }

            Integer likeCount = articleService.toggleLike(articleId, userId);
            return R.ok().put("likeCount", likeCount);
        } catch (Exception e) {
            log.error("点赞文章失败，articleId: {}", articleId, e);
            return R.error(e.getMessage());
        }
    }

    /**
     * 点踩文章
     */
    @Operation(summary = "点踩文章", description = "对文章进行点踩操作，如果已点踩则取消点踩")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "操作成功"),
            @ApiResponse(responseCode = "400", description = "操作失败"),
            @ApiResponse(responseCode = "401", description = "未登录")
    })
    @PostMapping("/{articleId}/dislikes")
    public R dislikeArticle(
            @Parameter(description = "文章 ID", required = true) @PathVariable("articleId") Long articleId,
            HttpServletRequest request) {
        try {
            Long userId = sessionUtils.getCurrentUserId(request);
            if (userId == null) {
                return R.error("请先登录");
            }

            Integer dislikeCount = articleService.toggleDislike(articleId, userId);
            return R.ok().put("dislikeCount", dislikeCount);
        } catch (Exception e) {
            log.error("点踩文章失败，articleId: {}", articleId, e);
            return R.error(e.getMessage());
        }
    }

    /**
     * 收藏文章
     */
    @Operation(summary = "收藏文章", description = "将文章添加到收藏夹，如果已收藏则取消收藏")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "操作成功"),
            @ApiResponse(responseCode = "400", description = "操作失败"),
            @ApiResponse(responseCode = "401", description = "未登录")
    })
    @PostMapping("/{articleId}/favorites")
    public R favoriteArticle(
            @Parameter(description = "文章 ID", required = true) @PathVariable("articleId") Long articleId,
            HttpServletRequest request) {
        try {
            Long userId = sessionUtils.getCurrentUserId(request);
            if (userId == null) {
                return R.error("请先登录");
            }

            Integer favoriteCount = articleService.toggleFavorite(articleId, userId);
            return R.ok().put("favoriteCount", favoriteCount);
        } catch (Exception e) {
            log.error("收藏文章失败，articleId: {}", articleId, e);
            return R.error(e.getMessage());
        }
    }

    /**
     * 获取用户的收藏文章列表
     */
    @Operation(summary = "获取我的收藏", description = "获取当前用户收藏的文章列表")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "查询成功"),
            @ApiResponse(responseCode = "401", description = "未登录")
    })
    @GetMapping("/my/favorites")
    public R getMyFavorites(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer page,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "10") Integer limit,
            HttpServletRequest request) {
        try {
            Long userId = sessionUtils.getCurrentUserId(request);
            if (userId == null) {
                return R.error("请先登录");
            }

            List<Interaction> favoriteInteractions = interactionService.getUserInteractionsList(
                    userId,
                    InteractionActionType.FAVORITE,
                    ContentType.ARTICLE
            );

            if (favoriteInteractions == null || favoriteInteractions.isEmpty()) {
                return R.ok().put("data", new java.util.ArrayList<>());
            }

            List<Long> favoriteArticleIds = favoriteInteractions.stream()
                    .map(Interaction::getContentId)
                    .collect(Collectors.toList());

            return R.ok().put("data", favoriteArticleIds);
        } catch (Exception e) {
            log.error("获取收藏列表失败", e);
            return R.error("获取收藏列表失败");
        }
    }

    /**
     * 检查用户对文章的互动状态
     */
    @Operation(summary = "检查互动状态", description = "检查当前用户对文章的点赞、点踩、收藏状态")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "查询成功")
    })
    @GetMapping("/{articleId}/interaction-status")
    @IgnoreAuth
    public R getInteractionStatus(
            @Parameter(description = "文章 ID", required = true) @PathVariable("articleId") Long articleId,
            HttpServletRequest request) {
        try {
            Long userId = sessionUtils.getCurrentUserId(request);
            
            boolean isLiked = false;
            boolean isDisliked = false;
            boolean isFavorited = false;

            if (userId != null) {
                isLiked = interactionService.hasValidInteraction(
                        userId, articleId, InteractionActionType.LIKE, ContentType.ARTICLE
                );
                isDisliked = interactionService.hasValidInteraction(
                        userId, articleId, InteractionActionType.DISLIKE, ContentType.ARTICLE
                );
                isFavorited = interactionService.hasValidInteraction(
                        userId, articleId, InteractionActionType.FAVORITE, ContentType.ARTICLE
                );
            }

            java.util.Map<String, Object> status = new java.util.HashMap<>();
            status.put("isLiked", isLiked);
            status.put("isDisliked", isDisliked);
            status.put("isFavorited", isFavorited);

            return R.ok().put("data", status);
        } catch (Exception e) {
            log.error("获取互动状态失败，articleId: {}", articleId, e);
            return R.error("获取互动状态失败");
        }
    }
}
