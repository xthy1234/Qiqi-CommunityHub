package com.gcs.controller;

import java.util.List;
import java.util.Map;

import com.gcs.annotation.IgnoreAuth;
import com.gcs.entity.Article;
import com.gcs.entity.ArticleDraft;
import com.gcs.service.ArticleDraftService;
import com.gcs.service.ArticleService;
import com.gcs.service.ArticleVersionService;
import com.gcs.utils.R;
import com.gcs.vo.ArticleVersionSimpleVO;
import com.gcs.vo.ArticleVersionVO;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import lombok.extern.slf4j.Slf4j;

/**
 * 文章版本控制器
 * 提供文章版本管理相关的 RESTful API 接口
 */
@Slf4j
@Tag(name = "文章版本管理", description = "文章版本历史、版本对比、版本回滚等操作的 RESTful API 接口")
@RestController
@RequestMapping("/articles/{articleId}/versions")
public class ArticleVersionController {
    
    @Autowired
    private ArticleService articleService;
    
    @Autowired
    private ArticleVersionService articleVersionService;

    private ArticleDraftService articleDraftService;
    /**
     * 获取文章版本列表
     */
    @Operation(
        summary = "获取文章版本列表", 
        description = "查询文章的所有历史版本列表（按创建时间倒序）"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "查询成功",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = List.class))),
        @ApiResponse(responseCode = "404", description = "文章不存在")
    })
    @GetMapping
    @IgnoreAuth
    public R getVersionList(
        @Parameter(description = "文章 ID", example = "1", required = true) 
        @PathVariable Long articleId) {
        try {
            // ✅ 使用新的 Service 方法，返回包含详细信息的简略 VO 列表
            List<ArticleVersionSimpleVO> versions = articleVersionService.getVersionHistoryWithDetails(articleId);
            
            return R.ok().put("data", versions);
        } catch (Exception e) {
            log.error("获取版本列表失败，articleId: {}", articleId, e);
            return R.error("获取版本列表失败");
        }
    }
    
    /**
     * 获取指定版本详情
     */
    @Operation(
        summary = "获取指定版本详情", 
        description = "查询文章的某个历史版本详细信息（包含完整内容）"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "查询成功",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ArticleVersionVO.class))),
        @ApiResponse(responseCode = "404", description = "版本不存在")
    })
    @GetMapping("/{version}")
    @IgnoreAuth
    public R getVersionDetail(
        @Parameter(description = "文章 ID", example = "1", required = true) 
        @PathVariable Long articleId,
        
        @Parameter(description = "版本号", example = "1", required = true) 
        @PathVariable Integer version) {
        try {
            // ✅ 使用新的 Service 方法，返回完整的详细 VO
            ArticleVersionVO vo = articleVersionService.getVersionDetailWithVo(articleId, version);
            if (vo == null) {
                return R.error("版本不存在");
            }

            return R.ok().put("data", vo);
        } catch (Exception e) {
            log.error("获取版本详情失败，articleId: {}, version: {}", articleId, version, e);
            return R.error("获取版本详情失败");
        }
    }
    
    /**
     * 对比两个版本
     */
    @Operation(
        summary = "对比两个版本", 
        description = "对比文章的两个历史版本（返回两个版本的完整数据用于前端 diff）"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "对比成功",
            content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "404", description = "版本不存在")
    })
    @GetMapping("/compare")
    @IgnoreAuth
    public R compareVersions(
        @Parameter(description = "文章 ID", example = "1", required = true) 
        @PathVariable Long articleId,
        
        @Parameter(description = "版本 A", example = "1", required = true) 
        @RequestParam Integer versionA,
        
        @Parameter(description = "版本 B", example = "2", required = true) 
        @RequestParam Integer versionB) {
        try {
            // ✅ 使用新的 Service 方法，返回两个版本的完整 VO
            Map<String, Object> result = articleVersionService.compareVersionsWithVo(articleId, versionA, versionB);
            
            return R.ok().put("data", result);
        } catch (IllegalArgumentException e) {
            log.error("版本对比失败：{}", e.getMessage(), e);
            return R.error(e.getMessage());
        } catch (Exception e) {
            log.error("版本对比失败，articleId: {}, versionA: {}, versionB: {}", 
                     articleId, versionA, versionB, e);
            return R.error("版本对比失败");
        }
    }
    
    /**
     * 回滚到指定版本
     */
    @Operation(
        summary = "回滚到指定版本", 
        description = "将文章内容回滚到某个历史版本（需要作者或管理员权限）"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "回滚成功",
            content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "401", description = "未登录"),
        @ApiResponse(responseCode = "403", description = "无权限"),
        @ApiResponse(responseCode = "404", description = "文章或版本不存在")
    })
    @PostMapping("/{version}/rollback")
    @Transactional
    public R rollbackToVersion(
        @Parameter(description = "文章 ID", example = "1", required = true) 
        @PathVariable Long articleId,
        
        @Parameter(description = "版本号", example = "1", required = true) 
        @PathVariable Integer version,
        
        HttpServletRequest request) {
        try {
            Long currentUserId = getCurrentUserId(request);
            if (currentUserId == null) {
                return R.error("请先登录");
            }

            // 验证权限（只有作者或管理员可以回滚）
            Article article = articleService.getById(articleId);
            if (article == null) {
                return R.error("文章不存在");
            }

            boolean isAdmin = checkIsAdmin(currentUserId);
            if (!isAdmin && !article.getAuthorId().equals(currentUserId)) {
                return R.error("无权限执行回滚操作");
            }

            articleVersionService.rollbackToVersion(articleId, version, currentUserId);

            return R.ok("回滚成功");
        } catch (IllegalArgumentException e) {
            log.error("回滚失败：{}", e.getMessage(), e);
            return R.error(e.getMessage());
        } catch (Exception e) {
            log.error("回滚失败，articleId: {}, version: {}", articleId, version, e);
            return R.error("回滚失败");
        }
    }
    
    // ==================== 辅助方法 ====================
    
    /**
     * 获取当前登录用户 ID
     */
    private Long getCurrentUserId(HttpServletRequest request) {
        String userIdStr = getSessionAttribute(request, "userId");
        return userIdStr != null ? Long.parseLong(userIdStr) : null;
    }
    
    /**
     * 检查是否为管理员
     */
    private boolean checkIsAdmin(Long userId) {
        // TODO: 实现管理员权限检查逻辑
        // 暂时返回 false
        return false;
    }
    
    /**
     * 获取会话属性
     */
    private String getSessionAttribute(HttpServletRequest request, String attributeName) {
        Object attribute = request.getSession().getAttribute(attributeName);
        return attribute != null ? attribute.toString() : null;
    }

    // 在 ArticleVersionController 中更新保存相关接口

    /**
     * 手动保存（创建小版本）
     */
    @Operation(summary = "手动保存文章", description = "用户点击保存按钮，基于草稿创建小版本并更新文章")
    @PostMapping("/{version}/save")
    public R saveArticle(@PathVariable Long articleId,
                         @Parameter(description = "标题和内容")
                         @RequestBody Map<String, Object> params,
                         HttpServletRequest request) {
        try {
            Long userId = getCurrentUserId(request);
            if (userId == null) {
                return R.error("请先登录");
            }

            Article article = articleService.getById(articleId);
            if (article == null) {
                return R.error("文章不存在");
            }

            // 验证权限
            if (!article.getAuthorId().equals(userId)) {
                return R.error("无权限修改此文章");
            }

            String title = (String) params.get("title");
            Map<String, Object> content = (Map<String, Object>) params.get("content");
            String changeSummary = (String) params.get("changeSummary");

            // ✅ 创建小版本并更新文章
            articleService.saveWithMinorVersion(articleId, userId, title, content, changeSummary);

            // ✅ 删除草稿
            articleDraftService.deleteDraft(articleId, userId);

            return R.ok("保存成功");
        } catch (Exception e) {
            log.error("保存文章失败，articleId: {}", articleId, e);
            return R.error("保存失败");
        }
    }

    /**
     * 标记为大版本
     */
    @Operation(summary = "标记为大版本", description = "用户主动触发，创建大版本记录")
    @PostMapping("/{version}/mark-major")
    public R markMajorVersion(@PathVariable Long articleId,
                              @RequestParam(required = false) String changeSummary,
                              HttpServletRequest request) {
        try {
            Long userId = getCurrentUserId(request);
            if (userId == null) {
                return R.error("请先登录");
            }

            Article article = articleService.getById(articleId);
            if (article == null) {
                return R.error("文章不存在");
            }

            // 验证权限
            if (!article.getAuthorId().equals(userId)) {
                return R.error("无权限修改此文章");
            }

            // ✅ 创建大版本
            articleVersionService.createMajorVersion(article, userId,
                    changeSummary != null ? changeSummary : "标记为大版本");

            // ✅ 删除草稿
            articleDraftService.deleteDraft(articleId, userId);

            return R.ok("已成功标记为大版本");
        } catch (Exception e) {
            log.error("标记大版本失败，articleId: {}", articleId, e);
            return R.error("标记失败");
        }
    }


}
