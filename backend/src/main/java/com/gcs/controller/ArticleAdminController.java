package com.gcs.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.gcs.converter.ArticleConverter;
import com.gcs.dto.BatchAuditDTO;
import com.gcs.entity.Article;
import com.gcs.entity.ArticleVersion;
import com.gcs.entity.User;
import com.gcs.entity.view.ArticleView;
import com.gcs.enums.AuditStatus;
import com.gcs.service.ArticleService;
import com.gcs.service.ArticleVersionService;
import com.gcs.service.PointsService;
import com.gcs.service.UserService;
import com.gcs.utils.AuthUtils;
import com.gcs.utils.PageUtils;
import com.gcs.utils.R;
import com.gcs.utils.SessionUtils;
import com.gcs.vo.AdminArticleDetailVO;
import com.gcs.vo.ArticleAuditHistoryVO;
import com.gcs.vo.ArticleDashboardStatsVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 文章管理控制器（管理员专用）
 * 提供文章审核、批量操作、统计数据等管理功能的 RESTful API 接口
 */
@Slf4j
@Tag(name = "文章管理后台", description = "文章审核、批量操作、统计等管理功能")
@RestController
@RequestMapping("/articles")
public class ArticleAdminController {

    @Autowired
    private ArticleService articleService;

    @Autowired
    private ArticleVersionService articleVersionService;

    @Autowired
    private PointsService pointsService;

    @Autowired
    private UserService userService;

    @Autowired
    private ArticleConverter articleConverter;

    @Autowired
    private SessionUtils sessionUtils;

    @Autowired
    private AuthUtils authUtils;

    /**
     * 管理员查询所有文章列表
     */
    @Operation(summary = "管理员查询文章列表", description = "管理员可以查询所有文章，支持按审核状态、分类、作者等条件筛选")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "查询成功"),
            @ApiResponse(responseCode = "401", description = "未登录"),
            @ApiResponse(responseCode = "403", description = "无管理员权限")
    })
    @GetMapping("/admin/list")
    public R adminGetArticles(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer page,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "10") Integer limit,
            @Parameter(description = "排序字段") @RequestParam(defaultValue = "createTime") String sort,
            @Parameter(description = "排序方式") @RequestParam(defaultValue = "desc") String order,
            @Parameter(description = "审核状态") @RequestParam(required = false) Integer auditStatus,
            @Parameter(description = "作者 ID") @RequestParam(required = false) Long authorId,
            @Parameter(description = "分类 ID") @RequestParam(required = false) Long categoryId,
            @Parameter(description = "开始日期") @RequestParam(required = false) String startDate,
            @Parameter(description = "结束日期") @RequestParam(required = false) String endDate,
            HttpServletRequest request) {
        try {
            Long currentUserId = sessionUtils.getCurrentUserId(request);
            if (currentUserId == null) {
                return R.error("请先登录");
            }

            if (!authUtils.isAdmin(currentUserId)) {
                return R.error("无管理员权限");
            }

            Map<String, Object> params = new HashMap<>();
            params.put("page", page);
            params.put("limit", limit);
            params.put("sort", sort);
            params.put("order", order);

            QueryWrapper<Article> queryWrapper = new QueryWrapper<>();
            queryWrapper.lambda().eq(Article::getDeleted, false);

            if (auditStatus != null) {
                queryWrapper.lambda().eq(Article::getAuditStatus, auditStatus);
            }
            if (authorId != null) {
                queryWrapper.lambda().eq(Article::getAuthorId, authorId);
            }
            if (categoryId != null) {
                queryWrapper.lambda().eq(Article::getCategoryId, categoryId);
            }
            if (startDate != null && !startDate.isEmpty()) {
                queryWrapper.lambda().ge(Article::getCreateTime, LocalDate.parse(startDate).atStartOfDay());
            }
            if (endDate != null && !endDate.isEmpty()) {
                queryWrapper.lambda().le(Article::getCreateTime, LocalDate.parse(endDate).atTime(23, 59, 59));
            }

            PageUtils pageResult = articleService.adminQueryPage(params, queryWrapper);

            List<ArticleView> articleViews = (List<ArticleView>) pageResult.getList();
            List<AdminArticleDetailVO> voList = articleViews.stream()
                    .map(this::convertToAdminVO)
                    .collect(Collectors.toList());

            pageResult.setList(voList);

            return R.ok().put("data", pageResult);
        } catch (Exception e) {
            log.error("管理员查询文章列表失败", e);
            return R.error("查询失败");
        }
    }

    /**
     * 管理员获取文章详情
     */
    @Operation(summary = "管理员获取文章详情", description = "管理员查看文章完整信息，包含审核信息等敏感字段")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "查询成功"),
            @ApiResponse(responseCode = "401", description = "未登录"),
            @ApiResponse(responseCode = "403", description = "无管理员权限"),
            @ApiResponse(responseCode = "404", description = "文章不存在")
    })
    @GetMapping("/admin/{id}")
    public R adminGetArticleDetail(
            @Parameter(description = "文章 ID") @PathVariable("id") Long id,
            HttpServletRequest request) {
        try {
            Long currentUserId = sessionUtils.getCurrentUserId(request);
            if (currentUserId == null) {
                return R.error("请先登录");
            }

            if (!authUtils.isAdmin(currentUserId)) {
                return R.error("无管理员权限");
            }

            ArticleView articleView = articleService.selectViewById(id);
            if (articleView == null) {
                return R.error("文章不存在");
            }

            AdminArticleDetailVO vo = convertToAdminVO(articleView);

            List<ArticleVersion> versions = articleVersionService.getVersionHistory(id);
            vo.setVersionCount(versions != null ? versions.size() : 0);
            vo.setContributorCount(0);

            return R.ok().put("data", vo);
        } catch (Exception e) {
            log.error("管理员获取文章详情失败，ID: {}", id, e);
            return R.error("获取失败");
        }
    }

    /**
     * 批量审核文章
     */
    @Operation(summary = "批量审核文章", description = "管理员批量审核多篇文章")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "审核成功"),
            @ApiResponse(responseCode = "401", description = "未登录"),
            @ApiResponse(responseCode = "403", description = "无管理员权限")
    })
    @PostMapping("/batch-audit")
    @Transactional
    public R batchAudit(@Valid @RequestBody BatchAuditDTO auditDTO, HttpServletRequest request) {
        try {
            AuditStatus auditStatus = AuditStatus.valueOf(auditDTO.getStatus());

            Long currentUserId = sessionUtils.getCurrentUserId(request);
            if (currentUserId == null) {
                return R.error("请先登录");
            }

            if (!authUtils.isAdmin(currentUserId)) {
                return R.error("无管理员权限");
            }

            List<Article> articles = new ArrayList<>();
            for (Long id : auditDTO.getIds()) {
                Article article = articleService.getById(id);
                if (article != null) {
                    Integer oldStatus = article.getAuditStatus().getCode();

                    article.setAuditStatus(auditStatus);
                    article.setAuditReply(auditDTO.getReply());

                    if (auditStatus == AuditStatus.APPROVED && article.getPublishTime() == null) {
                        article.setPublishTime(new Date());
                    }

                    articles.add(article);

                    if (auditStatus == AuditStatus.APPROVED) {
                        boolean hasReceivedPoints = checkUserReceivedPostPoints(article.getAuthorId(), id);
                        if (!hasReceivedPoints) {
                            pointsService.addPoints(article.getAuthorId(), "post_article", id, "发布文章并通过审核");
                            log.info("文章审核通过发放积分，articleId: {}, authorId: {}", id, article.getAuthorId());
                        }
                    }

                    articleService.recordAuditHistory(id, currentUserId, oldStatus, auditDTO.getStatus(), auditDTO.getReply());
                }
            }
            articleService.updateBatchById(articles);
            return R.ok();
        } catch (Exception e) {
            log.error("批量审核失败", e);
            return R.error("审核失败");
        }
    }

    /**
     * 批量修改文章分类
     */
    @Operation(summary = "批量修改文章分类", description = "管理员批量修改多篇文章的分类")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "修改成功"),
            @ApiResponse(responseCode = "401", description = "未登录"),
            @ApiResponse(responseCode = "403", description = "无管理员权限")
    })
    @PostMapping("/admin/batch-update-category")
    @Transactional
    public R batchUpdateCategory(
            @Parameter(description = "文章 ID 数组") @RequestBody Long[] articleIds,
            @Parameter(description = "新分类 ID") @RequestParam Long categoryId,
            HttpServletRequest request) {
        try {
            Long currentUserId = sessionUtils.getCurrentUserId(request);
            if (currentUserId == null) {
                return R.error("请先登录");
            }

            if (!authUtils.isAdmin(currentUserId)) {
                return R.error("无管理员权限");
            }

            boolean result = articleService.batchUpdateCategory(articleIds, categoryId);
            if (result) {
                return R.ok("分类修改成功");
            } else {
                return R.error("分类修改失败");
            }
        } catch (Exception e) {
            log.error("批量修改文章分类失败", e);
            return R.error("修改失败");
        }
    }

    /**
     * 设置文章推荐/置顶
     */
    @Operation(summary = "设置文章推荐/置顶", description = "管理员设置文章为推荐或置顶状态")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "设置成功"),
            @ApiResponse(responseCode = "401", description = "未登录"),
            @ApiResponse(responseCode = "403", description = "无管理员权限")
    })
    @PutMapping("/{id}/featured")
    public R setFeatured(
            @Parameter(description = "文章 ID") @PathVariable("id") Long id,
            @Parameter(description = "是否推荐") @RequestParam Boolean isFeatured,
            @Parameter(description = "推荐等级") @RequestParam(required = false) Integer featuredLevel,
            HttpServletRequest request) {
        try {
            Long currentUserId = sessionUtils.getCurrentUserId(request);
            if (currentUserId == null) {
                return R.error("请先登录");
            }

            if (!authUtils.isAdmin(currentUserId)) {
                return R.error("无管理员权限");
            }

            boolean result = articleService.setFeatured(id, isFeatured, featuredLevel);
            if (result) {
                return R.ok("设置成功");
            } else {
                return R.error("设置失败");
            }
        } catch (Exception e) {
            log.error("设置文章推荐状态失败，ID: {}", id, e);
            return R.error("设置失败");
        }
    }

    /**
     * 获取管理后台统计数据
     */
    @Operation(summary = "获取管理后台统计数据", description = "获取文章相关的统计数据，用于仪表盘展示")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "获取成功"),
            @ApiResponse(responseCode = "401", description = "未登录"),
            @ApiResponse(responseCode = "403", description = "无管理员权限")
    })
    @GetMapping("/admin/dashboard-stats")
    public R getDashboardStats(HttpServletRequest request) {
        try {
            Long currentUserId = sessionUtils.getCurrentUserId(request);
            if (currentUserId == null) {
                return R.error("请先登录");
            }

            if (!authUtils.isAdmin(currentUserId)) {
                return R.error("无管理员权限");
            }

            ArticleDashboardStatsVO statsVO = articleService.getDashboardStats();
            return R.ok().put("data", statsVO);
        } catch (Exception e) {
            log.error("获取统计数据失败", e);
            return R.error("获取失败");
        }
    }

    /**
     * 获取文章审核历史
     */
    @Operation(summary = "获取文章审核历史", description = "查看文章的所有审核操作记录")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "获取成功"),
            @ApiResponse(responseCode = "401", description = "未登录"),
            @ApiResponse(responseCode = "403", description = "无管理员权限")
    })
    @GetMapping("/{id}/audit-history")
    public R getAuditHistory(
            @Parameter(description = "文章 ID") @PathVariable("id") Long id,
            HttpServletRequest request) {
        try {
            Long currentUserId = sessionUtils.getCurrentUserId(request);
            if (currentUserId == null) {
                return R.error("请先登录");
            }

            if (!authUtils.isAdmin(currentUserId)) {
                return R.error("无管理员权限");
            }

            List<ArticleAuditHistoryVO> historyList = articleService.getAuditHistory(id);
            return R.ok().put("data", historyList);
        } catch (Exception e) {
            log.error("获取审核历史失败，ID: {}", id, e);
            return R.error("获取失败");
        }
    }

    /**
     * 检查用户是否已经获得过某篇文章的发布积分
     */
    private boolean checkUserReceivedPostPoints(Long userId, Long articleId) {
        QueryWrapper<com.gcs.entity.PointsTransaction> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId)
                .eq("source", "post_article")
                .eq("source_id", articleId);

        return pointsService.count(wrapper) > 0;
    }

    /**
     * 转换为管理员 VO
     */
    private AdminArticleDetailVO convertToAdminVO(ArticleView articleView) {
        AdminArticleDetailVO vo = new AdminArticleDetailVO();
        vo.setId(articleView.getId());
        vo.setTitle(articleView.getTitle());
        vo.setCoverUrl(articleView.getCoverUrl());
        vo.setCategoryId(articleView.getCategoryId());
        vo.setCategoryName(articleView.getCategoryName());
        vo.setAuthorId(articleView.getAuthorId());
        vo.setAuthorNickname(articleView.getAuthorNickname());
        vo.setAuthorAvatar(articleView.getAuthorAvatar());
        vo.setViewCount(articleView.getViewCount());
        vo.setLikeCount(articleView.getLikeCount());
        vo.setDislikeCount(articleView.getDislikeCount());
        vo.setFavoriteCount(articleView.getFavoriteCount());
        vo.setCommentCount(articleView.getCommentCount());
        vo.setAuditStatus(articleView.getAuditStatus().getCode());
        vo.setAuditReply(articleView.getAuditReply());
        vo.setEditMode(articleView.getEditMode());
        vo.setPublishTime(articleView.getPublishTime());
        vo.setCreateTime(articleView.getCreateTime());
        vo.setUpdateTime(articleView.getUpdateTime());

        vo.setIsTop(false);
        vo.setIsFeatured(false);
        vo.setMajorVersion(1);
        vo.setMinorVersion(0);

        return vo;
    }
}
