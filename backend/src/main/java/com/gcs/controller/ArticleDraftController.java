package com.gcs.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.gcs.annotation.IgnoreAuth;
import com.gcs.converter.ArticleConverter;
import com.gcs.converter.ArticleDraftConverter;
import com.gcs.dto.ArticleDraftDTO;
import com.gcs.entity.Article;
import com.gcs.entity.ArticleDraft;
import com.gcs.entity.ArticleVersion;
import com.gcs.enums.AuditStatus;
import com.gcs.service.ArticleDraftService;
import com.gcs.service.ArticleService;
import com.gcs.service.ArticleVersionService;
import com.gcs.service.UserService;
import com.gcs.utils.AuthUtils;
import com.gcs.utils.PageUtils;
import com.gcs.utils.R;
import com.gcs.utils.SessionUtils;
import com.gcs.vo.ArticleDetailVO;
import com.gcs.vo.ArticleDraftSimpleVO;
import com.gcs.vo.ArticleDraftVO;
import com.gcs.vo.UserSimpleVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Tag(name = "文章草稿管理", description = "文章草稿相关的 RESTful API 接口")
@RestController
@RequestMapping("/articles/drafts")
public class ArticleDraftController {

    @Autowired
    private ArticleDraftService articleDraftService;

    @Autowired
    private ArticleService articleService;

    @Autowired
    private ArticleVersionService articleVersionService;

    @Autowired
    private UserService userService;

    @Autowired
    private ArticleDraftConverter articleDraftConverter;
    
    @Autowired
    private ArticleConverter articleConverter;  // ✅ 新增：添加 articleConverter 注入

    @Autowired
    private SessionUtils sessionUtils;

    @Autowired
    private AuthUtils authUtils;

    /**
     * 创建新草稿（写文章时调用）
     */
    @Operation(summary = "创建新草稿", description = "用户点击'写文章'时调用，返回草稿 ID，前端后续自动保存都使用该 ID")
    @PostMapping
    public R createDraft(@RequestBody(required = false) Map<String, Object> data, HttpServletRequest request) {
        try {
            Long currentUserId = sessionUtils.getCurrentUserId(request);
            if (currentUserId == null) {
                return R.error("请先登录");
            }

            // 提取参数
            Long articleId = data != null ? convertToLong(data.get("articleId")) : null;
            String title = data != null ? (String) data.get("title") : "未命名草稿";
            Map<String, Object> content = data != null ? (Map<String, Object>) data.get("content") : new HashMap<>();
            String coverUrl = data != null ? (String) data.get("coverUrl") : null;
            Long categoryId = data != null ? convertToLong(data.get("categoryId")) : null;

            // ✅ 无论是否编辑已有文章，都先检查是否存在草稿
            if (articleId != null) {
                // 1. 编辑已有文章：验证文章权限 + 检查草稿
                Article article = articleService.getById(articleId);
                if (article == null) {
                    return R.error("文章不存在");
                }
                if (!authUtils.isOwner(articleId, article.getAuthorId(), currentUserId)) {
                    return R.error("无权限编辑此文章");
                }
                
                // 检查是否已有草稿，有则直接返回
                ArticleDraft existingDraft = articleDraftService.getDraft(articleId, currentUserId);
                if (existingDraft != null) {
                    Map<String, Object> result = new HashMap<>();
                    result.put("draftId", existingDraft.getId());
                    result.put("articleId", articleId);
                    result.put("hasDraft", true);
                    log.info("草稿已存在，直接返回，draftId: {}, articleId: {}, userId: {}", 
                            existingDraft.getId(), articleId, currentUserId);
                    return R.ok().put("data", result);
                }
                
                // ✅ 新增：如果没有草稿，使用文章内容初始化草稿
                title = article.getTitle() != null ? article.getTitle() : "未命名草稿";
                content = article.getContent() != null ? article.getContent() : new HashMap<>();
                coverUrl = article.getCoverUrl();
                categoryId = article.getCategoryId();
                
            } else {
                // 2. 新建文章：限制每个用户只能有一个未关联文章的草稿
                List<ArticleDraft> newArticleDrafts = articleDraftService.getBaseMapper()
                    .selectList(new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<ArticleDraft>()
                        .eq("user_id", currentUserId)
                        .isNull("article_id"));
                
                if (newArticleDrafts != null && !newArticleDrafts.isEmpty()) {
                    ArticleDraft existingDraft = newArticleDrafts.get(0);
                    Map<String, Object> result = new HashMap<>();
                    result.put("draftId", existingDraft.getId());
                    result.put("articleId", null);
                    result.put("hasDraft", true);
                    log.info("发现未关联文章的新草稿，直接返回，draftId: {}, userId: {}", 
                            existingDraft.getId(), currentUserId);
                    return R.ok().put("data", result);
                }
            }

            // 创建新草稿
            Long draftId = articleDraftService.createDraft(currentUserId, articleId, title, content, coverUrl, categoryId);

            Map<String, Object> result = new HashMap<>();
            result.put("draftId", draftId);
            result.put("articleId", articleId);
            result.put("hasDraft", false);
            
            log.info("创建新草稿成功，draftId: {}, articleId: {}, userId: {}", draftId, articleId, currentUserId);

            return R.ok("创建成功").put("data", result);
        } catch (Exception e) {
            log.error("创建草稿失败", e);
            return R.error("创建失败");
        }
    }

    /**
     * 根据草稿 ID 获取草稿详情
     */
    @Operation(summary = "获取草稿详情", description = "根据草稿 ID 获取草稿内容（用于加载编辑器）")
    @GetMapping("/{draftId}")
    public R getDraft(@PathVariable("draftId") Long draftId, HttpServletRequest request) {
        try {
            Long currentUserId = sessionUtils.getCurrentUserId(request);
            if (currentUserId == null) {
                return R.error("请先登录");
            }

            ArticleDraft draft = articleDraftService.getDraftById(draftId);
            if (draft == null) {
                return R.error("草稿不存在");
            }

            // 验证权限（只能是草稿所有者）
            if (!draft.getUserId().equals(currentUserId)) {
                return R.error("无权限查看此草稿");
            }

            ArticleDraftVO draftVO = articleDraftConverter.toVO(draft);
            
            // 补充作者信息
            var author = userService.getById(draft.getUserId());
            if (author != null) {
                var authorVO = new UserSimpleVO();
                authorVO.setId(author.getId());
                authorVO.setNickname(author.getNickname());
                authorVO.setAvatar(author.getAvatar());
                draftVO.setAuthor(authorVO);
            }

            Map<String, Object> result = new HashMap<>();
            result.put("draft", draftVO);
            result.put("content", draft.getContent());
            result.put("title", draft.getTitle());
            result.put("extra", draft.getExtra());
            
            // 如果有 articleId，返回版本信息
            if (draft.getArticleId() != null) {
                List<ArticleVersion> versions = articleVersionService.getVersionHistory(draft.getArticleId());
                if (versions != null && !versions.isEmpty()) {
                    ArticleVersion latestVersion = versions.get(0);
                    result.put("latestVersion", latestVersion.getVersion());
                    result.put("majorVersion", latestVersion.getMajorVersion());
                    result.put("minorVersion", latestVersion.getMinorVersion());
                    result.put("versionDisplay", latestVersion.getMajorVersion() + "." + latestVersion.getMinorVersion());
                }
            }

            return R.ok().put("data", result);
        } catch (Exception e) {
            log.error("获取草稿详情失败，draftId: {}", draftId, e);
            return R.error("获取数据失败");
        }
    }

    /**
     * 根据草稿 ID 自动保存
     */
    @Operation(summary = "自动保存草稿（基于 ID）", description = "前端定时调用，根据草稿 ID 保存内容")
    @PutMapping("/{draftId}")
    @Transactional(rollbackFor = Exception.class)
    public R autoSaveByDraftId(
            @PathVariable("draftId") Long draftId,
            @RequestBody Map<String, Object> saveData,
            HttpServletRequest request) {
        try {
            Long currentUserId = sessionUtils.getCurrentUserId(request);
            if (currentUserId == null) {
                return R.error("请先登录");
            }

            ArticleDraft draft = articleDraftService.getDraftById(draftId);
            if (draft == null) {
                return R.error("草稿不存在");
            }

            // 验证权限
            if (!draft.getUserId().equals(currentUserId)) {
                return R.error("无权限保存此草稿");
            }

            // 提取内容和标题
            Map<String, Object> content = (Map<String, Object>) saveData.get("content");
            String title = (String) saveData.get("title");
            Map<String, Object> extra = (Map<String, Object>) saveData.get("extra");
            
            // ✅ 修复：使用 convertToLong 处理 categoryId 的类型转换
            String coverUrl = (String) saveData.get("coverUrl");
            Long categoryId = convertToLong(saveData.get("categoryId"));

            if (content == null) {
                return R.error("内容不能为空");
            }

            // 更新草稿
            draft.setContent(content);
            draft.setTitle(title);
            draft.setExtra(extra);
            draft.setCoverUrl(coverUrl);
            draft.setCategoryId(categoryId);
            draft.setAutoSaveTime(LocalDateTime.now());
            articleDraftService.updateById(draft);

            return R.ok("自动保存成功");
        } catch (Exception e) {
            log.error("自动保存草稿失败，draftId: {}", draftId, e);
            return R.error("保存失败");
        }
    }

    /**
     * 发布文章（新文章或更新已有文章）
     */
    @Operation(summary = "发布文章", description = "根据草稿发布文章（article_id 为空时创建新文章，否则更新已有文章）")
    @PostMapping("/{draftId}/publish")
    @Transactional(rollbackFor = Exception.class)
    public R publish(
            @PathVariable("draftId") Long draftId,
            @RequestBody(required = false) Map<String, Object> publishData,
            HttpServletRequest request) {
        try {
            Long currentUserId = sessionUtils.getCurrentUserId(request);
            if (currentUserId == null) {
                return R.error("请先登录");
            }

            ArticleDraft draft = articleDraftService.getDraftById(draftId);
            if (draft == null) {
                return R.error("草稿不存在");
            }

            // 验证权限
            if (!draft.getUserId().equals(currentUserId)) {
                return R.error("无权限发布此草稿");
            }

            Long articleId = draft.getArticleId();
            Article article;
            
            if (articleId == null) {
                // === 情况 1：新文章，创建 article 记录 ===
                article = new Article();
                article.setAuthorId(currentUserId);
                article.setTitle(draft.getTitle());
                article.setContent(draft.getContent());
                article.setExtra(draft.getExtra());
                article.setCoverUrl(draft.getCoverUrl());
                article.setCategoryId(draft.getCategoryId());
                article.setAuditStatus(AuditStatus.PENDING); // 待审核
                article.setPublishTime(null); // 发布后设置为当前时间
                article.setCurrentVersion(1); // ✅ 初始化版本号为 1
                
                articleService.save(article);
                articleId = article.getId();
                
                log.info("创建新文章成功，articleId: {}, userId: {}", articleId, currentUserId);
                
            } else {
                // === 情况 2：更新已有文章 ===
                article = articleService.getById(articleId);
                if (article == null) {
                    return R.error("文章不存在");
                }
                
                // 验证权限
                if (!authUtils.isOwner(articleId, article.getAuthorId(), currentUserId)) {
                    return R.error("无权限修改此文章");
                }
                
                article.setTitle(draft.getTitle());
                article.setContent(draft.getContent());
                if (draft.getExtra() != null) {
                    article.setExtra(draft.getExtra());
                }
                if (draft.getCoverUrl() != null) {
                    article.setCoverUrl(draft.getCoverUrl());
                }
                if (draft.getCategoryId() != null) {
                    article.setCategoryId(draft.getCategoryId());
                }
                
                // ✅ 新增：更新 currentVersion（从版本表获取最新版本号）
                List<ArticleVersion> versions = articleVersionService.getVersionHistory(articleId);
                if (versions != null && !versions.isEmpty()) {
                    ArticleVersion latestVersion = versions.get(0);
                    article.setCurrentVersion(latestVersion.getVersion());
                }
                
                articleService.updateById(article);
                
                log.info("更新文章成功，articleId: {}, newVersion: {}", 
                        articleId, article.getCurrentVersion());
            }

            // ✅ 新增：提取 versionType 参数
            String changeSummary = publishData != null ? (String) publishData.get("changeSummary") : draft.getChangeSummary();
            Integer versionType = publishData != null ? 
                (publishData.get("versionType") instanceof Integer ? 
                    (Integer) publishData.get("versionType") : 
                    Integer.parseInt(publishData.get("versionType").toString())) : null;
            
            // ✅ 根据 versionType 决定创建大版本还是小版本
            // versionType: 0=小版本，1=大版本，null=自动判断
            Integer version;
            if (versionType != null) {
                if (versionType == 1) {
                    // 创建大版本
                    version = articleVersionService.createMajorVersion(article, currentUserId, changeSummary);
                } else {
                    // 创建小版本
                    version = articleVersionService.createMinorVersion(article, currentUserId, changeSummary);
                }
            } else {
                // 自动判断：新文章第一个版本设为大版本，其他为小版本
                if (draft.getArticleId() == null) {
                    version = articleVersionService.createMajorVersion(article, currentUserId, changeSummary);
                } else {
                    version = articleVersionService.createMinorVersion(article, currentUserId, changeSummary);
                }
            }

            // 删除草稿
            articleDraftService.deleteDraftById(draftId);

            Map<String, Object> result = new HashMap<>();
            result.put("articleId", articleId);
            result.put("version", version);
            
            ArticleVersion versionEntity = articleVersionService.getVersionDetail(articleId, version);
            if (versionEntity != null) {
                result.put("majorVersion", versionEntity.getMajorVersion());
                result.put("minorVersion", versionEntity.getMinorVersion());
                result.put("versionDisplay", versionEntity.getMajorVersion() + "." + versionEntity.getMinorVersion());
                result.put("versionType", versionEntity.getVersionType());
            }
            
            // ✅ 新增：获取文章详情并设置版本号
            Article articleDetail = articleService.getArticleDetail(articleId);
            ArticleDetailVO articleVO = articleConverter.toDetailVO(articleDetail);
            
            // 手动查询并设置版本号
            List<ArticleVersion> versions = articleVersionService.getVersionHistory(articleId);
            if (versions != null && !versions.isEmpty()) {
                ArticleVersion latestVersion = versions.get(0);
                articleVO.setMajorVersion(latestVersion.getMajorVersion());
                articleVO.setMinorVersion(latestVersion.getMinorVersion());
            } else {
                // 如果没有版本记录，默认为 1.0
                articleVO.setMajorVersion(1);
                articleVO.setMinorVersion(0);
            }
            
            result.put("article", articleVO);

            return R.ok("发布成功").put("data", result);
        } catch (Exception e) {
            log.error("发布文章失败，draftId: {}", draftId, e);
            return R.error("发布失败");
        }
    }

    /**
     * 保存为草稿（不发布）
     */
    @Operation(summary = "保存草稿（不发布）", description = "手动保存草稿到数据库（不创建版本，不发布）")
    @PostMapping("/{draftId}/save")
    @Transactional(rollbackFor = Exception.class)
    public R saveDraft(
            @PathVariable("draftId") Long draftId,
            @RequestBody Map<String, Object> saveData,
            HttpServletRequest request) {
        try {
            Long currentUserId = sessionUtils.getCurrentUserId(request);
            if (currentUserId == null) {
                return R.error("请先登录");
            }

            ArticleDraft draft = articleDraftService.getDraftById(draftId);
            if (draft == null) {
                return R.error("草稿不存在");
            }

            // 验证权限
            if (!draft.getUserId().equals(currentUserId)) {
                return R.error("无权限保存此草稿");
            }

            // ✅ 提取数据（为 null 时使用草稿原值）
            String title = saveData.get("title") != null ? 
                (String) saveData.get("title") : draft.getTitle();
            Map<String, Object> content = saveData.get("content") != null ? 
                (Map<String, Object>) saveData.get("content") : draft.getContent();
            Map<String, Object> extra = saveData.get("extra") != null ? 
                (Map<String, Object>) saveData.get("extra") : draft.getExtra();
            String coverUrl = saveData.get("coverUrl") != null ? 
                (String) saveData.get("coverUrl") : draft.getCoverUrl();
            Long categoryId = saveData.get("categoryId") != null ? 
                convertToLong(saveData.get("categoryId")) : draft.getCategoryId();
            
            if (content == null) {
                return R.error("内容不能为空");
            }

            // 更新草稿
            draft.setTitle(title);
            draft.setContent(content);
            draft.setExtra(extra);
            draft.setCoverUrl(coverUrl);
            draft.setCategoryId(categoryId);
            draft.setAutoSaveTime(LocalDateTime.now());
            articleDraftService.updateById(draft);
            
            log.info("保存草稿成功，draftId: {}", draftId);
            return R.ok("保存成功");
            
        } catch (Exception e) {
            log.error("保存草稿失败，draftId: {}", draftId, e);
            return R.error("保存失败");
        }
    }

    /**
     * 删除草稿
     */
    @Operation(summary = "删除草稿", description = "根据草稿 ID 删除草稿")
    @DeleteMapping("/{draftId}")
    @Transactional(rollbackFor = Exception.class)
    public R deleteDraft(@PathVariable("draftId") Long draftId, HttpServletRequest request) {
        try {
            Long currentUserId = sessionUtils.getCurrentUserId(request);
            if (currentUserId == null) {
                return R.error("请先登录");
            }

            ArticleDraft draft = articleDraftService.getDraftById(draftId);
            if (draft == null) {
                return R.error("草稿不存在");
            }

            // 验证权限
            if (!draft.getUserId().equals(currentUserId)) {
                return R.error("无权限删除此草稿");
            }

            articleDraftService.deleteDraftById(draftId);
            log.info("删除草稿成功，draftId: {}, userId: {}", draftId, currentUserId);

            return R.ok("草稿已删除");
        } catch (Exception e) {
            log.error("删除草稿失败，draftId: {}", draftId, e);
            return R.error("删除失败");
        }
    }

    /**
     * 获取用户的草稿列表
     */
    @Operation(summary = "获取我的草稿列表", description = "查询当前用户的所有草稿（分页）")
    @GetMapping("/my")
    public R getMyDrafts(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer page,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "10") Integer limit,
            HttpServletRequest request) {
        try {
            Long currentUserId = sessionUtils.getCurrentUserId(request);
            if (currentUserId == null) {
                return R.error("请先登录");
            }

            IPage<ArticleDraft> draftPage = articleDraftService.getDraftsByUserId(currentUserId, page, limit);
            
            // 转换为 SimpleVO
            List<ArticleDraftSimpleVO> simpleVOList = draftPage.getRecords().stream()
                    .map(draft -> {
                        ArticleDraftSimpleVO vo = new ArticleDraftSimpleVO();
                        vo.setId(draft.getId());
                        vo.setArticleId(draft.getArticleId());
                        vo.setTitle(draft.getTitle());
                        vo.setAutoSavedAt(draft.getAutoSaveTime());
                        vo.setCreateTime(draft.getCreateTime());
                        return vo;
                    })
                    .collect(Collectors.toList());

            PageUtils pageResult = new PageUtils(draftPage);
            pageResult.setList(simpleVOList);

            return R.ok().put("data", pageResult);
        } catch (Exception e) {
            log.error("获取草稿列表失败，userId: {}", sessionUtils.getCurrentUserId(request), e);
            return R.error("查询失败");
        }
    }

    /**
     * 辅助方法：将 Object 转换为 Long（兼容 Integer、String、Long）
     */
    private Long convertToLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Long) {
            return (Long) value;
        }
        if (value instanceof Integer) {
            return ((Integer) value).longValue();
        }
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        try {
            return Long.parseLong(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
