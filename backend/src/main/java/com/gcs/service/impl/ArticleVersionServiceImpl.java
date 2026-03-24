package com.gcs.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.gcs.dao.ArticleDao;
import com.gcs.dao.ArticleVersionDao;
import com.gcs.entity.Article;
import com.gcs.entity.ArticleVersion;
import com.gcs.service.ArticleVersionService;
import com.gcs.utils.JsonDiffUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * 文章版本服务实现类
 */
@Slf4j
@Service("articleVersionService")
public class ArticleVersionServiceImpl extends ServiceImpl<ArticleVersionDao, ArticleVersion> 
        implements ArticleVersionService {

    @Autowired
    private ArticleVersionDao articleVersionDao;

    @Autowired
    private ArticleDao articleDao;

    @Override
    public Integer createVersion(Article article, Long operatorId, String changeSummary) {
        // 获取下一个版本号
        Integer nextVersion = articleVersionDao.getMaxVersion(article.getId()) + 1;

        // 创建版本记录
        ArticleVersion version = new ArticleVersion();
        version.setArticleId(article.getId());
        version.setVersion(nextVersion);
        version.setTitle(article.getTitle());
        version.setContent(article.getContent());
        version.setChangeSummary(changeSummary);
        version.setOperatorId(operatorId);

        baseMapper.insert(version);

        log.info("创建文章版本成功，articleId: {}, version: {}", article.getId(), nextVersion);
        return nextVersion;
    }

    @Override
    public List<ArticleVersion> getVersionHistory(Long articleId) {
        return articleVersionDao.selectByArticleId(articleId);
    }

    @Override
    public ArticleVersion getVersionDetail(Long articleId, Integer version) {
        return articleVersionDao.selectByVersion(articleId, version);
    }

    @Override
    public Map<String, Object> compareVersions(Long articleId, Integer versionA, Integer versionB) {
        ArticleVersion va = articleVersionDao.selectByVersion(articleId, versionA);
        ArticleVersion vb = articleVersionDao.selectByVersion(articleId, versionB);

        if (va == null || vb == null) {
            throw new IllegalArgumentException("版本不存在");
        }

        // 构建返回结果，包含两个版本的完整信息
        Map<String, Object> result = new HashMap<>();
        
        // 源版本（较早的版本）
        Map<String, Object> sourceVersion = new HashMap<>();
        sourceVersion.put("version", va.getVersion());
        sourceVersion.put("createdAt", va.getCreateTime());
        sourceVersion.put("operatorId", va.getOperatorId());
        sourceVersion.put("changeSummary", va.getChangeSummary());
        sourceVersion.put("content", va.getContent());
        
        // 目标版本（较新的版本）
        Map<String, Object> targetVersion = new HashMap<>();
        targetVersion.put("version", vb.getVersion());
        targetVersion.put("createdAt", vb.getCreateTime());
        targetVersion.put("operatorId", vb.getOperatorId());
        targetVersion.put("changeSummary", vb.getChangeSummary());
        targetVersion.put("content", vb.getContent());
        
        result.put("sourceVersion", sourceVersion);
        result.put("targetVersion", targetVersion);

        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void rollbackToVersion(Long articleId, Integer version, Long operatorId) {
        ArticleVersion targetVersion = articleVersionDao.selectByVersion(articleId, version);
        if (targetVersion == null) {
            throw new IllegalArgumentException("版本不存在");
        }

        // 获取当前文章
        Article article = articleDao.selectById(articleId);
        if (article == null) {
            throw new IllegalArgumentException("文章不存在");
        }

        // 保存当前版本作为回滚前的快照
        String changeSummary = "回滚到版本 " + version;
        createVersion(article, operatorId, "回滚前备份：" + changeSummary);

        // 更新文章内容
        article.setTitle(targetVersion.getTitle());
        article.setContent(targetVersion.getContent());

        // 更新文章表
        articleDao.updateById(article);

        // 创建回滚后的新版本
        createVersion(article, operatorId, changeSummary);

        log.info("文章回滚成功，articleId: {}, targetVersion: {}", articleId, version);
    }
}
