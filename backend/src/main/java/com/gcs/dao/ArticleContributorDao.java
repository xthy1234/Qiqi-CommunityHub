package com.gcs.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.gcs.entity.ArticleContributor;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 文章贡献者 DAO 接口
 */
@Mapper
public interface ArticleContributorDao extends BaseMapper<ArticleContributor> {

    /**
     * 查询文章的所有贡献者
     *
     * @param articleId 文章 ID
     * @return 贡献者列表
     */
    List<ArticleContributor> selectByArticleId(@Param("articleId") Long articleId);

    /**
     * 查询指定用户的贡献记录
     *
     * @param articleId 文章 ID
     * @param userId 用户 ID
     * @return 贡献记录
     */
    ArticleContributor selectByArticleAndUser(@Param("articleId") Long articleId, 
                                              @Param("userId") Long userId);

    /**
     * 更新贡献行数（累加）
     *
     * @param articleId 文章 ID
     * @param userId 用户 ID
     * @param lines 增加的行数
     * @return 影响行数
     */
    int addContributedLines(@Param("articleId") Long articleId, 
                           @Param("userId") Long userId, 
                           @Param("lines") Integer lines);
}
