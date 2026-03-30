package com.gcs.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.gcs.dao.ArticleContributorDao;
import com.gcs.dao.UserDao;
import com.gcs.entity.ArticleContributor;
import com.gcs.entity.User;
import com.gcs.service.ArticleContributorService;
import com.gcs.service.PointsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * 文章贡献者服务实现类
 */
@Slf4j
@Service("articleContributorService")
public class ArticleContributorServiceImpl extends ServiceImpl<ArticleContributorDao, ArticleContributor> 
        implements ArticleContributorService {

    @Autowired
    private ArticleContributorDao articleContributorDao;

    @Autowired
    private UserDao userDao;
    
    @Autowired
    private PointsService pointsService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    @Deprecated
    public void addContribution(Long articleId, Long userId, int lines) {
        if (lines <= 0) {
            return;
        }


        ArticleContributor contributor = articleContributorDao.selectByArticleAndUser(articleId, userId);

        if (contributor == null) {

            contributor = new ArticleContributor();
            contributor.setArticleId(articleId);
            contributor.setUserId(userId);
            contributor.setAddedLines(lines);
            contributor.setModifiedLines(0);
            contributor.setDeletedLines(0);
            articleContributorDao.insert(contributor);
            

            pointsService.addPoints(userId, "post_article", articleId, "参与编辑文章");
        } else {

            articleContributorDao.addContributedLines(articleId, userId, lines);
            

            pointsService.addPoints(userId, "edit_article", articleId, "继续编辑文章");
        }

        log.info("添加贡献记录成功（旧方式），articleId: {}, userId: {}, lines: {}", articleId, userId, lines);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addDetailedContribution(Long articleId, Long userId, 
                                       Integer addedLines, Integer modifiedLines, Integer deletedLines) {
        if ((addedLines == null || addedLines <= 0) && 
            (modifiedLines == null || modifiedLines <= 0) && 
            (deletedLines == null || deletedLines <= 0)) {
            return;
        }


        Integer added = addedLines != null ? addedLines : 0;
        Integer modified = modifiedLines != null ? modifiedLines : 0;
        Integer deleted = deletedLines != null ? deletedLines : 0;


        articleContributorDao.addDetailedContribution(articleId, userId, added, modified, deleted);
        

        boolean isFirstEdit = checkIsFirstContribution(articleId, userId);
        
        if (isFirstEdit) {
            // 首次编辑，按发布文章标准
            pointsService.addPoints(userId, "post_article", articleId, 
                    String.format("编辑文章（+%d 行，~%d 行，-%d 行）", added, modified, deleted));
        } else {
            // 继续编辑，按编辑文章标准
            pointsService.addPoints(userId, "edit_article", articleId, 
                    String.format("编辑文章（+%d 行，~%d 行，-%d 行）", added, modified, deleted));
        }

        log.info("添加详细贡献记录成功，articleId: {}, userId: {}, added: {}, modified: {}, deleted: {}", 
                articleId, userId, added, modified, deleted);
    }

    /**
     * 检查是否是首次贡献
     */
    private boolean checkIsFirstContribution(Long articleId, Long userId) {
        ArticleContributor contributor = articleContributorDao.selectByArticleAndUser(articleId, userId);
        return contributor == null;
    }

    /**
     * 计算贡献分数
     */
    private int calculateContributionScore(Integer added, Integer modified, Integer deleted) {
        int score = 0;
        score += added != null ? added * 2 : 0;      // 每新增一行得 2 分
        score += modified != null ? modified * 1 : 0; // 每修改一行得 1 分
        score += deleted != null ? deleted * 1 : 0;   // 每删除一行得 1 分
        return score;
    }

    @Override
    public List<Map<String, Object>> getContributors(Long articleId) {
        List<ArticleContributor> contributors = articleContributorDao.selectByArticleId(articleId);
        
        List<Map<String, Object>> result = new ArrayList<>();
        for (ArticleContributor c : contributors) {
            User user = userDao.selectById(c.getUserId());
            
            Map<String, Object> contributorInfo = new HashMap<>();
            contributorInfo.put("userId", c.getUserId());
            contributorInfo.put("nickname", user != null ? user.getNickname() : "未知用户");
            contributorInfo.put("avatar", user != null ? user.getAvatar() : "");
            contributorInfo.put("addedLines", c.getAddedLines() != null ? c.getAddedLines() : 0);
            contributorInfo.put("modifiedLines", c.getModifiedLines() != null ? c.getModifiedLines() : 0);
            contributorInfo.put("deletedLines", c.getDeletedLines() != null ? c.getDeletedLines() : 0);
            contributorInfo.put("score", c.getScore() != null ? c.getScore() : 0);
            contributorInfo.put("lastContributedAt", c.getLastContributedAt());
            
            result.add(contributorInfo);
        }
        
        return result;
    }

    @Override
    public long countContributors(Long articleId) {
        QueryWrapper<ArticleContributor> wrapper = new QueryWrapper<>();
        wrapper.eq("article_id", articleId);
        return baseMapper.selectCount(wrapper);
    }
}
