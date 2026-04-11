package com.gcs.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.gcs.entity.ArticleVideoAnnotation;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * 文章视频注释 DAO
 * @author 
 * @date 2026-04-05
 */
@Mapper
public interface ArticleVideoAnnotationDao extends BaseMapper<ArticleVideoAnnotation> {

    /**
     * 根据文章ID和视频URL查询注释列表
     * @param articleId 文章ID
     * @param videoUrl 视频URL
     * @return 注释列表
     */
    List<ArticleVideoAnnotation> selectByArticleAndVideo(@Param("articleId") Long articleId, 
                                                          @Param("videoUrl") String videoUrl);

    /**
     * 根据文章ID查询所有注释
     * @param articleId 文章ID
     * @return 注释列表
     */
    List<ArticleVideoAnnotation> selectByArticleId(@Param("articleId") Long articleId);

    /**
     * 分页查询注释列表（支持多条件）
     * @param params 查询参数
     * @return 注释列表
     */
    List<ArticleVideoAnnotation> selectAnnotationList(@Param("params") Map<String, Object> params);
}
