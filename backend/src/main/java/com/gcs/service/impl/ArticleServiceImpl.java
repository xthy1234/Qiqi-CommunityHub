package com.gcs.service.impl;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.gcs.enums.AuditStatus;
import com.gcs.vo.ArticleDetailVO;
import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;

import com.gcs.dao.ArticleDao;
import com.gcs.entity.Article;
import com.gcs.entity.view.ArticleView;
import com.gcs.service.ArticleService;
import com.gcs.service.ArticleVersionService;
import com.gcs.utils.PageUtils;
import com.gcs.utils.Query;
import com.gcs.vo.ArticleSearchVO;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/**
 * 帖子服务实现类
 * 提供帖子相关的业务逻辑处理
 * @author 
 * @date 2026-04-16
 */
@Slf4j
@Service("articleService")
public class ArticleServiceImpl extends ServiceImpl<ArticleDao, Article> implements ArticleService {

    @Autowired
    private ArticleVersionService articleVersionService;

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
    
    @Override
    public Article getArticleDetail(Long id) {
        if (id == null) {
            return null;
        }
        
        // 直接返回 ArticleView（包含联表数据）
        return selectViewById(id);
    }
    
    @Override
    public void insertArticle(Article article) {
        // 确保 publish_time 有值
        if (article.getPublishTime() == null && article.getAuditStatus() == AuditStatus.APPROVED) {
            article.setPublishTime(new java.util.Date());
        }
        
        // 初始化默认值
        if (article.getEditMode() == null) {
            article.setEditMode(0);
        }
        
        if (article.getAuditReply() == null) {
            article.setAuditReply("");
        }
        
        this.save(article);
    }
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveWithMinorVersion(Long articleId, Long userId, String title,
                                     Map<String, Object> content, String changeSummary) {
        Article article = this.getById(articleId);
        if (article == null) {
            throw new IllegalArgumentException("文章不存在");
        }

        // 1️⃣ 先保存当前版本为快照（小版本）
        articleVersionService.createMinorVersion(article, userId,
                changeSummary != null ? changeSummary : "手动保存");

        // 2️⃣ 更新文章内容
        article.setTitle(title);
        article.setContent(content);
//        article.setUpdateTime(LocalDateTime.now());
        this.updateById(article);

        log.info("保存文章并创建小版本，articleId: {}", articleId);
    }

}
