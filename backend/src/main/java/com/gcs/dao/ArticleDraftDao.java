package com.gcs.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.gcs.entity.ArticleDraft;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ArticleDraftDao extends BaseMapper<ArticleDraft> {
    
    /**
     * 根据文章 ID 和用户 ID 查询草稿
     */
    ArticleDraft selectByArticleAndUser(@Param("articleId") Long articleId, 
                                        @Param("userId") Long userId);
    
    /**
     * 检查是否存在草稿
     */
    boolean existsByArticleAndUser(@Param("articleId") Long articleId, 
                                   @Param("userId") Long userId);
    
    /**
     * 查询用户的所有草稿（分页）
     */
    java.util.List<ArticleDraft> selectDraftsByUserId(
        @Param("userId") Long userId, 
        com.baomidou.mybatisplus.core.metadata.IPage<ArticleDraft> page);
}
