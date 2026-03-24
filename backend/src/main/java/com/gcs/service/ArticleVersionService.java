package com.gcs.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.gcs.entity.Article;
import com.gcs.entity.ArticleVersion;

import java.util.List;
import java.util.Map;

/**
 * 文章版本服务接口
 */
public interface ArticleVersionService extends IService<ArticleVersion> {

    /**
     * 创建新版本（保存文章快照）
     *
     * @param article 文章内容
     * @param operatorId 操作人 ID
     * @param changeSummary 修改摘要
     * @return 版本号
     */
    Integer createVersion(Article article, Long operatorId, String changeSummary);

    /**
     * 获取版本列表
     *
     * @param articleId 文章 ID
     * @return 版本列表
     */
    List<ArticleVersion> getVersionHistory(Long articleId);

    /**
     * 获取指定版本详情
     *
     * @param articleId 文章 ID
     * @param version 版本号
     * @return 版本信息
     */
    ArticleVersion getVersionDetail(Long articleId, Integer version);

    /**
     * 对比两个版本（返回两个版本的完整数据）
     *
     * @param articleId 文章 ID
     * @param versionA 版本 A
     * @param versionB 版本 B
     * @return 包含两个版本完整数据的 Map
     */
    Map<String, Object> compareVersions(Long articleId, Integer versionA, Integer versionB);

    /**
     * 回滚到指定版本
     *
     * @param articleId 文章 ID
     * @param version 版本号
     * @param operatorId 操作人 ID
     */
    void rollbackToVersion(Long articleId, Integer version, Long operatorId);
}
