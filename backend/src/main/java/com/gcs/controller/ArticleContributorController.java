package com.gcs.controller;

import com.gcs.annotation.IgnoreAuth;
import com.gcs.service.ArticleContributorService;
import com.gcs.utils.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 文章贡献者控制器
 * 提供贡献者相关的 RESTful API 接口
 */
@Slf4j
@Tag(name = "文章贡献者管理", description = "文章贡献者相关的 RESTful API 接口")
@RestController
@RequestMapping("/articles")
public class ArticleContributorController {

    @Autowired
    private ArticleContributorService articleContributorService;

    /**
     * 获取文章贡献者列表
     */
    @Operation(summary = "获取文章贡献者列表", description = "查询文章的贡献者及其贡献详情")
    @GetMapping("/{articleId}/contributors")
    @IgnoreAuth
    public R getContributors(@PathVariable Long articleId) {
        try {
            List<Map<String, Object>> contributors = articleContributorService.getContributors(articleId);
            return R.ok().put("data", contributors);
        } catch (Exception e) {
            log.error("获取贡献者列表失败，articleId: {}", articleId, e);
            return R.error("获取贡献者列表失败");
        }
    }

    /**
     * 获取贡献者数量
     */
    @Operation(summary = "获取贡献者数量", description = "统计文章的贡献者总数")
    @GetMapping("/{articleId}/contributors/count")
    @IgnoreAuth
    public R countContributors(@PathVariable Long articleId) {
        try {
            long count = articleContributorService.countContributors(articleId);
            return R.ok().put("data", count);
        } catch (Exception e) {
            log.error("获取贡献者数量失败，articleId: {}", articleId, e);
            return R.error("获取贡献者数量失败");
        }
    }
}
