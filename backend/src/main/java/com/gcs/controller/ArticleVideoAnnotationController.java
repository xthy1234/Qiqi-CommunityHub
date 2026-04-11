package com.gcs.controller;


import com.gcs.annotation.IgnoreAuth;
import com.gcs.dto.VideoAnnotationCreateDTO;
import com.gcs.dto.VideoAnnotationUpdateDTO;
import com.gcs.service.ArticleVideoAnnotationService;
import com.gcs.utils.R;
import com.gcs.vo.VideoAnnotationDetailVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 文章视频注释控制器
 * @author 
 * @date 2026-04-05
 */
@Slf4j
@Tag(name = "文章视频注释管理", description = "文章视频时间轴注释的 RESTful API")
@RestController
@RequestMapping("/api/articles/{articleId}/annotations")
public class ArticleVideoAnnotationController {

    @Autowired
    private ArticleVideoAnnotationService annotationService;

    @Operation(summary = "创建视频注释", description = "为文章的指定视频添加时间轴注释（仅作者可操作）")
    @PostMapping
    public R createAnnotation(
            @Parameter(description = "文章 ID", required = true) @PathVariable Long articleId,
            @Valid @RequestBody VideoAnnotationCreateDTO dto,
            HttpServletRequest request) {
        try {
            Long creatorId = getCurrentUserId(request);
            if (creatorId == null) {
                return R.error("请先登录");
            }

            VideoAnnotationDetailVO result = annotationService.createAnnotation(articleId, creatorId, dto);
            return R.ok("注释创建成功").put("data", result);
        } catch (IllegalArgumentException e) {
            log.warn("创建注释失败: {}", e.getMessage());
            return R.error(e.getMessage());
        } catch (Exception e) {
            log.error("创建注释异常", e);
            return R.error("创建注释失败");
        }
    }

    @Operation(summary = "获取文章的注释列表", description = "获取指定文章的所有注释或指定视频的注释")
    @GetMapping
    @IgnoreAuth
    public R getAnnotations(
            @Parameter(description = "文章 ID", required = true) @PathVariable Long articleId,
            @Parameter(description = "视频 URL（可选）") @RequestParam(required = false) String videoUrl) {
        try {
            List<VideoAnnotationDetailVO> annotations = annotationService.getAnnotationsByArticle(articleId, videoUrl);
            return R.ok().put("data", annotations);
        } catch (Exception e) {
            log.error("获取注释列表失败", e);
            return R.error("获取注释列表失败");
        }
    }

    @Operation(summary = "获取单个注释详情", description = "根据注释 ID 获取详细信息")
    @GetMapping("/{annotationId}")
    @IgnoreAuth
    public R getAnnotation(
            @Parameter(description = "文章 ID", required = true) @PathVariable Long articleId,
            @Parameter(description = "注释 ID", required = true) @PathVariable Long annotationId) {
        try {
            VideoAnnotationDetailVO annotation = annotationService.getAnnotationById(annotationId);
            if (annotation == null) {
                return R.error("注释不存在");
            }
            
            if (!annotation.getArticleId().equals(articleId)) {
                return R.error("注释不属于该文章");
            }
            
            return R.ok().put("data", annotation);
        } catch (Exception e) {
            log.error("获取注释详情失败", e);
            return R.error("获取注释详情失败");
        }
    }

    @Operation(summary = "更新视频注释", description = "修改注释内容（仅作者或管理员可操作）")
    @PutMapping("/{annotationId}")
    public R updateAnnotation(
            @Parameter(description = "文章 ID", required = true) @PathVariable Long articleId,
            @Parameter(description = "注释 ID", required = true) @PathVariable Long annotationId,
            @Valid @RequestBody VideoAnnotationUpdateDTO dto,
            HttpServletRequest request) {
        try {
            Long operatorId = getCurrentUserId(request);
            if (operatorId == null) {
                return R.error("请先登录");
            }

            VideoAnnotationDetailVO result = annotationService.updateAnnotation(articleId, annotationId, operatorId, dto);
            return R.ok("注释更新成功").put("data", result);
        } catch (SecurityException e) {
            log.warn("权限不足: {}", e.getMessage());
            return R.error(403, e.getMessage());
        } catch (IllegalArgumentException e) {
            log.warn("更新注释失败: {}", e.getMessage());
            return R.error(e.getMessage());
        } catch (Exception e) {
            log.error("更新注释异常", e);
            return R.error("更新注释失败");
        }
    }

    @Operation(summary = "删除视频注释", description = "删除指定的注释（仅作者或管理员可操作）")
    @DeleteMapping("/{annotationId}")
    public R deleteAnnotation(
            @Parameter(description = "文章 ID", required = true) @PathVariable Long articleId,
            @Parameter(description = "注释 ID", required = true) @PathVariable Long annotationId,
            HttpServletRequest request) {
        try {
            Long operatorId = getCurrentUserId(request);
            if (operatorId == null) {
                return R.error("请先登录");
            }

            boolean success = annotationService.deleteAnnotation(articleId, annotationId, operatorId);
            if (success) {
                return R.ok("注释删除成功");
            } else {
                return R.error("注释删除失败");
            }
        } catch (SecurityException e) {
            log.warn("权限不足: {}", e.getMessage());
            return R.error(403, e.getMessage());
        } catch (IllegalArgumentException e) {
            log.warn("删除注释失败: {}", e.getMessage());
            return R.error(e.getMessage());
        } catch (Exception e) {
            log.error("删除注释异常", e);
            return R.error("删除注释失败");
        }
    }

    @Operation(summary = "分页查询注释列表", description = "支持多条件筛选和排序（管理后台使用）")
    @GetMapping("/page")
    public R queryPage(@RequestParam Map<String, Object> params) {
        try {
            return R.ok().put("data", annotationService.queryPage(params));
        } catch (Exception e) {
            log.error("分页查询失败", e);
            return R.error("分页查询失败");
        }
    }

    private Long getCurrentUserId(HttpServletRequest request) {
        Object userId = request.getAttribute("userId");
        if (userId == null) {
            userId = request.getSession().getAttribute("userId");
        }
        return userId != null ? Long.valueOf(userId.toString()) : null;
    }
}
