package com.gcs.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.gcs.entity.Article;

import java.util.List;
import java.util.Map;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import org.apache.ibatis.annotations.Param;
import com.gcs.entity.view.ArticleView;
import com.gcs.vo.ArticleSearchVO;

/**
 * 帖子
 * 
 * @author 
 * @email 
 * @date 2026-04-16 11:22:09
 */
public interface ArticleDao extends BaseMapper<Article> {

    List<ArticleView> selectListView(@Param("ew") Wrapper<Article> wrapper);

    IPage<ArticleView> selectListView(IPage<ArticleView> page, @Param("ew") Wrapper<Article> wrapper);

    ArticleView selectView(@Param("ew") Wrapper<Article> wrapper);

    List<Map<String, Object>> selectValue(@Param("params") Map<String, Object> params,
                                          @Param("ew") Wrapper<Article> wrapper);

    List<Map<String, Object>> selectTimeStatValue(@Param("params") Map<String, Object> params,
                                                  @Param("ew") Wrapper<Article> wrapper);

    List<Map<String, Object>> selectGroup(@Param("params") Map<String, Object> params,
                                          @Param("ew") Wrapper<Article> wrapper);

    /**
     * 根据 ID 查询文章视图
     * @param params 参数（包含 id）
     * @return 文章视图
     */
    ArticleView selectViewById(@Param("params") Map<String, Object> params);

    /**
     * 全文搜索文章（基于 PostgreSQL tsvector）
     * @param params 查询参数（包含 keyword）
     * @return 搜索结果列表（按相关度排序）
     */
    List<ArticleSearchVO> searchByFullText(@Param("params") Map<String, Object> params);

}
