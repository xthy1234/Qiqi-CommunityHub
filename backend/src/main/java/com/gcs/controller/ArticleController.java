package com.gcs.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gcs.annotation.IgnoreAuth;
import com.gcs.converter.ArticleConverter;
import com.gcs.dto.ArticleCreateDTO;
import com.gcs.dto.ArticleUpdateDTO;
import com.gcs.entity.Article;
import com.gcs.entity.ArticleVersion;
import com.gcs.entity.Interaction;
import com.gcs.entity.view.ArticleView;
import com.gcs.enums.AuditStatus;
import com.gcs.enums.EditMode;
import com.gcs.service.ArticleService;
import com.gcs.service.ArticleVersionService;
import com.gcs.utils.AuthUtils;
import com.gcs.utils.InteractionUtils;
import com.gcs.utils.PageUtils;
import com.gcs.utils.R;
import com.gcs.utils.SessionUtils;
import com.gcs.vo.ArticleDetailVO;
import com.gcs.vo.ArticleSearchVO;
import com.gcs.vo.ArticleVO;
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

import java.util.*;
import java.util.stream.Collectors;

/**
 * 文章核心控制器
 * 提供文章的基础增删改查、搜索等核心功能的 RESTful API 接口
 */
@Slf4j
@Tag(name = "文章核心管理", description = "文章的基础增删改查、搜索等核心功能")
@RestController
@RequestMapping("/articles")
public class ArticleController {

    @Autowired
    private ArticleService articleService;

    @Autowired
    private ArticleVersionService articleVersionService;

    @Autowired
    private ArticleConverter articleConverter;

    @Autowired
    private SessionUtils sessionUtils;

    @Autowired
    private AuthUtils authUtils;

    @Autowired
    private InteractionUtils interactionUtils;

    /**
     * 获取文章分页列表（通用接口）
     */
    @Operation(summary = "获取文章分页列表", description = "分页查询文章列表，支持多种条件筛选")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "查询成功"),
            @ApiResponse(responseCode = "400", description = "参数错误"),
            @ApiResponse(responseCode = "500", description = "服务器错误")
    })
    @GetMapping
    @IgnoreAuth
    public R getPage(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer page,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "10") Integer limit,
            @Parameter(description = "排序字段") @RequestParam(defaultValue = "createTime") String sort,
            @Parameter(description = "排序方式 (asc/desc)") @RequestParam(defaultValue = "desc") String order,
            @Parameter(description = "审核状态") @RequestParam(required = false) Integer auditStatus,
            @Parameter(description = "作者 ID") @RequestParam(required = false) Long authorId,
            @Parameter(description = "分类 ID") @RequestParam(required = false) Long categoryId,
            @Parameter(description = "查询类型 (all/published/my/favorite/pending)") @RequestParam(defaultValue = "all") String type,
            HttpServletRequest request) {
        try {
            Map<String, Object> params = new HashMap<>();
            params.put("page", page);
            params.put("limit", limit);
            params.put("sort", sort);
            params.put("order", order);

            QueryWrapper<Article> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("deleted", false);

            Long currentUserId = sessionUtils.getCurrentUserId(request);
            boolean auditStatusIgnored = false;
            boolean authorIdIgnored = false;

            switch (type) {
                case "all":
                    queryWrapper.eq("a.audit_status", AuditStatus.APPROVED.getCode());
                    auditStatusIgnored = true;
                    break;

                case "my":
                    if (currentUserId == null) {
                        return R.error("请先登录");
                    }
                    queryWrapper.eq("a.author_id", currentUserId);
                    authorIdIgnored = true;
                    break;

                case "published":
                    queryWrapper.eq("a.audit_status", AuditStatus.APPROVED.getCode());
                    auditStatusIgnored = true;
                    break;

                case "pending":
                    if (currentUserId == null) {
                        return R.error("请先登录");
                    }
                    boolean isAdmin = authUtils.isAdmin(currentUserId);
                    if (isAdmin) {
                        queryWrapper.eq("a.audit_status", AuditStatus.PENDING.getCode());
                    } else {
                        queryWrapper.eq("a.author_id", currentUserId)
                                .eq("a.audit_status", AuditStatus.PENDING.getCode());
                        authorIdIgnored = true;
                    }
                    auditStatusIgnored = true;
                    break;

                case "favorite":
                    if (currentUserId == null) {
                        return R.error("请先登录");
                    }
                    List<Interaction> favoriteInteractions = articleService.getUserFavorites(currentUserId);
                    if (favoriteInteractions == null || favoriteInteractions.isEmpty()) {
                        PageUtils pageResult = new PageUtils(new ArrayList<>(), 0, limit, page);
                        return R.ok().put("data", pageResult);
                    }
                    List<Long> favoriteArticleIds = favoriteInteractions.stream()
                            .map(Interaction::getContentId)
                            .collect(Collectors.toList());
                    queryWrapper.in("a.id", favoriteArticleIds)
                            .eq("a.audit_status", AuditStatus.APPROVED.getCode());
                    auditStatusIgnored = true;
                    authorIdIgnored = true;
                    break;

                default:
                    queryWrapper.eq("a.audit_status", AuditStatus.APPROVED.getCode());
                    auditStatusIgnored = true;
            }

            if (!auditStatusIgnored && auditStatus != null) {
                queryWrapper.eq("a.audit_status", auditStatus);
            }
            if (!authorIdIgnored && authorId != null) {
                queryWrapper.eq("a.author_id", authorId);
            }
            if (categoryId != null) {
                queryWrapper.eq("a.category_id", categoryId);
            }

            IPage<ArticleView> articlePage = new Page<>(page, limit);
            IPage<ArticleView> resultPage = articleService.selectListViewPage(articlePage, queryWrapper);

            List<ArticleVO> voList = resultPage.getRecords()
                    .stream()
                    .map(this::convertArticleViewToVO)
                    .collect(Collectors.toList());

            PageUtils pageResult = new PageUtils(resultPage);
            pageResult.setList(voList);

            return R.ok().put("data", pageResult);
        } catch (Exception e) {
            log.error("获取文章分页列表失败", e);
            return R.error("获取数据失败");
        }
    }

    /**
     * 获取文章详情
     */
    @Operation(summary = "获取文章详情", description = "根据文章 ID 查询详细信息，包含作者、分类等信息")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "查询成功"),
            @ApiResponse(responseCode = "404", description = "文章不存在"),
            @ApiResponse(responseCode = "500", description = "服务器错误")
    })
    @GetMapping("/{id}")
    @IgnoreAuth
    public R get(@PathVariable("id") Long id, HttpServletRequest request) {
        try {
            Article article = articleService.getArticleDetail(id);
            if (article == null) {
                return R.error("文章不存在");
            }

            ArticleDetailVO vo;
            if (article instanceof ArticleView view) {
                vo = articleConverter.viewToDetailVO(view);
            } else {
                vo = articleConverter.toDetailVO(article);
            }

            Long currentUserId = sessionUtils.getCurrentUserId(request);
            Boolean isLiked = interactionUtils.hasLiked(currentUserId, id, com.gcs.enums.ContentType.ARTICLE);
            vo.setIsLiked(isLiked != null && isLiked);

            Boolean isDisliked = interactionUtils.hasDisliked(currentUserId, id, com.gcs.enums.ContentType.ARTICLE);
            vo.setIsDisliked(isDisliked != null && isDisliked);

            Boolean isFavorited = interactionUtils.hasFavorited(currentUserId, id, com.gcs.enums.ContentType.ARTICLE);
            vo.setIsFavorited(isFavorited != null && isFavorited);

            vo.setCurrentVersion(article.getCurrentVersion());

            if (article.getCurrentVersion() != null) {
                ArticleVersion currentVersionEntity = articleVersionService.getVersionDetail(
                        id,
                        article.getCurrentVersion()
                );
                if (currentVersionEntity != null) {
                    vo.setMajorVersion(currentVersionEntity.getMajorVersion());
                    vo.setMinorVersion(currentVersionEntity.getMinorVersion());
                } else {
                    vo.setMajorVersion(1);
                    vo.setMinorVersion(0);
                }
            } else {
                List<ArticleVersion> versions = articleVersionService.getVersionHistory(id);
                if (versions != null && !versions.isEmpty()) {
                    ArticleVersion latestVersion = versions.get(0);
                    vo.setMajorVersion(latestVersion.getMajorVersion());
                    vo.setMinorVersion(latestVersion.getMinorVersion());
                } else {
                    vo.setMajorVersion(1);
                    vo.setMinorVersion(0);
                }
            }

            return R.ok().put("data", vo);
        } catch (Exception e) {
            log.error("获取文章详情失败，ID: {}", id, e);
            return R.error("获取数据失败");
        }
    }

    /**
     * 获取推荐文章列表
     */
    @Operation(summary = "获取推荐文章列表", description = "获取所有推荐的文章，按推荐等级排序")
    @GetMapping("/featured")
    @IgnoreAuth
    public R getFeaturedArticles(
            @Parameter(description = "限制数量") @RequestParam(defaultValue = "10") Integer limit,
            HttpServletRequest request) {
        try {
            QueryWrapper<Article> wrapper = new QueryWrapper<>();
            wrapper.eq("is_featured", true)
                    .eq("deleted", false)
                    .eq("audit_status", AuditStatus.APPROVED.getCode())
                    .orderByDesc("featured_level")
                    .orderByDesc("create_time")
                    .last("LIMIT " + limit);

            List<Article> featuredArticles = articleService.list(wrapper);

            List<ArticleVO> voList = featuredArticles.stream()
                    .map(article -> {
                        ArticleVO vo = articleConverter.toVO(article);
                        vo.setIsFeatured(article.getIsFeatured());
                        vo.setFeaturedLevel(article.getFeaturedLevel());
                        return vo;
                    })
                    .collect(Collectors.toList());

            return R.ok().put("data", voList);
        } catch (Exception e) {
            log.error("获取推荐文章列表失败", e);
            return R.error("获取数据失败");
        }
    }

    /**
     * 创建文章
     */
    @Operation(summary = "创建文章", description = "发布新文章")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "发布成功"),
            @ApiResponse(responseCode = "401", description = "未登录"),
            @ApiResponse(responseCode = "500", description = "发布失败")
    })
    @PostMapping
    @Transactional
    public R create(@Valid @RequestBody ArticleCreateDTO createDTO, HttpServletRequest request) {
        try {
            Long userId = sessionUtils.getCurrentUserId(request);
            if (userId == null) {
                return R.error("请先登录");
            }

            Article article = articleConverter.toEntity(createDTO);
            article.setAuthorId(userId);
            article.setAuditStatus(AuditStatus.PENDING);

            articleService.insertArticle(article);

            ArticleDetailVO vo = articleConverter.toDetailVO(
                    articleService.getArticleDetail(article.getId())
            );

            vo.setMajorVersion(1);
            vo.setMinorVersion(0);

            return R.ok("发布成功").put("data", vo);
        } catch (Exception e) {
            log.error("发布文章失败", e);
            return R.error(e.getMessage());
        }
    }

    /**
     * 更新文章
     */
    @Operation(summary = "更新文章", description = "根据文章 ID 更新文章信息")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "更新成功"),
            @ApiResponse(responseCode = "404", description = "文章不存在"),
            @ApiResponse(responseCode = "400", description = "参数验证失败"),
            @ApiResponse(responseCode = "500", description = "服务器错误")
    })
    @PutMapping("/{id}")
    @Transactional
    @Deprecated
    public R update(@PathVariable("id") Long id, @Valid @RequestBody ArticleUpdateDTO updateDTO,
                    HttpServletRequest request) {
        try {
            Long userId = sessionUtils.getCurrentUserId(request);
            if (userId == null) {
                return R.error("请先登录");
            }

            Article originalArticle = articleService.getById(id);
            if (originalArticle == null) {
                return R.error("文章不存在");
            }

            if (!originalArticle.getAuthorId().equals(userId)) {
                return R.error("无权限修改他人文章");
            }

            articleConverter.updateEntity(updateDTO, originalArticle);
            articleService.updateById(originalArticle);

            ArticleDetailVO vo = articleConverter.toDetailVO(
                    articleService.getArticleDetail(id)
            );

            List<ArticleVersion> versions = articleVersionService.getVersionHistory(id);
            if (versions != null && !versions.isEmpty()) {
                ArticleVersion latestVersion = versions.get(0);
                vo.setMajorVersion(latestVersion.getMajorVersion());
                vo.setMinorVersion(latestVersion.getMinorVersion());
            } else {
                vo.setMajorVersion(1);
                vo.setMinorVersion(0);
            }

            return R.ok("更新成功").put("data", vo);
        } catch (Exception e) {
            log.error("更新文章失败，ID: {}", id, e);
            return R.error(e.getMessage());
        }
    }

    /**
     * 部分更新文章
     */
    @PatchMapping("/{id}")
    @Transactional
    public R partialUpdateArticle(@PathVariable("id") Long id, @RequestBody Article article) {
        try {
            article.setId(id);
            articleService.updateById(article);
            return R.ok();
        } catch (Exception e) {
            log.error("部分更新文章失败，ID: {}", id, e);
            return R.error("修改失败");
        }
    }

    /**
     * 批量删除文章
     */
    @Operation(summary = "批量删除文章", description = "批量删除多篇文章（统一接口，自动判断权限）")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "删除成功"),
            @ApiResponse(responseCode = "401", description = "未登录"),
            @ApiResponse(responseCode = "403", description = "无权限删除")
    })
    @PostMapping("/batch-delete")
    @Transactional
    public R batchDeleteArticles(@RequestBody Long[] ids, HttpServletRequest request) {
        try {
            if (ids == null || ids.length == 0) {
                return R.error("请选择要删除的文章");
            }

            Long currentUserId = sessionUtils.getCurrentUserId(request);
            if (currentUserId == null) {
                return R.error("请先登录");
            }

            boolean isAdmin = authUtils.isAdmin(currentUserId);

            for (Long id : ids) {
                Article article = articleService.getById(id);
                if (article == null) {
                    return R.error("文章不存在，ID: " + id);
                }

                if (!isAdmin && !article.getAuthorId().equals(currentUserId)) {
                    return R.error("无权限删除文章，ID: " + id);
                }
            }

            articleService.removeByIds(Arrays.asList(ids));

            if (isAdmin) {
                log.info("管理员批量删除文章成功，数量：{}, 管理员 ID: {}", ids.length, currentUserId);
            } else {
                log.info("作者批量删除文章成功，数量：{}, 作者 ID: {}", ids.length, currentUserId);
            }
            return R.ok();
        } catch (Exception e) {
            log.error("批量删除文章失败", e);
            return R.error("批量删除失败");
        }
    }

    /**
     * 删除文章
     */
    @Operation(summary = "删除文章", description = "根据文章 ID 删除文章（逻辑删除，自动判断状态和权限）")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "删除成功"),
            @ApiResponse(responseCode = "404", description = "文章不存在"),
            @ApiResponse(responseCode = "403", description = "无权限删除"),
            @ApiResponse(responseCode = "500", description = "服务器错误")
    })
    @DeleteMapping("/{id}")
    @Transactional
    public R deleteArticle(@PathVariable("id") Long id, HttpServletRequest request) {
        try {
            Article article = articleService.getById(id);
            if (article == null) {
                return R.error("文章不存在");
            }

            Long currentUserId = sessionUtils.getCurrentUserId(request);
            if (currentUserId == null) {
                return R.error("请先登录");
            }

            boolean isAdmin = authUtils.isAdmin(currentUserId);

            if (isAdmin) {
                articleService.removeById(id);
                log.info("管理员删除文章成功，ID: {}, 管理员 ID: {}", id, currentUserId);
                return R.ok();
            }

            if (!article.getAuthorId().equals(currentUserId)) {
                return R.error("无权限删除此文章");
            }

            articleService.removeById(id);
            log.info("作者删除文章成功，ID: {}, 作者 ID: {}", id, currentUserId);
            return R.ok();
        } catch (Exception e) {
            log.error("删除文章失败，ID: {}", id, e);
            return R.error("删除失败");
        }
    }

    /**
     * 全文搜索文章
     */
    @Operation(summary = "全文搜索文章", description = "基于 PostgreSQL 全文搜索，支持关键词搜索、分类筛选、时间范围筛选")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "搜索成功"),
            @ApiResponse(responseCode = "400", description = "搜索关键词不能为空"),
            @ApiResponse(responseCode = "500", description = "服务器错误")
    })
    @GetMapping("/search")
    @IgnoreAuth
    public R searchArticles(
            @Parameter(description = "搜索关键词（必填）", required = true, example = "Java") @RequestParam(required = false) String keyword,
            @Parameter(description = "分类 ID（可选）", example = "1") @RequestParam(required = false) Long categoryId,
            @Parameter(description = "开始日期 (yyyy-MM-dd)", example = "2025-01-01") @RequestParam(required = false) String startDate,
            @Parameter(description = "结束日期 (yyyy-MM-dd)", example = "2025-12-31") @RequestParam(required = false) String endDate,
            @Parameter(description = "返回结果数量限制", example = "20") @RequestParam(defaultValue = "20") Integer limit,
            HttpServletRequest request) {
        try {
            Map<String, Object> params = new HashMap<>();

            if (keyword != null && !keyword.trim().isEmpty()) {
                params.put("keyword", keyword.trim());
            } else {
                return R.error("搜索关键词不能为空");
            }

            if (categoryId != null) {
                params.put("categoryId", categoryId);
            }

            if (startDate != null && !startDate.isEmpty()) {
                params.put("startDate", startDate);
            }
            if (endDate != null && !endDate.isEmpty()) {
                params.put("endDate", endDate);
            }

            params.put("limit", limit);

            List<ArticleSearchVO> resultList = articleService.searchByFullText(params);

            return R.ok().put("data", resultList);
        } catch (Exception e) {
            log.error("全文搜索失败，keyword: {}", keyword, e);
            return R.error("搜索失败");
        }
    }

    /**
     * 修改文章编辑模式
     */
    @Operation(summary = "修改文章编辑模式", description = "设置文章的编辑模式（仅作者可编辑/所有人可建议）")
    @PutMapping("/{articleId}/edit-mode")
    public R updateEditMode(@PathVariable Long articleId,
                            @RequestParam Integer editMode,
                            HttpServletRequest request) {
        try {
            Long currentUserId = sessionUtils.getCurrentUserId(request);
            if (currentUserId == null) {
                return R.error("请先登录");
            }

            Article article = articleService.getById(articleId);
            if (article == null) {
                return R.error("文章不存在");
            }

            if (!article.getAuthorId().equals(currentUserId)) {
                return R.error("无权限修改编辑模式");
            }

            try {
                EditMode mode = EditMode.valueOfCode(editMode);
            } catch (IllegalArgumentException e) {
                return R.error("无效的编辑模式");
            }

            article.setEditMode(editMode);
            articleService.updateById(article);

            return R.ok("编辑模式更新成功");
        } catch (Exception e) {
            log.error("修改编辑模式失败，articleId: {}", articleId, e);
            return R.error("修改编辑模式失败");
        }
    }

    /**
     * 增加文章浏览量
     */
    @Operation(summary = "增加文章浏览量", description = "增加指定文章的浏览量（带去重机制，同一用户每天只计一次）")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "操作成功"),
            @ApiResponse(responseCode = "404", description = "文章不存在"),
            @ApiResponse(responseCode = "500", description = "服务器错误")
    })
    @PostMapping("/{id}/view")
    @IgnoreAuth
    public R incrementView(@PathVariable("id") Long id, HttpServletRequest request) {
        try {
            Article article = articleService.getById(id);
            if (article == null) {
                return R.error("文章不存在");
            }

            String viewerKey = generateViewerKey(request);
            articleService.incrementViewCount(id, viewerKey);

            return R.ok("浏览量已更新");
        } catch (Exception e) {
            log.error("增加文章浏览量失败，articleId: {}", id, e);
            return R.error("更新浏览量失败");
        }
    }

    /**
     * 将 ArticleView 转换为 ArticleVO
     */
    private ArticleVO convertArticleViewToVO(ArticleView articleView) {
        ArticleVO vo = new ArticleVO();
        vo.setId(articleView.getId());
        vo.setTitle(articleView.getTitle());
        vo.setCoverUrl(articleView.getCoverUrl());
        vo.setCategoryId(articleView.getCategoryId());
        vo.setCategoryName(articleView.getCategoryName());
        vo.setAuthorId(articleView.getAuthorId());
        vo.setAuthorNickname(articleView.getAuthorNickname());
        vo.setAuthorAvatar(articleView.getAuthorAvatar());

        vo.setLikeCount(articleView.getLikeCount());
        vo.setDislikeCount(articleView.getDislikeCount());
        vo.setFavoriteCount(articleView.getFavoriteCount());
        vo.setCommentCount(articleView.getCommentCount());
        vo.setShareCount(articleView.getShareCount());
        vo.setViewCount(articleView.getViewCount());

        vo.setPublishTime(articleView.getPublishTime() != null ? articleView.getPublishTime() : java.util.Date.from(articleView.getCreateTime().atZone(java.time.ZoneId.systemDefault()).toInstant()));

        vo.setAuditStatus(articleView.getAuditStatus().getCode());
        vo.setCurrentVersion(articleView.getCurrentVersion());
        vo.setCreateTime(articleView.getCreateTime());

        vo.setIsFeatured(articleView.getIsFeatured());
        vo.setFeaturedLevel(articleView.getFeaturedLevel());

        return vo;
    }

    /**
     * 生成访问者标识
     */
    private String generateViewerKey(HttpServletRequest request) {
        Long userId = sessionUtils.getCurrentUserId(request);
        if (userId != null) {
            return "user:" + userId;
        }

        String ip = request.getRemoteAddr();
        String userAgent = request.getHeader("User-Agent");
        return "ip:" + ip + ":" + (userAgent != null ? userAgent.hashCode() : "unknown");
    }
}
