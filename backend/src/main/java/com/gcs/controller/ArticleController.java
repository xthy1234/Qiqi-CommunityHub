package com.gcs.controller;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.gcs.annotation.IgnoreAuth;
import com.gcs.converter.ArticleConverter;
import com.gcs.entity.ArticleVersion;
import com.gcs.entity.User;
import com.gcs.entity.view.ArticleView;
import com.gcs.enums.AuditStatus;
import com.gcs.service.UserService;
import com.gcs.utils.Query;
import com.gcs.vo.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.gcs.dto.ArticleCreateDTO;
import com.gcs.dto.ArticleDraftDTO;
import com.gcs.dto.ArticleUpdateDTO;
import com.gcs.entity.Article;
import com.gcs.entity.Interaction;
import com.gcs.enums.ContentType;
import com.gcs.enums.InteractionActionType;
import com.gcs.service.ArticleService;
import com.gcs.service.InteractionService;
import com.gcs.utils.PageUtils;
import com.gcs.utils.R;
import com.gcs.utils.MPUtil;
import com.gcs.vo.ArticleSearchVO;

import com.gcs.enums.EditMode;
import com.gcs.service.ArticleVersionService;
import com.gcs.service.ArticleContributorService;

import lombok.extern.slf4j.Slf4j;

/**
 * 帖子控制器
 * 提供帖子相关的 RESTful API 接口
 * @author 111
 * @email
 * @date
 */
@Slf4j
@Tag(name = "帖子管理", description = "帖子相关的 RESTful API 接口")
@RestController
@RequestMapping("/articles")
public class ArticleController {
    
    @Autowired
    private ArticleService articleService;
    
    @Autowired
    private ArticleVersionService articleVersionService;

    @Autowired
   private UserService userService;

    @Autowired
   private InteractionService favoriteService;

    @Autowired
    private ArticleContributorService articleContributorService;


    @Autowired
    private ArticleConverter articleConverter;

    /**
     * 获取文章分页列表（通用接口）
     * 支持参数：
     * - page=1&limit=6&sort=createTime&order=desc（分页排序）
     * - auditStatus=1（审核状态：0-待审核，1:已通过，2:已拒绝）
     * - authorId=1（作者 ID）
     * - categoryId=1（分类 ID）
     * - type=my（我的文章）/published（已发布）/favorite（我的收藏）/all（全部，默认）
     *
     * 参数优先级说明：
     * - type 参数决定核心查询条件，某些 type 值会忽略 auditStatus 和 authorId 参数
     * - categoryId 可与任意 type 组合使用
     *
     * 注意：草稿文章现在存储在 article_draft 表中，不再使用 audit_status=3
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
            @Parameter(description = "审核状态 (0:待审核，1:已通过，2:已拒绝)") @RequestParam(required = false) Integer auditStatus,
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

            // 构建查询条件
            QueryWrapper<Article> queryWrapper = new QueryWrapper<>();

            // 1. 基础条件：逻辑删除
            queryWrapper.eq("deleted", false);

            // 2. 获取当前用户 ID
            Long currentUserId = getCurrentUserId(request);
            System.out.println("currentUserId: " + currentUserId);
            // 标记哪些参数应该被忽略
            boolean auditStatusIgnored = false;
            boolean authorIdIgnored = false;

            // 3. 根据 type 设置核心条件（所有字段都使用表别名 a.）
            switch (type) {
                case "all":
                    // 所有公开文章（审核通过的）
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
                    // 所有已发布文章
                    queryWrapper.eq("a.audit_status", AuditStatus.APPROVED.getCode());
                    auditStatusIgnored = true;
                    break;

                case "pending":
                    // 待审核文章（管理员看全部，用户看自己的）
                    if (currentUserId == null) {
                        return R.error("请先登录");
                    }
                    boolean isAdmin = checkIsAdmin(currentUserId);
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
                    // 当前用户收藏的已发布文章
                    if (currentUserId == null) {
                        return R.error("请先登录");
                    }

                    // 从 interaction 表查询当前用户收藏的文章 ID 列表
                    List<Interaction> favoriteInteractions = favoriteService.getUserInteractionsList(
                            currentUserId,
                            InteractionActionType.FAVORITE,
                            ContentType.ARTICLE
                    );

                    if (favoriteInteractions == null || favoriteInteractions.isEmpty()) {
                        // 如果没有收藏任何文章，返回空列表
                        PageUtils pageResult = new PageUtils(new ArrayList<>(), 0, limit, page);
                        return R.ok().put("data", pageResult);
                    }

                    // 提取收藏的文章 ID
                    List<Long> favoriteArticleIds = favoriteInteractions.stream()
                            .map(Interaction::getContentId)
                            .collect(Collectors.toList());

                    // 查询这些文章（只查询已发布的）
                    queryWrapper.in("a.id", favoriteArticleIds)
                            .eq("a.audit_status", AuditStatus.APPROVED.getCode());
                    auditStatusIgnored = true;
                    authorIdIgnored = true;
                    break;

                default:
                    // 默认作为 all 处理
                    queryWrapper.eq("a.audit_status", AuditStatus.APPROVED.getCode());
                    auditStatusIgnored = true;
            }

            // 4. 处理未被忽略的额外筛选条件（所有字段都使用表别名 a.）
            if (!auditStatusIgnored && auditStatus != null) {
                queryWrapper.eq("a.audit_status", auditStatus);
            }
            if (!authorIdIgnored && authorId != null) {
                queryWrapper.eq("a.author_id", authorId);
            }
            if (categoryId != null) {
                queryWrapper.eq("a.category_id", categoryId);
            }

            // 5. 执行分页查询（使用 selectListView 来获取联表数据）
            IPage<ArticleView> articlePage = new Query<ArticleView>(params).getPage();
            IPage<ArticleView> resultPage = articleService.selectListViewPage(articlePage, queryWrapper);

            // 6. 将 ArticleView 转换为 ArticleVO
            List<ArticleVO> voList = resultPage.getRecords()
                    .stream()
                    .map(this::convertArticleViewToVO)
                    .collect(Collectors.toList());

            // 7. 构建返回结果
            PageUtils pageResult = new PageUtils(resultPage);
            pageResult.setList(voList);

            return R.ok().put("data", pageResult);
        } catch (Exception e) {
            log.error("获取文章分页列表失败", e);
            return R.error("获取数据失败");
        }
    }


    /**
     * 获取帖子详情
     */
    @Operation(summary = "获取帖子详情", description = "根据帖子 ID 查询详细信息，包含作者、分类等信息")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "查询成功"),
            @ApiResponse(responseCode = "404", description = "帖子不存在"),
            @ApiResponse(responseCode = "500", description = "服务器错误")
    })
    @GetMapping("/{id}")
    @IgnoreAuth
    public R get(@PathVariable("id") Long id, HttpServletRequest request) {
        try {
            // ✅ Service 只负责返回 Entity
            Article article = articleService.getArticleDetail(id);
            if (article == null) {
                return R.error("文章不存在");
            }

            // ✅ Controller 负责转换为 VO
            ArticleDetailVO vo;
            if (article instanceof ArticleView view) {
                // 使用 MapStruct 转换（自动处理扩展字段）
                vo = articleConverter.viewToDetailVO(view);
            } else {
                vo = articleConverter.toDetailVO(article);
            }

            // ✅ Controller 负责补充业务字段（点赞、收藏状态）
            Long currentUserId = getCurrentUserId(request);
            Boolean isLiked = checkIsLiked(currentUserId, id);
            vo.setIsLiked(isLiked != null && isLiked);

            Boolean isFavorited = checkIsFavorited(currentUserId, id);
            vo.setIsFavorited(isFavorited != null && isFavorited);
            
            // ✅ 新增：从 article 实体获取 currentVersion
            vo.setCurrentVersion(article.getCurrentVersion());
            
            // ✅ 查询最新版本并设置 majorVersion、minorVersion
            List<ArticleVersion> versions = articleVersionService.getVersionHistory(id);
            if (versions != null && !versions.isEmpty()) {
                ArticleVersion latestVersion = versions.get(0);
                vo.setMajorVersion(latestVersion.getMajorVersion());
                vo.setMinorVersion(latestVersion.getMinorVersion());
            } else {
                // 如果没有版本记录，默认为 1.0
                vo.setMajorVersion(1);
                vo.setMinorVersion(0);
            }

            return R.ok().put("data", vo);
        } catch (Exception e) {
            log.error("获取文章详情失败，ID: {}", id, e);
            return R.error("获取数据失败");
        }
    }

    /**
     * 创建文章
     */
    @PostMapping
    @Transactional
    public R create(@Valid @RequestBody ArticleCreateDTO createDTO, HttpServletRequest request) {
        try {
            Long userId = getCurrentUserId(request);
            if (userId == null) {
                return R.error("请先登录");
            }

            // ✅ 使用 Converter 转换 DTO → Entity
            Article article = articleConverter.toEntity(createDTO);
            article.setAuthorId(userId);
            article.setAuditStatus(AuditStatus.PENDING);

            // ✅ Service 负责保存
            articleService.insertArticle(article);

            // ✅ Controller 负责转换为 VO 并返回
            ArticleDetailVO vo = articleConverter.toDetailVO(
                    articleService.getArticleDetail(article.getId())
            );
            
            // ✅ 新增：查询最新版本并设置版本号（新文章第一个版本为 1.0）
            vo.setMajorVersion(1);
            vo.setMinorVersion(0);

            return R.ok("发布成功").put("data", vo);
        } catch (Exception e) {
            log.error("发布文章失败", e);
            return R.error(e.getMessage());
        }
    }

    /**
     * 批量审核帖子
     */
    @PostMapping("/batch-audit")
    @Transactional
    public R batchAudit(@RequestBody Long[] ids,
                        @RequestParam Integer status,
                        @RequestParam(required = false) String reply) {
        try {
            AuditStatus auditStatus = AuditStatus.valueOf(status);

            List<Article> articles = new ArrayList<>();
            for (Long id : ids) {
                Article article = articleService.getById(id);
                if (article != null) {
                    article.setAuditStatus(auditStatus);
                    article.setAuditReply(reply);

                    // 审核通过时，如果 publishTime 为空，则设置为当前时间
                    if (auditStatus == AuditStatus.APPROVED && article.getPublishTime() == null) {
                        article.setPublishTime(new Date());
                    }

                    articles.add(article);
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
     * 更新帖子
     */
    @Operation(summary = "更新帖子", description = "根据帖子 ID 更新帖子信息")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "更新成功"),
            @ApiResponse(responseCode = "404", description = "帖子不存在"),
            @ApiResponse(responseCode = "400", description = "参数验证失败"),
            @ApiResponse(responseCode = "500", description = "服务器错误")
    })
    @PutMapping("/{id}")
    @Transactional
    @Deprecated
    public R update(@PathVariable("id") Long id, @Valid @RequestBody ArticleUpdateDTO updateDTO,
                    HttpServletRequest request) {
        try {
            Long userId = getCurrentUserId(request);
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

            // ✅ 使用 Converter 更新 Entity 字段
            articleConverter.updateEntity(updateDTO, originalArticle);

            // ✅ Service 负责保存
            articleService.updateById(originalArticle);

            // ✅ Controller 负责转换为 VO 并返回
            ArticleDetailVO vo = articleConverter.toDetailVO(
                    articleService.getArticleDetail(id)
            );
            
            // ✅ 新增：查询最新版本并设置版本号
            List<ArticleVersion> versions = articleVersionService.getVersionHistory(id);
            if (versions != null && !versions.isEmpty()) {
                ArticleVersion latestVersion = versions.get(0);
                vo.setMajorVersion(latestVersion.getMajorVersion());
                vo.setMinorVersion(latestVersion.getMinorVersion());
            } else {
                // 如果没有版本记录，默认为 1.0
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
     * 部分更新帖子
     */
    @PatchMapping("/{id}")
    @Transactional
    public R partialUpdateArticle(@PathVariable("id") Long id, @RequestBody Article article) {
        try {
            article.setId(id);
            articleService.updateById(article);
            return R.ok();
        } catch (Exception e) {
            log.error("部分更新帖子失败，ID: {}", id, e);
            return R.error("修改失败");
        }
    }
    /**
     * 批量删除文章（统一接口）
     * 自动判断每篇文章的状态和权限
     */
    @PostMapping("/batch-delete")
    @Transactional
    public R batchDeleteArticles(@RequestBody Long[] ids, HttpServletRequest request) {
        try {
            if (ids == null || ids.length == 0) {
                return R.error("请选择要删除的文章");
            }
            
            // 获取当前用户信息
            String userIdStr = getSessionAttribute(request, "userId");
            if (userIdStr == null) {
                return R.error("请先登录");
            }
            
            Long currentUserId = Long.parseLong(userIdStr);
            
            // 判断是否为管理员
            boolean isAdmin = checkIsAdmin(currentUserId);
            
            // 验证所有文章
            for (Long id : ids) {
                Article article = articleService.getById(id);
                if (article == null) {
                    return R.error("文章不存在，ID: " + id);
                }
                
                // 如果不是管理员，验证是否为作者本人
                if (!isAdmin && !article.getAuthorId().equals(currentUserId)) {
                    return R.error("无权限删除文章，ID: " + id);
                }
            }
            
            // 批量删除
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
     * 删除文章（统一接口）
     * 自动判断文章状态和权限
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
            
            // 获取当前用户信息
            String userIdStr = getSessionAttribute(request, "userId");
            if (userIdStr == null) {
                return R.error("请先登录");
            }
            
            Long currentUserId = Long.parseLong(userIdStr);
            
            // 判断是否为管理员
            boolean isAdmin = checkIsAdmin(currentUserId);
            
            // 管理员可以直接删除任何文章
            if (isAdmin) {
                articleService.removeById(id);
                log.info("管理员删除文章成功，ID: {}, 管理员 ID: {}", id, currentUserId);
                return R.ok();
            }
            
            // 非管理员只能删除自己的文章
            if (!article.getAuthorId().equals(currentUserId)) {
                return R.error("无权限删除此文章");
            }
            
            // 作者删除自己的文章
            articleService.removeById(id);
            log.info("作者删除文章成功，ID: {}, 作者 ID: {}", id, currentUserId);
            return R.ok();
        } catch (Exception e) {
            log.error("删除文章失败，ID: {}", id, e);
            return R.error("删除失败");
        }
    }

    /**
     * 按值统计
     */
    @GetMapping("/stats/value/{xColumn}/{yColumn}")
    public R valueStatistics(@PathVariable("yColumn") String yColumn, 
                             @PathVariable("xColumn") String xColumn,
                             HttpServletRequest request) {
        try {
            Map<String, Object> params = createStatsParams(xColumn, yColumn);
            QueryWrapper<Article> queryWrapper = buildStatsQueryWrapper(request);
            
            List<Map<String, Object>> result = articleService.selectValue(params, queryWrapper);
            formatDatesInResult(result);
            
            return R.ok().put("data", result);
        } catch (Exception e) {
            log.error("统计查询失败", e);
            return R.error("统计失败");
        }
    }

    /**
     * 按值统计（多列）
     */
    @GetMapping("/stats/value/multiple/{xColumn}")
    public R multipleValueStatistics(@PathVariable("xColumn") String xColumn,
                                     @RequestParam String yColumns,
                                     HttpServletRequest request) {
        try {
            String[] yColumnArray = yColumns.split(",");
            List<List<Map<String, Object>>> results = new ArrayList<>();
            
            QueryWrapper<Article> queryWrapper = buildStatsQueryWrapper(request);
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
            
            for (String yColumn : yColumnArray) {
                Map<String, Object> params = createStatsParams(xColumn, yColumn);
                List<Map<String, Object>> result = articleService.selectValue(params, queryWrapper);
                formatDatesInResult(result);
                results.add(result);
            }
            
            return R.ok().put("data", results);
        } catch (Exception e) {
            log.error("多列统计查询失败", e);
            return R.error("统计失败");
        }
    }

    /**
     * 时间统计
     */
    @GetMapping("/stats/time/{xColumn}/{yColumn}/{timeType}")
    public R timeStatistics(@PathVariable("yColumn") String yColumn, 
                            @PathVariable("xColumn") String xColumn,
                            @PathVariable("timeType") String timeType,
                            HttpServletRequest request) {
        try {
            Map<String, Object> params = createTimeStatsParams(xColumn, yColumn, timeType);
            QueryWrapper<Article> queryWrapper = buildStatsQueryWrapper(request);
            
            List<Map<String, Object>> result = articleService.selectTimeStatValue(params, queryWrapper);
            formatDatesInResult(result);
            
            return R.ok().put("data", result);
        } catch (Exception e) {
            log.error("时间统计查询失败", e);
            return R.error("统计失败");
        }
    }

    /**
     * 分组统计
     */
    @GetMapping("/stats/group/{column}")
    public R groupStatistics(@PathVariable("column") String column,
                             HttpServletRequest request) {
        try {
            Map<String, Object> params = new HashMap<>();
            params.put("column", column);
            
            QueryWrapper<Article> queryWrapper = buildStatsQueryWrapper(request);
            List<Map<String, Object>> result = articleService.selectGroup(params, queryWrapper);
            formatDatesInResult(result);
            
            return R.ok().put("data", result);
        } catch (Exception e) {
            log.error("分组统计查询失败", e);
            return R.error("统计失败");
        }
    }

    /**
     * 获取帖子总数
     */
    @GetMapping("/count")
    public R getCount(@RequestParam Map<String, Object> params, 
                      Article article, 
                      HttpServletRequest request) {
        try {

            String authorIdStr = getSessionAttribute(request, "userId");
           article.setAuthorId(authorIdStr != null ? Long.parseLong(authorIdStr) : null);
            
            QueryWrapper<Article> queryWrapper = buildQueryWrapper(article, params);
            Long count = articleService.count(MPUtil.sort(
                MPUtil.between(MPUtil.likeOrEq(queryWrapper, article), params), params));
                
            return R.ok().put("data", count);
        } catch (Exception e) {
            log.error("获取帖子总数失败", e);
            return R.error("查询失败");
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
            
            // 处理关键词（必需）
            if (keyword != null && !keyword.trim().isEmpty()) {
                params.put("keyword", keyword.trim());
            } else {
                return R.error("搜索关键词不能为空");
            }
            
            // 可选的分类筛选
            if (categoryId != null) {
                params.put("categoryId", categoryId);
            }
            
            // 可选的时间范围筛选
            if (startDate != null && !startDate.isEmpty()) {
                params.put("startDate", startDate);
            }
            if (endDate != null && !endDate.isEmpty()) {
                params.put("endDate", endDate);
            }
            
            // 设置默认限制
            params.put("limit", limit);
            
            List<ArticleSearchVO> resultList = articleService.searchByFullText(params);
            
            return R.ok().put("data", resultList);
        } catch (Exception e) {
            log.error("全文搜索失败，keyword: {}", keyword, e);
            return R.error("搜索失败");
        }
    }

    // ==================== 私有辅助方法 ====================

    /**
     * 获取当前登录用户 ID
     * @param request HTTP 请求
     * @return 用户 ID，未登录则返回 null
     */
    private Long getCurrentUserId(HttpServletRequest request) {
        String userIdStr = getSessionAttribute(request, "userId");
        return userIdStr != null ? Long.parseLong(userIdStr) : null;
    }

    /**
     * 检查是否已点赞
     */
    private Boolean checkIsLiked(Long userId, Long articleId) {
        if (userId == null || articleId == null) {
            return false;
        }
        
        try {
            List<Interaction> interactions = favoriteService.getUserInteractionsList(
                userId, 
                InteractionActionType.LIKE,
                ContentType.ARTICLE
            );
            
            if (interactions == null || interactions.isEmpty()) {
                return false;
            }
            
            return interactions.stream()
                .anyMatch(i -> i.getContentId().equals(articleId));
        } catch (Exception e) {
            log.error("检查点赞状态失败，userId: {}, articleId: {}", userId, articleId, e);
            return false;
        }
    }
    
    /**
     * 检查是否已收藏
     */
    private Boolean checkIsFavorited(Long userId, Long articleId) {
        if (userId == null || articleId == null) {
            return false;
        }
        
        try {
            List<Interaction> interactions = favoriteService.getUserInteractionsList(
                userId, 
                InteractionActionType.FAVORITE,
                ContentType.ARTICLE
            );
            
            if (interactions == null || interactions.isEmpty()) {
                return false;
            }
            
            return interactions.stream()
                .anyMatch(i -> i.getContentId().equals(articleId));
        } catch (Exception e) {
            log.error("检查收藏状态失败，userId: {}, articleId: {}", userId, articleId, e);
            return false;
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
            Long currentUserId = getCurrentUserId(request);
            if (currentUserId == null) {
                return R.error("请先登录");
            }

            Article article = articleService.getById(articleId);
            if (article == null) {
                return R.error("文章不存在");
            }

            // 验证权限（只有作者可以修改编辑模式）
            if (!article.getAuthorId().equals(currentUserId)) {
                return R.error("无权限修改编辑模式");
            }

            // 验证编辑模式合法性
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
     * 将 ArticleView 转换为 ArticleVO（包含联表查询的分类名、作者信息）
     */
    private ArticleVO convertArticleViewToVO(ArticleView articleView) {
        ArticleVO vo = new ArticleVO();
        vo.setId(articleView.getId());
        vo.setTitle(articleView.getTitle());
        vo.setCoverUrl(articleView.getCoverUrl());
        vo.setCategoryId(articleView.getCategoryId());
        vo.setCategoryName(articleView.getCategoryName());  // ✅ 直接使用联表查询的结果
        vo.setAuthorId(articleView.getAuthorId());
        vo.setAuthorNickname(articleView.getAuthorNickname());  // ✅ 直接使用联表查询的结果
        vo.setAuthorAvatar(articleView.getAuthorAvatar());  // ✅ 直接使用联表查询的结果
        
        vo.setLikeCount(articleView.getLikeCount());
        vo.setDislikeCount(articleView.getDislikeCount());
        vo.setViewCount(articleView.getViewCount());
        
        // 如果 publishTime 为空，则使用 createTime
        vo.setPublishTime(articleView.getPublishTime() != null ? articleView.getPublishTime() : java.util.Date.from(articleView.getCreateTime().atZone(java.time.ZoneId.systemDefault()).toInstant()));
        
        vo.setAuditStatus(articleView.getAuditStatus().getCode());
        vo.setCreateTime(articleView.getCreateTime());
        return vo;
    }


    /**
     * 构建查询条件
     */
    private QueryWrapper<Article> buildQueryWrapper(Article article, Map<String, Object> params) {
        QueryWrapper<Article> queryWrapper = new QueryWrapper<>();
        // 使用 columnMap 方式，MyBatis-Plus 会自动处理表别名和逻辑删除
        return queryWrapper;
    }

    /**
     * 构建统计查询条件
     */
    private QueryWrapper<Article> buildStatsQueryWrapper(HttpServletRequest request) {
        QueryWrapper<Article> queryWrapper = new QueryWrapper<>();
        return queryWrapper;
    }

    /**
     * 创建统计参数
     */
    private Map<String, Object> createStatsParams(String xColumn, String yColumn) {
        Map<String, Object> params = new HashMap<>();
        params.put("xColumn", xColumn);
        params.put("yColumn", yColumn);
        return params;
    }

    /**
     * 创建时间统计参数
     */
    private Map<String, Object> createTimeStatsParams(String xColumn, String yColumn, String timeType) {
        Map<String, Object> params = createStatsParams(xColumn, yColumn);
        params.put("timeStatType", timeType);
        return params;
    }

    /**
     * 格式化结果中的日期
     */
    private void formatDatesInResult(List<Map<String, Object>> result) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        for (Map<String, Object> item : result) {
            for (Map.Entry<String, Object> entry : item.entrySet()) {
                if (entry.getValue() instanceof Date) {
                    entry.setValue(dateFormat.format((Date) entry.getValue()));
                }
            }
        }
    }

    /**
     * 获取会话属性
     */
    private String getSessionAttribute(HttpServletRequest request, String attributeName) {
        Object attribute = request.getSession().getAttribute(attributeName);
        return attribute != null ? attribute.toString() : null;
    }

    /**
     * 检查用户是否为管理员
     */
    private boolean checkIsAdmin(Long userId) {
        try {
            User user = userService.getById(userId);
            if (user == null) {
                return false;
            }
            
            // 方法 1: 通过 roleId 判断（假设 roleId=2 为管理员）
            return user.getRoleId() != null && user.getRoleId() == 2L;
        } catch (Exception e) {
            log.error("检查管理员权限失败，userId: {}", userId, e);
            return false;
        }
    }

}
