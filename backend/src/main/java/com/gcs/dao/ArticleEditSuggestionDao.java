package com.gcs.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.gcs.entity.ArticleEditSuggestion;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 文章修改建议 DAO 接口
 */
@Mapper
public interface ArticleEditSuggestionDao extends BaseMapper<ArticleEditSuggestion> {

    /**
     * 查询文章的建议列表
     *
     * @param articleId 文章 ID
     * @param status 状态（可选）
     * @return 建议列表
     */
    List<ArticleEditSuggestion> selectByArticleId(@Param("articleId") Long articleId, 
                                                  @Param("status") Integer status);

    /**
     * 查询待审核的建议列表
     *
     * @param articleId 文章 ID
     * @return 待审核建议列表
     */
    List<ArticleEditSuggestion> selectPendingSuggestions(@Param("articleId") Long articleId);

    /**
     * 统计待审核建议数量
     *
     * @param articleId 文章 ID
     * @return 待审核数量
     */
    Integer countPendingSuggestions(@Param("articleId") Long articleId);

    /**
     * 查询用户提出的建议列表（按创建时间降序）
     *
     * @param proposerId 建议者 ID
     * @param status 状态（可选）
     * @return 建议列表
     */
    List<ArticleEditSuggestion> selectByProposerId(@Param("proposerId") Long proposerId,
                                                   @Param("status") Integer status);

    /**
     * 查询用户的文章收到的建议列表（按创建时间降序）
     *
     * @param authorId 文章作者 ID
     * @param status 状态（可选）
     * @return 建议列表
     */
    List<ArticleEditSuggestion> selectByAuthorId(@Param("authorId") Long authorId,
                                                 @Param("status") Integer status);
}
