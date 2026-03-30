package com.gcs.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.gcs.entity.ArticleViewLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 文章浏览记录 DAO
 */
@Mapper
public interface ArticleViewLogDao extends BaseMapper<ArticleViewLog> {
    
    /**
     * 插入浏览记录（忽略重复）
     * @param articleId 文章 ID
     * @param viewerKey 访问者标识
     * @return 影响的行数（1 表示插入成功，0 表示重复）
     */
    int insertIgnore(@Param("articleId") Long articleId, 
                     @Param("viewerKey") String viewerKey);
}
