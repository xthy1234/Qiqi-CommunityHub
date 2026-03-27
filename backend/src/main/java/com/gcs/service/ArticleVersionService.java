package com.gcs.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.gcs.entity.Article;
import com.gcs.entity.ArticleVersion;
import com.gcs.vo.ArticleVersionSimpleVO;
import com.gcs.vo.ArticleVersionVO;
import com.gcs.vo.ArticleVersionSimpleVO;
import com.gcs.vo.ArticleVersionVO;

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
     * 获取版本历史列表（包含详细信息）
     *
     * @param articleId 文章 ID
     * @return 版本历史列表（简略 VO）
     */
    List<ArticleVersionSimpleVO> getVersionHistoryWithDetails(Long articleId);

    /**
     * 获取指定版本详情
     *
     * @param articleId 文章 ID
     * @param version 版本号
     * @return 版本信息
     */
    ArticleVersion getVersionDetail(Long articleId, Integer version);

    /**
     * 获取指定版本详情（包含完整 VO）
     *
     * @param articleId 文章 ID
     * @param version 版本号
     * @return 版本详细 VO
     */
    ArticleVersionVO getVersionDetailWithVo(Long articleId, Integer version);

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
     * 对比两个版本（返回完整 VO）
     *
     * @param articleId 文章 ID
     * @param versionA 版本 A
     * @param versionB 版本 B
     * @return 包含两个版本详细 VO 的 Map
     */
    Map<String, Object> compareVersionsWithVo(Long articleId, Integer versionA, Integer versionB);

    /**
     * 回滚到指定版本
     *
     * @param articleId 文章 ID
     * @param version 版本号
     * @param operatorId 操作人 ID
     */
    void rollbackToVersion(Long articleId, Integer version, Long operatorId);

    // 新增方法
    /**
     * 创建小版本
     */
    Integer createMinorVersion(Article article, Long operatorId, String changeSummary);

    /**
     * 创建大版本
     */
    Integer createMajorVersion(Article article, Long operatorId, String changeSummary);

    /**
     * 计算下一个版本号
     * @param articleId 文章 ID
     * @param isMajor 是否为大版本
     * @return [majorVersion, minorVersion]
     */
    int[] calculateNextVersion(Long articleId, boolean isMajor);

}
