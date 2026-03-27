package com.gcs.controller;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.gcs.annotation.IgnoreAuth;
import com.gcs.dto.SuggestionReviewDTO;
import com.gcs.dto.SuggestionSubmitDTO;
import com.gcs.entity.Article;
import com.gcs.entity.ArticleEditSuggestion;
import com.gcs.enums.EditMode;
import com.gcs.service.ArticleEditSuggestionService;
import com.gcs.service.ArticleService;
import com.gcs.utils.PageUtils;
import com.gcs.utils.R;
import com.gcs.vo.ArticleSuggestionVO;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import lombok.extern.slf4j.Slf4j;

/**
 * 文章修改建议控制器
 * 提供文章修改建议相关的 RESTful API 接口
 */
@Slf4j
@Tag(name = "文章修改建议管理", description = "文章修改建议相关的 RESTful API 接口，包括提交建议、审核建议、查询建议列表等功能")
@RestController
@RequestMapping("/articles/suggestions")
public class ArticleSuggestionController {
    
    @Autowired
    private ArticleService articleService;
    
    @Autowired
    private ArticleEditSuggestionService articleEditSuggestionService;
    
    /**
     * 提交修改建议
     */
    @Operation(
        summary = "提交修改建议", 
        description = "对指定文章提交修改建议，需要文章开启协作编辑模式（edit_mode=1）。不允许作者给自己提交建议。"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "提交成功", 
            content = @Content(mediaType = "application/json", 
                schema = @Schema(implementation = R.class))),
        @ApiResponse(responseCode = "400", description = "参数错误"),
        @ApiResponse(responseCode = "401", description = "未登录"),
        @ApiResponse(responseCode = "404", description = "文章不存在"),
        @ApiResponse(responseCode = "403", description = "文章未开启协作模式或作者不能给自己提建议")
    })
    @PostMapping("/{articleId}")
    @Transactional
    public R submitSuggestion(
        @Parameter(description = "文章 ID", example = "1", required = true) 
        @PathVariable Long articleId,
        
        @Parameter(description = "修改建议信息", required = true) 
        @Valid @RequestBody SuggestionSubmitDTO dto,
        
        HttpServletRequest request) {
        try {
            Long currentUserId = getCurrentUserId(request);
            if (currentUserId == null) {
                return R.error("请先登录");
            }
            
            Article article = articleService.getById(articleId);
            if (article == null) {
                return R.error("文章不存在");
            }
            
            // 检查编辑模式
            if (article.getEditMode() == null || article.getEditMode() != EditMode.ALL_SUGGEST.getCode()) {
                return R.error("该文章未开启协作编辑模式");
            }
            
            // 不允许作者给自己提建议
            if (article.getAuthorId().equals(currentUserId)) {
                return R.error("作者无需向自己提交建议");
            }
            
            articleEditSuggestionService.submitSuggestion(
                articleId, currentUserId, dto.getTitle(), dto.getContent(), dto.getChangeSummary());
            
            return R.ok("建议提交成功，等待作者审核");
        } catch (IllegalArgumentException e) {
            log.error("提交建议失败：{}", e.getMessage(), e);
            return R.error(e.getMessage());
        } catch (Exception e) {
            log.error("提交建议失败，articleId: {}", articleId, e);
            return R.error("提交建议失败");
        }
    }
    
    /**
     * 获取文章的修改建议列表
     */
    @Operation(
        summary = "获取建议列表", 
        description = "查询指定文章的所有修改建议，支持按审核状态筛选。公开接口，无需登录。"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "查询成功",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = PageUtils.class))),
        @ApiResponse(responseCode = "400", description = "参数错误"),
        @ApiResponse(responseCode = "404", description = "文章不存在")
    })
    @GetMapping("/{articleId}")
    @IgnoreAuth
    public R getSuggestions(
        @Parameter(description = "文章 ID", example = "1", required = true) 
        @PathVariable Long articleId,
        
        @Parameter(description = "审核状态筛选（0-待审核，1-已通过，2-已拒绝）", example = "0", required = false) 
        @RequestParam(required = false) Integer status,
        
        @Parameter(description = "页码", example = "1") 
        @RequestParam(defaultValue = "1") Integer page,
        
        @Parameter(description = "每页数量", example = "10") 
        @RequestParam(defaultValue = "10") Integer limit) {
        try {
            // ✅ 调用 Service 构建完整的 VO 列表
            var voPage = articleEditSuggestionService.getSuggestionsWithDetails(
                articleId, status, page, limit);
            
            return R.ok().put("data", voPage);
        } catch (Exception e) {
            log.error("获取建议列表失败，articleId: {}", articleId, e);
            return R.error("获取建议列表失败");
        }
    }
    
    /**
     * 获取单个建议详情
     */
    @Operation(
        summary = "获取建议详情", 
        description = "查询单个修改建议的详细信息，包含建议内容、当前文章内容对比等。公开接口，无需登录。"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "查询成功",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = ArticleSuggestionVO.class))),
        @ApiResponse(responseCode = "404", description = "建议不存在")
    })
    @GetMapping("/{suggestionId}/detail")
    @IgnoreAuth
    public R getSuggestionDetail(
        @Parameter(description = "建议 ID", example = "1", required = true) 
        @PathVariable Long suggestionId) {
        try {
            // ✅ 调用 Service 构建完整的 VO
            ArticleSuggestionVO vo = articleEditSuggestionService.getSuggestionDetailWithVo(suggestionId);
            return R.ok().put("data", vo);
        } catch (IllegalArgumentException e) {
            log.error("获取建议详情失败：{}", e.getMessage(), e);
            return R.error(e.getMessage());
        } catch (Exception e) {
            log.error("获取建议详情失败，suggestionId: {}", suggestionId, e);
            return R.error("获取建议详情失败");
        }
    }
    
    /**
     * 审核修改建议
     */
    @Operation(
        summary = "审核建议", 
        description = "文章作者审核修改建议，可以选择通过或拒绝。需要通过审核后会更新文章版本并记录贡献者。"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "审核成功",
            content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "400", description = "参数错误"),
        @ApiResponse(responseCode = "401", description = "未登录"),
        @ApiResponse(responseCode = "403", description = "无权限审核（非文章作者）"),
        @ApiResponse(responseCode = "404", description = "建议或文章不存在")
    })
    @PutMapping("/{suggestionId}/review")
    @Transactional
    public R reviewSuggestion(
        @Parameter(description = "建议 ID", example = "1", required = true) 
        @PathVariable Long suggestionId,
        
        @Parameter(description = "审核结果", required = true) 
        @Valid @RequestBody SuggestionReviewDTO dto,
        
        HttpServletRequest request) {
        try {
            Long currentUserId = getCurrentUserId(request);
            if (currentUserId == null) {
                return R.error("请先登录");
            }
            
            // 从建议中获取文章 ID
            ArticleEditSuggestion suggestion = articleEditSuggestionService.getSuggestionDetail(suggestionId);
            if (suggestion == null) {
                return R.error("建议不存在");
            }
            
            Article article = articleService.getById(suggestion.getArticleId());
            if (article == null) {
                return R.error("文章不存在");
            }
            
            // 验证权限（只有作者可以审核）
            if (!article.getAuthorId().equals(currentUserId)) {
                return R.error("无权限审核建议");
            }
            
            articleEditSuggestionService.reviewSuggestion(
                suggestionId, currentUserId, dto.getApproved(), dto.getReason());
            
            String message = dto.getApproved() ? "建议已通过" : "建议已拒绝";
            return R.ok(message);
        } catch (IllegalArgumentException e) {
            log.error("审核失败：{}", e.getMessage(), e);
            return R.error(e.getMessage());
        } catch (Exception e) {
            log.error("审核失败，suggestionId: {}", suggestionId, e);
            return R.error("审核失败");
        }
    }
    
    /**
     * 获取我提出的建议列表
     */
    @Operation(
        summary = "获取我提出的建议列表", 
        description = "查询当前用户向所有文章提出的修改建议，支持按审核状态筛选。需要登录。"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "查询成功",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = PageUtils.class))),
        @ApiResponse(responseCode = "401", description = "未登录")
    })
    @GetMapping("/proposed-by-me")
    public R getSuggestionsProposedByMe(
        @Parameter(description = "审核状态筛选（0-待审核，1-已通过，2-已拒绝）", example = "1") 
        @RequestParam(required = false) Integer status,
        
        @Parameter(description = "页码", example = "1") 
        @RequestParam(defaultValue = "1") Integer page,
        
        @Parameter(description = "每页数量", example = "10") 
        @RequestParam(defaultValue = "10") Integer limit,
        
        HttpServletRequest request) {
        try {
            Long currentUserId = getCurrentUserId(request);
            if (currentUserId == null) {
                return R.error("请先登录");
            }
            
            // ✅ 调用 Service 构建完整的 VO 列表
            var voPage = articleEditSuggestionService.getMySuggestionsWithDetails(
                currentUserId, status, page, limit);
            
            return R.ok().put("data", voPage);
        } catch (Exception e) {
            log.error("获取我提出的建议列表失败", e);
            return R.error("获取建议列表失败");
        }
    }
    
    /**
     * 获取我的文章收到的建议列表
     */
    @Operation(
        summary = "获取我的文章收到的建议列表", 
        description = "查询当前用户的文章收到的所有修改建议，支持按审核状态筛选。需要登录。"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "查询成功",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = PageUtils.class))),
        @ApiResponse(responseCode = "401", description = "未登录")
    })
    @GetMapping("/received-by-me")
    public R getSuggestionsReceivedByMe(
        @Parameter(description = "审核状态筛选（0-待审核，1-已通过，2-已拒绝）", example = "0") 
        @RequestParam(required = false) Integer status,
        
        @Parameter(description = "页码", example = "1") 
        @RequestParam(defaultValue = "1") Integer page,
        
        @Parameter(description = "每页数量", example = "10") 
        @RequestParam(defaultValue = "10") Integer limit,
        
        HttpServletRequest request) {
        try {
            Long currentUserId = getCurrentUserId(request);
            if (currentUserId == null) {
                return R.error("请先登录");
            }
            
            // ✅ 调用 Service 构建完整的 VO 列表
            var voPage = articleEditSuggestionService.getReceivedSuggestionsWithDetails(
                currentUserId, status, page, limit);
            
            return R.ok().put("data", voPage);
        } catch (Exception e) {
            log.error("获取我的文章收到的建议列表失败", e);
            return R.error("获取建议列表失败");
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
     * 获取会话属性
     */
    private String getSessionAttribute(HttpServletRequest request, String attributeName) {
        Object attribute = request.getSession().getAttribute(attributeName);
        return attribute != null ? attribute.toString() : null;
    }
}
