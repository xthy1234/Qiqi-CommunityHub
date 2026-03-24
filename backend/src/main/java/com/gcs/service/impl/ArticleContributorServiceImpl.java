package com.gcs.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.gcs.dao.ArticleContributorDao;
import com.gcs.dao.UserDao;
import com.gcs.entity.ArticleContributor;
import com.gcs.entity.User;
import com.gcs.service.ArticleContributorService;
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

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addContribution(Long articleId, Long userId, int lines) {
        if (lines <= 0) {
            return;
        }

        // 查询是否已存在贡献记录
        ArticleContributor contributor = articleContributorDao.selectByArticleAndUser(articleId, userId);

        if (contributor == null) {
            // 新增贡献记录
            contributor = new ArticleContributor();
            contributor.setArticleId(articleId);
            contributor.setUserId(userId);
            contributor.setContributedLines(lines);
            articleContributorDao.insert(contributor);
        } else {
            // 累加贡献行数
            articleContributorDao.addContributedLines(articleId, userId, lines);
        }

        log.info("添加贡献记录成功，articleId: {}, userId: {}, lines: {}", articleId, userId, lines);
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
            contributorInfo.put("contributedLines", c.getContributedLines());
            contributorInfo.put("contributionTime", c.getCreateTime());
            
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
