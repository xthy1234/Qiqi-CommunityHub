package com.gcs.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.gcs.entity.ArticleContributor;

import java.util.List;
import java.util.Map;

/**
 * 文章贡献者服务接口
 */
public interface ArticleContributorService extends IService<ArticleContributor> {

    /**
     * 添加或更新贡献记录
     *
     * @param articleId 文章 ID
     * @param userId 用户 ID
     * @param lines 贡献行数
     */
    void addContribution(Long articleId, Long userId, int lines);

    /**
     * 获取贡献者列表（按贡献行数排序）
     *
     * @param articleId 文章 ID
     * @return 贡献者列表（含用户信息）
     */
    List<Map<String, Object>> getContributors(Long articleId);

    /**
     * 统计贡献者数量
     *
     * @param articleId 文章 ID
     * @return 贡献者数量
     */
    long countContributors(Long articleId);
}
