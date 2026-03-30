package com.gcs.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.gcs.dao.ArticleViewLogDao;
import com.gcs.entity.Article;
import com.gcs.entity.ArticleViewLog;
import com.gcs.service.ArticleService;
import com.gcs.service.ArticleViewLogService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

/**
 * 文章浏览记录服务实现
 */
@Slf4j
@Service("articleViewLogService")
public class ArticleViewLogServiceImpl extends ServiceImpl<ArticleViewLogDao, ArticleViewLog> 
        implements ArticleViewLogService {

    @Autowired
    private ArticleViewLogDao articleViewLogDao;
    
    @Autowired
    private ArticleService articleService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean incrementViewCount(Long articleId, String viewerKey) {
        if (articleId == null || viewerKey == null) {
            return false;
        }

        try {

            int inserted = articleViewLogDao.insertIgnore(articleId, viewerKey);
            

            if (inserted > 0) {
                Article article = articleService.getById(articleId);
                if (article != null) {
                    Integer currentCount = article.getViewCount();
                    if (currentCount == null) {
                        currentCount = 0;
                    }
                    article.setViewCount(currentCount + 1);
                    articleService.updateById(article);
                    
                    log.info("增加文章浏览量成功，articleId: {}, viewerKey: {}, 当前浏览量：{}", 
                            articleId, viewerKey, article.getViewCount());
                    return true;
                }
            } else {
                log.debug("浏览记录已存在，跳过计数，articleId: {}, viewerKey: {}", articleId, viewerKey);
            }
            
            return inserted > 0;
        } catch (Exception e) {
            log.error("增加文章浏览量失败，articleId: {}, viewerKey: {}", articleId, viewerKey, e);
            throw new RuntimeException("增加浏览量失败", e);
        }
    }
}
