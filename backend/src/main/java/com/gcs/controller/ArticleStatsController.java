package com.gcs.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.gcs.entity.Article;
import com.gcs.service.ArticleService;
import com.gcs.utils.MPUtil;
import com.gcs.utils.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.text.SimpleDateFormat;
import java.util.*;

/**
 * 文章统计控制器
 * 提供文章数据统计和分析相关的 RESTful API 接口
 */
@Slf4j
@Tag(name = "文章统计分析", description = "文章数据统计、图表分析等功能")
@RestController
@RequestMapping("/articles")
public class ArticleStatsController {

    @Autowired
    private ArticleService articleService;

    /**
     * 按值统计
     */
    @Operation(summary = "按值统计", description = "根据指定字段进行数值统计")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "统计成功"),
            @ApiResponse(responseCode = "500", description = "统计失败")
    })
    @GetMapping("/stats/value/{xColumn}/{yColumn}")
    public R valueStatistics(
            @Parameter(description = "X轴字段") @PathVariable("yColumn") String yColumn,
            @Parameter(description = "Y轴字段") @PathVariable("xColumn") String xColumn,
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
    @Operation(summary = "按值统计（多列）", description = "同时对多个Y轴字段进行统计")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "统计成功"),
            @ApiResponse(responseCode = "500", description = "统计失败")
    })
    @GetMapping("/stats/value/multiple/{xColumn}")
    public R multipleValueStatistics(
            @Parameter(description = "X轴字段") @PathVariable("xColumn") String xColumn,
            @Parameter(description = "Y轴字段列表（逗号分隔）") @RequestParam String yColumns,
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
    @Operation(summary = "时间统计", description = "按日/月/年统计指定字段")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "统计成功"),
            @ApiResponse(responseCode = "500", description = "统计失败")
    })
    @GetMapping("/stats/time/{xColumn}/{yColumn}/{timeType}")
    public R timeStatistics(
            @Parameter(description = "时间字段") @PathVariable("yColumn") String yColumn,
            @Parameter(description = "统计字段") @PathVariable("xColumn") String xColumn,
            @Parameter(description = "时间类型（日/月/年）") @PathVariable("timeType") String timeType,
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
    @Operation(summary = "分组统计", description = "按指定字段分组统计")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "统计成功"),
            @ApiResponse(responseCode = "500", description = "统计失败")
    })
    @GetMapping("/stats/group/{column}")
    public R groupStatistics(
            @Parameter(description = "分组字段") @PathVariable("column") String column,
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
     * 获取文章总数
     */
    @Operation(summary = "获取文章总数", description = "根据条件统计文章数量")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "查询成功"),
            @ApiResponse(responseCode = "500", description = "查询失败")
    })
    @GetMapping("/count")
    public R getCount(
            @RequestParam Map<String, Object> params,
            Article article,
            HttpServletRequest request) {
        try {
            QueryWrapper<Article> queryWrapper = buildQueryWrapper(article, params);
            Long count = articleService.count(MPUtil.sort(
                    MPUtil.between(MPUtil.likeOrEq(queryWrapper, article), params), params));

            return R.ok().put("data", count);
        } catch (Exception e) {
            log.error("获取文章总数失败", e);
            return R.error("查询失败");
        }
    }

    /**
     * 构建查询条件
     */
    private QueryWrapper<Article> buildQueryWrapper(Article article, Map<String, Object> params) {
        QueryWrapper<Article> queryWrapper = new QueryWrapper<>();
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
}
