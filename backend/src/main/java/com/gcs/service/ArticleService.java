package com.gcs.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.extension.service.IService;
import com.gcs.entity.Article;
import com.gcs.entity.view.ArticleView;
import com.gcs.utils.PageUtils;
import com.gcs.vo.ArticleSearchVO;

import java.util.List;
import java.util.Map;

/**
 * 帖子服务接口
 *
 * @author
 * @date 2026-04-16
 */
public interface ArticleService extends IService<Article> {

    /**
     * 分页查询帖子列表
     *
     * @param params 查询参数
     * @return 分页结果
     */
    PageUtils queryPage(Map<String, Object> params);

    /**
     * 分页查询文章列表视图
     */
    IPage<ArticleView> selectListViewPage(IPage<ArticleView> page, Wrapper<Article> queryWrapper);

    /**
     * 查询帖子列表视图
     *
     * @param queryWrapper 查询条件包装器
     * @return 帖子视图列表
     */
    List<ArticleView> selectListView(Wrapper<Article> queryWrapper);

    /**
     * 查询单个帖子视图
     *
     * @param queryWrapper 查询条件包装器
     * @return 帖子视图
     */
    ArticleView selectView(Wrapper<Article> queryWrapper);

    /**
     * 根据 ID 查询帖子视图
     * @param id 帖子 ID
     * @return 帖子视图
     */
    ArticleView selectViewById(Long id);

    /**
     * 带条件的分页查询帖子列表
     *
     * @param params 查询参数
     * @param queryWrapper 查询条件包装器
     * @return 分页结果
     */
    PageUtils queryPage(Map<String, Object> params, Wrapper<Article> queryWrapper);

    /**
     * 统计值查询
     *
     * @param params 查询参数
     * @param queryWrapper 查询条件包装器
     * @return 统计结果列表
     */
    List<Map<String, Object>> selectValue(Map<String, Object> params, Wrapper<Article> queryWrapper);

    /**
     * 时间统计值查询
     *
     * @param params 查询参数
     * @param queryWrapper 查询条件包装器
     * @return 时间统计数据列表
     */
    List<Map<String, Object>> selectTimeStatValue(Map<String, Object> params, Wrapper<Article> queryWrapper);

    /**
     * 分组查询
     *
     * @param params 查询参数
     * @param queryWrapper 查询条件包装器
     * @return 分组结果列表
     */
    List<Map<String, Object>> selectGroup(Map<String, Object> params, Wrapper<Article> queryWrapper);

    /**
     * 全文搜索文章
     *
     * @param params 查询参数（包含 keyword、categoryId、startDate、endDate、limit 等）
     * @return 搜索结果列表（按相关度排序）
     */
    List<ArticleSearchVO> searchByFullText(Map<String, Object> params);
}
