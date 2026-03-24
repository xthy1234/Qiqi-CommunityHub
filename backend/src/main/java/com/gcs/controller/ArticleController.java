package com.gcs.controller;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.gcs.annotation.IgnoreAuth;
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
import com.gcs.enums.AuditStatus;
import com.gcs.enums.ContentType;
import com.gcs.enums.InteractionActionType;
import com.gcs.service.ArticleService;
import com.gcs.service.InteractionService;
import com.gcs.utils.PageUtils;
import com.gcs.utils.R;
import com.gcs.utils.MPUtil;
import com.gcs.vo.ArticleSearchVO;

import com.gcs.dto.SuggestionSubmitDTO;
import com.gcs.dto.SuggestionReviewDTO;
import com.gcs.entity.ArticleEditSuggestion;
import com.gcs.entity.ArticleVersion;
import com.gcs.enums.EditMode;
import com.gcs.service.ArticleVersionService;
import com.gcs.service.ArticleContributorService;
import com.gcs.service.ArticleEditSuggestionService;
import com.gcs.vo.ArticleVersionVO;
import com.gcs.vo.VersionCompareResultVO;
import com.gcs.vo.ArticleSuggestionVO;
import com.gcs.vo.ContributorVO;

import lombok.extern.slf4j.Slf4j;

/**
 * 帖子控制器
 * 提供帖子相关的 RESTful API 接口
 * @author 
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
   private UserService userService;

    @Autowired
   private InteractionService favoriteService;

    @Autowired
    private ArticleVersionService articleVersionService;

    @Autowired
    private ArticleContributorService articleContributorService;

    @Autowired
    private ArticleEditSuggestionService articleEditSuggestionService;

    /**
     * 获取文章分页列表（通用接口）
     * 支持参数：
     * - page=1&limit=6&sort=createTime&order=desc（分页排序）
     * - auditStatus=1（审核状态：0-待审核，1:已通过，2:已拒绝，3:草稿）
     * - authorId=1（作者 ID）
     * - categoryId=1（分类 ID）
     * - type=my（我的文章）/draft（草稿）/published（已发布）/favorite（我的收藏）/all（全部，默认）
     * 
     * 参数优先级说明：
     * - type 参数决定核心查询条件，某些 type 值会忽略 auditStatus 和 authorId 参数
     * - categoryId 可与任意 type 组合使用
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
        @Parameter(description = "审核状态 (0:待审核，1:已通过，2:已拒绝，3:草稿)") @RequestParam(required = false) Integer auditStatus,
        @Parameter(description = "作者 ID") @RequestParam(required = false) Long authorId,
        @Parameter(description = "分类 ID") @RequestParam(required = false) Long categoryId,
        @Parameter(description = "查询类型 (all/draft/published/my/favorite/pending)") @RequestParam(defaultValue = "all") String type,
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
                    // 当前用户的文章（所有状态）
                    if (currentUserId == null) {
                        return R.error("请先登录");
                    }
                    queryWrapper.eq("a.author_id", currentUserId);
                    authorIdIgnored = true;
                    break;
                    
                case "draft":
                    // 当前用户的草稿
                    if (currentUserId == null) {
                        return R.error("请先登录");
                    }
                    queryWrapper.eq("a.author_id", currentUserId)
                               .eq("a.audit_status", AuditStatus.DRAFT.getCode());
                    auditStatusIgnored = true;
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
    public R getArticleInfo(@PathVariable("id") Long id) {
        try {
            Article article = updateViewCount(id);
            // 将 Article 转换为 ArticleDetailVO
            ArticleDetailVO vo = convertToDetailVO(article);
            return R.ok().put("data", vo);
        } catch (Exception e) {
            log.error("获取帖子详情失败，ID: {}", id, e);
            return R.error("获取详情失败");
        }
    }
    
    /**
     * 创建文章草稿（简化版，只需要标题和内容，必须登录）
     */
    @Operation(summary = "创建文章草稿", description = "创建草稿文章，只需要标题和内容，其他信息可后续完善。需要登录后使用。")
    @PostMapping("/draft")
    public R createDraft(@Valid @RequestBody ArticleDraftDTO draftDTO, 
                        HttpServletRequest request) {
        try {
            // 从 Session 获取作者 ID（必须登录）
            String authorIdStr = getSessionAttribute(request, "userId");
            if (authorIdStr == null) {
                return R.error("请先登录后再发布文章");
            }
            
            Article article = convertToEntityFromDraft(draftDTO);
            
            // 设置作者 ID
            article.setAuthorId(Long.parseLong(authorIdStr));
            
            prepareArticleForSave(article);
            // 设置为待审核状态（0）
            article.setAuditStatus(AuditStatus.PENDING);
            // 设置默认发布时间为当前时间
            if (article.getPublishTime() == null) {
                article.setPublishTime(new Date());
            }
            
            articleService.save(article);
            
            log.info("草稿创建成功，ID: {}, 作者 ID: {}", article.getId(), article.getAuthorId());
            return R.ok().put("id", article.getId());
        } catch (Exception e) {
            log.error("创建草稿失败", e);
            return R.error("创建草稿失败：" + e.getMessage());
        }
    }

    /**
     * 更新文章草稿（完善草稿信息）
     */
    @Operation(summary = "更新文章草稿", description = "完善草稿文章的信息，可以更新标题、内容、分类等")
    @PutMapping("/draft/{id}")
    @Transactional
    public R updateDraft(@PathVariable("id") Long id, 
                        @Valid @RequestBody ArticleDraftDTO draftDTO,
                        HttpServletRequest request) {
        try {
            Article article = articleService.getById(id);
            if (article == null) {
                return R.error("文章不存在");
            }
            
            // 验证作者权限（只有作者本人可以修改）
            String authorIdStr = getSessionAttribute(request, "userId");
            if (authorIdStr == null || !article.getAuthorId().equals(Long.parseLong(authorIdStr))) {
                return R.error("无权限修改此文章");
            }
            
            // 更新字段
            article.setTitle(draftDTO.getTitle());
            article.setContent(draftDTO.getContent());
            article.setCoverUrl(draftDTO.getCoverUrl());
            article.setCategoryId(draftDTO.getCategoryId());
            article.setAttachment(draftDTO.getAttachment());
            // 保持草稿状态
            article.setAuditStatus(AuditStatus.DRAFT);
            
            articleService.updateById(article);
            
            log.info("草稿更新成功，ID: {}", id);
            return R.ok();
        } catch (Exception e) {
            log.error("更新草稿失败，ID: {}", id, e);
            return R.error("更新草稿失败");
        }
    }

    /**
     * 提交草稿审核（将草稿状态改为待审核）
     */
    @Operation(summary = "提交草稿审核", description = "将草稿文章提交审核，审核后对外可见")
    @PostMapping("/draft/{id}/submit")
    @Transactional
    public R submitDraft(@PathVariable("id") Long id,
                        HttpServletRequest request) {
        try {
            Article article = articleService.getById(id);
            if (article == null) {
                return R.error("文章不存在");
            }
            
            // 验证作者权限
            String authorIdStr = getSessionAttribute(request, "userId");
            if (authorIdStr == null || !article.getAuthorId().equals(Long.parseLong(authorIdStr))) {
                return R.error("无权限操作此文章");
            }
            
            // 检查是否已提交过
            if (article.getAuditStatus() != null && article.getAuditStatus() != AuditStatus.PENDING) {
                return R.error("文章已审核，无法重复提交");
            }
            
            // 设置为待审核状态
            article.setAuditStatus(AuditStatus.PENDING);
            articleService.updateById(article);
            
            log.info("草稿提交审核成功，ID: {}", id);
            return R.ok();
        } catch (Exception e) {
            log.error("提交草稿审核失败，ID: {}", id, e);
            return R.error("提交审核失败");
        }
    }

    /**
     * 根据 ID 获取草稿详情
     */
    @GetMapping("/draft/{id}")
    public R getDraftInfo(@PathVariable("id") Long id,
                         HttpServletRequest request) {
        try {
            ArticleView articleView = articleService.selectViewById(id);
            if (articleView == null) {
                return R.error("文章不存在");
            }
            
            // 验证作者权限
            String authorIdStr = getSessionAttribute(request, "userId");
            if (authorIdStr == null || !articleView.getAuthorId().equals(Long.parseLong(authorIdStr))) {
                return R.error("无权限查看此文章");
            }
            
            // 转换为草稿 VO（包含 categoryName，不包含 publishTime 等无关字段）
            ArticleDraftVO vo = convertToDraftVO(articleView);
            return R.ok().put("data", vo);
        } catch (Exception e) {
            log.error("获取草稿详情失败，ID: {}", id, e);
            return R.error("获取详情失败");
        }
    }

    /**
     * 创建文章
     */
    @PostMapping
    public R createArticle(@Valid @RequestBody ArticleCreateDTO createDTO, 
                         HttpServletRequest request) {
        try {
            // 从 Session 获取作者 ID（必须登录）
            String authorIdStr = getSessionAttribute(request, "userId");
            if (authorIdStr == null) {
                return R.error("请先登录后再发布文章");
            }
            
            Article article = convertToEntity(createDTO);
            
            // 设置作者 ID
            article.setAuthorId(Long.parseLong(authorIdStr));
            
            prepareArticleForSave(article);
            // 设置默认发布时间为当前时间
            if (article.getPublishTime() == null) {
                article.setPublishTime(new Date());
            }
            
            articleService.save(article);
            
            log.info("文章创建成功，ID: {}, 作者 ID: {}", article.getId(), article.getAuthorId());
            return R.ok().put("id", article.getId());
        } catch (Exception e) {
            log.error("保存帖子失败", e);
            return R.error("保存失败");
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
    public R updateArticle(
        @Parameter(description = "帖子 ID", required = true, example = "1") @PathVariable("id") Long id, 
        @Parameter(description = "帖子信息", required = true) @Valid @RequestBody ArticleUpdateDTO updateDTO) {
        try {
            Article article = articleService.getById(id);
            if (article == null) {
                return R.error("帖子不存在");
            }
            
            // 使用转换器更新实体
            convertToUpdateEntity(updateDTO, article);
            articleService.updateById(article);
            return R.ok();
        } catch (Exception e) {
            log.error("修改帖子失败，ID: {}", id, e);
            return R.error("修改失败");
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










    // ... existing code ...


// ==================== 版本控制相关接口 ====================

/**
 * 获取文章版本列表
 */
@Operation(summary = "获取文章版本列表", description = "查询文章的所有历史版本")
@GetMapping("/{articleId}/versions")
public R getVersionList(@PathVariable Long articleId,
                        @RequestParam(defaultValue = "1") Integer page,
                        @RequestParam(defaultValue = "10") Integer limit) {
    try {
        List<ArticleVersion> versions = articleVersionService.getVersionHistory(articleId);

        List<ArticleVersionVO> voList = versions.stream()
                .map(this::convertToVersionVO)
                .collect(Collectors.toList());

        return R.ok().put("data", voList);
    } catch (Exception e) {
        log.error("获取版本列表失败，articleId: {}", articleId, e);
        return R.error("获取版本列表失败");
    }
}

/**
 * 获取指定版本详情
 */
@Operation(summary = "获取指定版本详情", description = "查询文章的某个历史版本详细信息")
@GetMapping("/{articleId}/versions/{version}")
public R getVersionDetail(@PathVariable Long articleId,
                          @PathVariable Integer version) {
    try {
        ArticleVersion versionEntity = articleVersionService.getVersionDetail(articleId, version);
        if (versionEntity == null) {
            return R.error("版本不存在");
        }

        ArticleVersionVO vo = convertToVersionVO(versionEntity);
        return R.ok().put("data", vo);
    } catch (Exception e) {
        log.error("获取版本详情失败，articleId: {}, version: {}", articleId, version, e);
        return R.error("获取版本详情失败");
    }
}


/**
 * 回滚到指定版本
 */
@Operation(summary = "回滚到指定版本", description = "将文章内容回滚到某个历史版本")
@PostMapping("/{articleId}/versions/{version}/rollback")
@Transactional
public R rollbackToVersion(@PathVariable Long articleId,
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
        log.error("回滚失败，articleId: {}", articleId, e);
        return R.error("回滚失败");
    }
}

// ==================== 协作管理相关接口 ====================

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
 * 提交修改建议
 */
@Operation(summary = "提交修改建议", description = "对文章提交修改建议（需文章开启协作模式）")
@PostMapping("/{articleId}/suggestions")
@Transactional
public R submitSuggestion(@PathVariable Long articleId,
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
 * 获取建议列表
 */
@Operation(summary = "获取建议列表", description = "查询文章的修改建议列表（分页）")
@GetMapping("/{articleId}/suggestions")
public R getSuggestions(@PathVariable Long articleId,
                        @RequestParam(required = false) Integer status,
                        @RequestParam(defaultValue = "1") Integer page,
                        @RequestParam(defaultValue = "10") Integer limit) {
    try {
        var suggestionPage = articleEditSuggestionService.getSuggestions(
                articleId, status, page, limit);

        List<ArticleSuggestionVO> voList = suggestionPage.getRecords().stream()
                .map(this::convertToSuggestionVO)
                .collect(Collectors.toList());

        PageUtils pageUtils = new PageUtils(suggestionPage);
        pageUtils.setList(voList);

        return R.ok().put("data", pageUtils);
    } catch (Exception e) {
        log.error("获取建议列表失败，articleId: {}", articleId, e);
        return R.error("获取建议列表失败");
    }
}

/**
 * 获取建议详情
 */
@Operation(summary = "获取建议详情", description = "查询单个修改建议的详细信息")
@GetMapping("/{articleId}/suggestions/{suggestionId}")
public R getSuggestionDetail(@PathVariable Long articleId,
                             @PathVariable Long suggestionId) {
    try {
        ArticleEditSuggestion suggestion = articleEditSuggestionService.getSuggestionDetail(suggestionId);

        ArticleSuggestionVO vo = convertToSuggestionVO(suggestion);
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
 * 审核建议
 */
@Operation(summary = "审核建议", description = "作者审核修改建议（通过/拒绝）")
@PutMapping("/{articleId}/suggestions/{suggestionId}")
@Transactional
public R reviewSuggestion(@PathVariable Long articleId,
                          @PathVariable Long suggestionId,
                          @Valid @RequestBody SuggestionReviewDTO dto,
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
 * 获取贡献者列表
 */
@Operation(summary = "获取贡献者列表", description = "查询文章的所有贡献者及其贡献行数")
@GetMapping("/{articleId}/contributors")
public R getContributors(@PathVariable Long articleId) {
    try {
        List<Map<String, Object>> contributors = articleContributorService.getContributors(articleId);
        return R.ok().put("data", contributors);
    } catch (Exception e) {
        log.error("获取贡献者列表失败，articleId: {}", articleId, e);
        return R.error("获取贡献者列表失败");
    }
}

// ==================== 辅助方法 ====================

private ArticleVersionVO convertToVersionVO(ArticleVersion version) {
    ArticleVersionVO vo = new ArticleVersionVO();
    vo.setId(version.getId());
    vo.setArticleId(version.getArticleId());
    vo.setVersion(version.getVersion());
    vo.setTitle(version.getTitle());
    vo.setChangeSummary(version.getChangeSummary());
    vo.setOperatorId(version.getOperatorId());
    vo.setCreateTime(version.getCreateTime());
    return vo;
}

private ArticleSuggestionVO convertToSuggestionVO(ArticleEditSuggestion suggestion) {
    ArticleSuggestionVO vo = new ArticleSuggestionVO();
    vo.setId(suggestion.getId());
    vo.setArticleId(suggestion.getArticleId());
    vo.setProposerId(suggestion.getProposerId());
    vo.setTitle(suggestion.getTitle());
    vo.setChangeSummary(suggestion.getChangeSummary());
    vo.setStatus(suggestion.getStatus());
    vo.setCreateTime(suggestion.getCreateTime());
    vo.setReviewTime(suggestion.getReviewTime());
    vo.setReviewerId(suggestion.getReviewerId());

    // 如果有 extra 信息，添加到 VO
    if (suggestion.getExtra() != null) {
        Integer addedLines = (Integer) suggestion.getExtra().get("addedLines");
        Integer removedLines = (Integer) suggestion.getExtra().get("removedLines");
        if (addedLines != null) vo.setAddedLines(addedLines);
        if (removedLines != null) vo.setRemovedLines(removedLines);
    }

    return vo;
}

// ... existing code ...















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
        
        vo.setAuditStatus(articleView.getAuditStatus());
        vo.setCreateTime(articleView.getCreateTime());
        return vo;
    }

    /**
     * 将 Article 转换为 ArticleVO
     */
    private ArticleVO convertToVO(Article article) {
        ArticleVO vo = new ArticleVO();
        vo.setId(article.getId());
        vo.setTitle(article.getTitle());
        vo.setCoverUrl(article.getCoverUrl());
        vo.setCategoryId(article.getCategoryId());
        vo.setAuthorId(article.getAuthorId());

        // 查询作者头像
        if (article.getAuthorId() != null) {
            User author = userService.getById(article.getAuthorId());
            if (author != null) {
                vo.setAuthorAvatar(author.getAvatar());
                vo.setAuthorNickname(author.getNickname());  // ✅ 补充设置作者昵称
            }
        }
        
        vo.setLikeCount(article.getLikeCount());
        vo.setDislikeCount(article.getDislikeCount());
        vo.setViewCount(article.getViewCount());
        
        // 如果 publishTime 为空，则使用 createTime
        vo.setPublishTime(article.getPublishTime() != null ? article.getPublishTime() : java.util.Date.from(article.getCreateTime().atZone(java.time.ZoneId.systemDefault()).toInstant()));
        
        vo.setAuditStatus(article.getAuditStatus());
        vo.setCreateTime(article.getCreateTime());
        return vo;
    }

    /**
     * 将 Article 转换为 ArticleDetailVO
     */
  private ArticleDetailVO convertToDetailVO(Article article) {
       ArticleDetailVO vo = new ArticleDetailVO();
       vo.setId(article.getId());
       vo.setTitle(article.getTitle());
       vo.setCoverUrl(article.getCoverUrl());
       vo.setCategoryId(article.getCategoryId());
       
       // 如果是 ArticleView 类型，直接使用联表查询的结果
       if (article instanceof ArticleView) {
           ArticleView view = (ArticleView) article;
           vo.setCategoryName(view.getCategoryName());
           vo.setAuthorAvatar(view.getAuthorAvatar());
           
           // 查询作者昵称（如果 ArticleView 中有这个字段）
           vo.setAuthorNickname(view.getAuthorNickname());
       } else {
           // 否则手动查询分类和用户信息
           vo.setCategoryName(null); // 需要时可以通过 categoryId 查询
           vo.setAuthorAvatar(null);
           vo.setAuthorNickname(null);
       }
       
       vo.setAuthorId(article.getAuthorId());
       
       // 查询作者头像和昵称（如果上面没有设置的话）
       if (article.getAuthorId() != null && vo.getAuthorAvatar() == null) {
           User author = userService.getById(article.getAuthorId());
           if (author != null) {
               vo.setAuthorAvatar(author.getAvatar());
               vo.setAuthorNickname(author.getNickname());
           }
       }
       
       vo.setLikeCount(article.getLikeCount());
       vo.setDislikeCount(article.getDislikeCount());
       vo.setViewCount(article.getViewCount());
       
       // 如果 publishTime 为空，则使用 createTime
       vo.setPublishTime(article.getPublishTime() != null ? article.getPublishTime() : java.util.Date.from(article.getCreateTime().atZone(java.time.ZoneId.systemDefault()).toInstant()));
       
       vo.setAuditStatus(article.getAuditStatus());
       vo.setCreateTime(article.getCreateTime());
       vo.setContent(article.getContent());
       vo.setAttachment(article.getAttachment());
       vo.setFavoriteCount(article.getFavoriteCount());
       vo.setAuditReply(article.getAuditReply());
       if (article.getUpdateTime() != null) {
           vo.setUpdateTime(article.getUpdateTime().toString());
       }
       return vo;
   }

    /**
     * 将 ArticleDraftDTO 转换为 Article
     */
    private Article convertToEntityFromDraft(ArticleDraftDTO dto) {
        Article article = new Article();
        article.setTitle(dto.getTitle());
        article.setCoverUrl(dto.getCoverUrl());
        article.setCategoryId(dto.getCategoryId());
        article.setContent(dto.getContent());
        article.setAttachment(dto.getAttachment());
        return article;
    }

    /**
     * 将 ArticleCreateDTO 转换为 Article
     */
    private Article convertToEntity(ArticleCreateDTO dto) {
        Article article = new Article();
        article.setTitle(dto.getTitle());
        article.setCoverUrl(dto.getCoverUrl());
        article.setCategoryId(dto.getCategoryId());
        article.setContent(dto.getContent());
        article.setAttachment(dto.getAttachment());
        return article;
    }

    /**
     * 将 ArticleUpdateDTO 转换为 Article
     */
    private void convertToUpdateEntity(ArticleUpdateDTO dto, Article article) {
        article.setTitle(dto.getTitle());
        article.setCoverUrl(dto.getCoverUrl());
        article.setCategoryId(dto.getCategoryId());
        article.setContent(dto.getContent());
        article.setAttachment(dto.getAttachment());
    }

    /**
     * 更新帖子浏览量
     */
    private Article updateViewCount(Long id) {
        Article article = articleService.getById(id);
        if (article != null) {
            article.setViewCount(Optional.ofNullable(article.getViewCount()).orElse(0) + 1);
            articleService.updateById(article);
            // 不再使用 Wrapper，而是调用专用方法
            return articleService.selectViewById(id);
        }
        return null;
    }

    /**
     * 准备帖子保存数据
     */
    private void prepareArticleForSave(Article article) {
//        article.setId(Instant.now().toEpochMilli());
        // 确保 publish_time 有值
        if (article.getPublishTime() == null) {
            article.setPublishTime(new Date());
        }
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
        // 不再使用tableName 判断，这里可以根据实际需求决定是否保留
        // 如果后续没有用到这个方法，可以考虑删除
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

    /**
     * 将 ArticleView 转换为 ArticleDraftVO
     */
    private ArticleDraftVO convertToDraftVO(ArticleView articleView) {
        ArticleDraftVO vo = new ArticleDraftVO();
        vo.setId(articleView.getId());
        vo.setTitle(articleView.getTitle());
        vo.setContent(articleView.getContent());
        vo.setCoverUrl(articleView.getCoverUrl());
        vo.setCategoryId(articleView.getCategoryId());
        vo.setCategoryName(articleView.getCategoryName());
        vo.setAttachment(articleView.getAttachment());
        vo.setAuthorId(articleView.getAuthorId());
        vo.setAuditStatus(articleView.getAuditStatus());
        vo.setCreateTime(articleView.getCreateTime());
        vo.setUpdateTime(articleView.getUpdateTime());
        return vo;
    }

}
