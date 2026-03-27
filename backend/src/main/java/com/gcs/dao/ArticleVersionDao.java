package com.gcs.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.gcs.entity.ArticleVersion;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 文章版本 DAO 接口
 */
@Mapper
public interface ArticleVersionDao extends BaseMapper<ArticleVersion> {

    /**
     * 查询文章的版本列表（按版本号降序）
     *
     * @param articleId 文章 ID
     * @return 版本列表
     */
    List<ArticleVersion> selectByArticleId(@Param("articleId") Long articleId);

    /**
     * 查询指定版本
     *
     * @param articleId 文章 ID
     * @param version 版本号
     * @return 版本信息
     */
    ArticleVersion selectByVersion(@Param("articleId") Long articleId, 
                                   @Param("version") Integer version);

    /**
     * 获取最大版本号
     *
     * @param articleId 文章 ID
     * @return 最大版本号
     */
    Integer getMaxVersion(@Param("articleId") Long articleId);

    /**
     * 获取最大主版本号
     *
     * @param articleId 文章 ID
     * @return 最大主版本号
     */
    Integer selectMaxMajorVersion(@Param("articleId") Long articleId);

    /**
     * 获取指定主版本下的最大次版本号
     *
     * @param articleId 文章 ID
     * @param majorVersion 主版本号
     * @return 最大次版本号
     */
    Integer selectMaxMinorVersion(@Param("articleId") Long articleId, 
                                  @Param("majorVersion") Integer majorVersion);
}
