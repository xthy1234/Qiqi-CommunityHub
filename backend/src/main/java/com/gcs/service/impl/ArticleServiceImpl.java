package com.gcs.service.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;

import com.gcs.dao.ArticleDao;
import com.gcs.entity.Article;
import com.gcs.entity.view.ArticleView;
import com.gcs.service.ArticleService;
import com.gcs.utils.PageUtils;
import com.gcs.utils.Query;
import com.gcs.vo.ArticleSearchVO;

import lombok.extern.slf4j.Slf4j;

/**
 * 帖子服务实现类
 * 提供帖子相关的业务逻辑处理
 * @author 
 * @date 2026-04-16
 */
@Slf4j
@Service("articleService")
public class ArticleServiceImpl extends ServiceImpl<ArticleDao, Article> implements ArticleService {

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<Article> articlePage = new Query<Article>(params).getPage();
        IPage<Article> resultPage = this.page(articlePage, new QueryWrapper<>());
        
        return new PageUtils(resultPage);
    }

    @Override
    public List<ArticleView> selectListView(Wrapper<Article> queryWrapper) {
        return baseMapper.selectListView(queryWrapper);
    }

    @Override
    public ArticleView selectView(Wrapper<Article> queryWrapper) {
        return baseMapper.selectView(queryWrapper);
    }

    @Override
    public ArticleView selectViewById(Long id) {
        if (id == null) {
            return null;
        }
        Map<String, Object> params = new HashMap<>();
        params.put("id", id);
        return baseMapper.selectViewById(params);
    }

    @Override
    public PageUtils queryPage(Map<String, Object> params, Wrapper<Article> queryWrapper) {
        IPage<ArticleView> articlePage = new Query<ArticleView>(params).getPage();
        IPage<ArticleView> resultPage = baseMapper.selectListView(articlePage, queryWrapper);
        // 手动设置总记录数（解决 MyBatis-Plus 自动 COUNT 查询的问题）
        long totalCount = baseMapper.selectCount(queryWrapper);
        articlePage.setTotal(totalCount);
        return new PageUtils(resultPage);
    }

    @Override
    public IPage<ArticleView> selectListViewPage(IPage<ArticleView> page, Wrapper<Article> queryWrapper) {
        return baseMapper.selectListView(page, queryWrapper);
    }

    @Override
    public List<Map<String, Object>> selectValue(Map<String, Object> params, Wrapper<Article> queryWrapper) {
        return baseMapper.selectValue(params, queryWrapper);
    }

    @Override
    public List<Map<String, Object>> selectTimeStatValue(Map<String, Object> params, Wrapper<Article> queryWrapper) {
        return baseMapper.selectTimeStatValue(params, queryWrapper);
    }

    @Override
    public List<Map<String, Object>> selectGroup(Map<String, Object> params, Wrapper<Article> queryWrapper) {
        return baseMapper.selectGroup(params, queryWrapper);
    }

    @Override
    public List<ArticleSearchVO> searchByFullText(Map<String, Object> params) {
        return baseMapper.searchByFullText(params);
    }
}
