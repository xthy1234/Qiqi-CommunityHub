package com.gcs.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.gcs.dao.ArticleDao;
import com.gcs.dao.ArticleVersionDao;
import com.gcs.dao.UserDao;
import com.gcs.entity.Article;
import com.gcs.entity.ArticleVersion;
import com.gcs.entity.User;
import com.gcs.service.ArticleVersionService;
import com.gcs.vo.UserSimpleVO;
import com.gcs.vo.ArticleVersionSimpleVO;
import com.gcs.vo.ArticleVersionVO;
import com.gcs.converter.ArticleVersionConverter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;

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
    
    @Autowired
    private UserDao userDao;
    
    @Autowired
    private ArticleVersionConverter articleVersionConverter;

    @Override
    public Integer createVersion(Article article, Long operatorId, String changeSummary) {

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
    public List<ArticleVersionSimpleVO> getVersionHistoryWithDetails(Long articleId) {
        // 查询版本历史
        List<ArticleVersion> versions = articleVersionDao.selectByArticleId(articleId);
        
        //  获取当前版本号
        Article article = articleDao.selectById(articleId);
        Integer currentVersion = (article != null) ? article.getCurrentVersion() : null;
        
        //  使用 Converter 转换为简略 VO
        List<ArticleVersionSimpleVO> voList = versions.stream()
            .map(version -> {
                ArticleVersionSimpleVO simpleVO = buildSimpleVO(version);
                //  设置 isCurrent
                simpleVO.setIsCurrent(currentVersion != null && version.getVersion().equals(currentVersion));
                return simpleVO;
            })
            .collect(Collectors.toList());
        
        return voList;
    }

    @Override
    public ArticleVersionVO getVersionDetailWithVo(Long articleId, Integer version) {
        // 查询版本详情
        ArticleVersion articleVersion = articleVersionDao.selectByVersion(articleId, version);
        if (articleVersion == null) {
            throw new IllegalArgumentException("版本不存在");
        }
        
        //  获取当前版本号
        Article article = articleDao.selectById(articleId);
        Integer currentVersion = (article != null) ? article.getCurrentVersion() : null;
        
        //  使用 Converter 构建详细 VO
        ArticleVersionVO vo = buildDetailVO(articleVersion, articleId);
        //  设置 isCurrent
        vo.setIsCurrent(currentVersion != null && articleVersion.getVersion().equals(currentVersion));
        
        return vo;
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
    public Map<String, Object> compareVersionsWithVo(Long articleId, Integer versionA, Integer versionB) {
        ArticleVersion va = articleVersionDao.selectByVersion(articleId, versionA);
        ArticleVersion vb = articleVersionDao.selectByVersion(articleId, versionB);

        if (va == null || vb == null) {
            throw new IllegalArgumentException("版本不存在");
        }

        //  使用 Converter 转换为详细 VO
        ArticleVersionVO sourceVO = buildDetailVO(va, articleId);
        ArticleVersionVO targetVO = buildDetailVO(vb, articleId);

        // 构建返回结果
        Map<String, Object> result = new HashMap<>();
        result.put("sourceVersion", sourceVO);
        result.put("targetVersion", targetVO);

        return result;
    }

    /**
     * 构建简略 VO（用于列表展示）
     */
    private ArticleVersionSimpleVO buildSimpleVO(ArticleVersion version) {
        if (version == null) {
            return null;
        }
        
        //  第一步：使用 Converter 进行基础转换
        ArticleVersionSimpleVO vo = articleVersionConverter.toSimpleVO(version);
        
        //  第二步：补充操作人信息
        if (version.getOperatorId() != null) {
            User operator = userDao.selectById(version.getOperatorId());
            if (operator != null) {
                UserSimpleVO operatorVO = buildUserSimpleVO(operator);
                vo.setOperator(operatorVO);
            }
        }
        
        //  第三步：补充贡献者信息
        if (version.getContributorId() != null) {
            User contributor = userDao.selectById(version.getContributorId());
            if (contributor != null) {
                UserSimpleVO contributorVO = buildUserSimpleVO(contributor);
                vo.setContributor(contributorVO);
            }
        }
        
        //  第四步：判断是否为最新版本
        Integer maxVersion = articleVersionDao.getMaxVersion(version.getArticleId());
        vo.setIsLatest(version.getVersion().equals(maxVersion));
        
        //  isCurrent 由调用方设置（因为需要 article 的 currentVersion）
        
        return vo;
    }

    /**
     * 构建详细 VO（用于详情页和对比）
     */
    private ArticleVersionVO buildDetailVO(ArticleVersion version, Long articleId) {
        if (version == null) {
            return null;
        }
        
        //  第一步：使用 Converter 进行基础转换
        ArticleVersionVO vo = articleVersionConverter.toDetailVO(version);
        
        //  第二步：补充操作人信息
        if (version.getOperatorId() != null) {
            User operator = userDao.selectById(version.getOperatorId());
            if (operator != null) {
                UserSimpleVO operatorVO = buildUserSimpleVO(operator);
                vo.setOperator(operatorVO);
            }
        }
        
        //  第三步：补充贡献者信息
        if (version.getContributorId() != null) {
            User contributor = userDao.selectById(version.getContributorId());
            if (contributor != null) {
                UserSimpleVO contributorVO = buildUserSimpleVO(contributor);
                vo.setContributor(contributorVO);
            }
        }
        
        //  第四步：判断是否为最新版本
        Integer maxVersion = articleVersionDao.getMaxVersion(articleId);
        vo.setIsLatest(version.getVersion().equals(maxVersion));
        
        //  isCurrent 由调用方设置（因为需要 article 的 currentVersion）
        
        return vo;
    }

    /**
     * 构建 UserSimpleVO
     */
    private UserSimpleVO buildUserSimpleVO(User user) {
        if (user == null) {
            return null;
        }
        
        UserSimpleVO vo = new UserSimpleVO();
        vo.setId(user.getId());
        vo.setNickname(user.getNickname());
        vo.setAvatar(user.getAvatar());
        vo.setLastOnlineTime(user.getLastOnlineTime());
        
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void rollbackToVersion(Long articleId, Integer version, Long operatorId) {
        // 1️⃣ 查询目标版本
        ArticleVersion targetVersion = articleVersionDao.selectByVersion(articleId, version);
        if (targetVersion == null) {
            throw new IllegalArgumentException("版本不存在");
        }

        // 2️⃣ 获取当前文章和最新版本
        Article article = articleDao.selectById(articleId);
        if (article == null) {
            throw new IllegalArgumentException("文章不存在");
        }
        
        List<ArticleVersion> versions = getVersionHistory(articleId);
        if (versions == null || versions.isEmpty()) {
            throw new IllegalArgumentException("文章没有版本记录");
        }
        ArticleVersion latestVersion = versions.get(0);
        
        log.info("开始回滚，articleId: {}, 从版本 {} ({}.{}) 回滚到内容版本 {} ({}.{})", 
                articleId, 
                latestVersion.getVersion(), latestVersion.getMajorVersion(), latestVersion.getMinorVersion(),
                targetVersion.getVersion(), targetVersion.getMajorVersion(), targetVersion.getMinorVersion());

        // 3️⃣ 更新文章内容为目标版本的内容
        article.setTitle(targetVersion.getTitle());
        article.setContent(targetVersion.getContent());
        
        // 4️⃣ 使用 createMinorVersion 创建回滚版本
        //  createMinorVersion 会自动基于当前最大版本号计算，不会冲突
        String changeSummary = "回滚到版本 " + targetVersion.getMajorVersion() + "." + targetVersion.getMinorVersion();
        Integer rollbackVersion = createMinorVersion(article, operatorId, changeSummary);
        
        // 5️⃣ 更新文章的 currentVersion
        article.setCurrentVersion(rollbackVersion);
        articleDao.updateById(article);

        log.info("文章回滚成功，articleId: {}, 回滚目标：{}, 新版本号：{} (自动计算)", 
                articleId, version, rollbackVersion);
    }

    @Override
    public Integer createMinorVersion(Article article, Long operatorId, String changeSummary) {
        // 计算新版本号
        int[] versions = calculateNextVersion(article.getId(), false);
        Integer majorVersion = versions[0];
        Integer minorVersion = versions[1];

        // 获取全局版本号（自增）
        Integer nextGlobalVersion = articleVersionDao.getMaxVersion(article.getId()) + 1;

        ArticleVersion version = new ArticleVersion();
        version.setArticleId(article.getId());
        version.setVersion(nextGlobalVersion);
        version.setTitle(article.getTitle());
        version.setContent(article.getContent());
        version.setVersionType(0); // 小版本
        version.setMajorVersion(majorVersion);
        version.setMinorVersion(minorVersion);
        version.setChangeSummary(changeSummary);
        version.setOperatorId(operatorId);
        //  默认贡献者为操作者（作者自己）
        version.setContributorId(operatorId);

        baseMapper.insert(version);

        log.info("创建小版本成功，articleId: {}, version: {}.{}",
                article.getId(), majorVersion, minorVersion);

        return nextGlobalVersion;
    }

    @Override
    public Integer createMajorVersion(Article article, Long operatorId, String changeSummary) {
        // 计算新版本号
        int[] versions = calculateNextVersion(article.getId(), true);
        Integer majorVersion = versions[0];
        Integer minorVersion = versions[1];

        // 获取全局版本号（自增）
        Integer nextGlobalVersion = articleVersionDao.getMaxVersion(article.getId()) + 1;

        ArticleVersion version = new ArticleVersion();
        version.setArticleId(article.getId());
        version.setVersion(nextGlobalVersion);
        version.setTitle(article.getTitle());
        version.setContent(article.getContent());
        version.setVersionType(1); // 大版本
        version.setMajorVersion(majorVersion);
        version.setMinorVersion(minorVersion);
        version.setChangeSummary(changeSummary);
        version.setOperatorId(operatorId);
        //  默认贡献者为操作者（作者自己）
        version.setContributorId(operatorId);

        baseMapper.insert(version);

        log.info("创建大版本成功，articleId: {}, version: {}.{}",
                article.getId(), majorVersion, minorVersion);

        return nextGlobalVersion;
    }

    /**
     * 创建小版本（指定贡献者）
     */
    public Integer createMinorVersion(Article article, Long operatorId, String changeSummary, Long contributorId) {
        // 计算新版本号
        int[] versions = calculateNextVersion(article.getId(), false);
        Integer majorVersion = versions[0];
        Integer minorVersion = versions[1];

        // 获取全局版本号（自增）
        Integer nextGlobalVersion = articleVersionDao.getMaxVersion(article.getId()) + 1;

        ArticleVersion version = new ArticleVersion();
        version.setArticleId(article.getId());
        version.setVersion(nextGlobalVersion);
        version.setTitle(article.getTitle());
        version.setContent(article.getContent());
        version.setVersionType(0); // 小版本
        version.setMajorVersion(majorVersion);
        version.setMinorVersion(minorVersion);
        version.setChangeSummary(changeSummary);
        version.setOperatorId(operatorId);
        //  设置实际贡献者
        version.setContributorId(contributorId);

        baseMapper.insert(version);

        log.info("创建小版本成功，articleId: {}, version: {}.{}, contributorId: {}",
                article.getId(), majorVersion, minorVersion, contributorId);

        return nextGlobalVersion;
    }

    /**
     * 创建大版本（指定贡献者）
     */
    public Integer createMajorVersion(Article article, Long operatorId, String changeSummary, Long contributorId) {
        // 计算新版本号
        int[] versions = calculateNextVersion(article.getId(), true);
        Integer majorVersion = versions[0];
        Integer minorVersion = versions[1];

        // 获取全局版本号（自增）
        Integer nextGlobalVersion = articleVersionDao.getMaxVersion(article.getId()) + 1;

        ArticleVersion version = new ArticleVersion();
        version.setArticleId(article.getId());
        version.setVersion(nextGlobalVersion);
        version.setTitle(article.getTitle());
        version.setContent(article.getContent());
        version.setVersionType(1); // 大版本
        version.setMajorVersion(majorVersion);
        version.setMinorVersion(minorVersion);
        version.setChangeSummary(changeSummary);
        version.setOperatorId(operatorId);
        //  设置实际贡献者
        version.setContributorId(contributorId);

        baseMapper.insert(version);

        log.info("创建大版本成功，articleId: {}, version: {}.{}, contributorId: {}",
                article.getId(), majorVersion, minorVersion, contributorId);

        return nextGlobalVersion;
    }

    @Override
    public int[] calculateNextVersion(Long articleId, boolean isMajor) {
        if (isMajor) {
            // 大版本：major+1, minor=0
            Integer maxMajor = articleVersionDao.selectMaxMajorVersion(articleId);
            Integer newMajor = (maxMajor == null ? 0 : maxMajor) + 1;
            return new int[]{newMajor, 0};
        } else {
            // 小版本：major 不变，minor+1
            Integer maxMajor = articleVersionDao.selectMaxMajorVersion(articleId);
            Integer currentMajor = (maxMajor == null ? 1 : maxMajor);

            Integer maxMinor = articleVersionDao.selectMaxMinorVersion(articleId, currentMajor);
            Integer newMinor = (maxMinor == null ? 0 : maxMinor) + 1;

            return new int[]{currentMajor, newMinor};
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteVersion(Long articleId, Integer version, Long operatorId) {
        // 1️⃣ 查询目标版本
        ArticleVersion targetVersion = articleVersionDao.selectByVersion(articleId, version);
        if (targetVersion == null) {
            throw new IllegalArgumentException("版本不存在");
        }

        // 2️⃣ 检查是否为最新版本
        Integer maxVersion = articleVersionDao.getMaxVersion(articleId);
        if (version.equals(maxVersion)) {
            throw new IllegalArgumentException("不能删除最新版本，请先回滚到其他版本");
        }

        // 3️⃣ 执行软删除
        targetVersion.setIsDeleted(true);
        baseMapper.updateById(targetVersion);

        log.info("删除文章版本成功，articleId: {}, version: {}, operatorId: {}", 
                articleId, version, operatorId);
    }

}
