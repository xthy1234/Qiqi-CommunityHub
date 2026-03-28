package com.gcs.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.gcs.dao.ArticleDraftDao;
import com.gcs.entity.ArticleDraft;
import com.gcs.service.ArticleDraftService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service("articleDraftService")
public class ArticleDraftServiceImpl extends ServiceImpl<ArticleDraftDao, ArticleDraft> 
        implements ArticleDraftService {
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createDraft(Long userId, Long articleId, String title, Map<String, Object> content, 
                            String coverUrl, Long categoryId) {
        ArticleDraft draft = new ArticleDraft();
        draft.setUserId(userId);
        draft.setArticleId(articleId);
        draft.setTitle(title != null ? title : "未命名草稿");
        draft.setContent(content != null ? content : new HashMap<>());
        draft.setExtra(new HashMap<>()); // ✅ 提供默认空 Map，避免 null
        draft.setCoverUrl(coverUrl);
        draft.setCategoryId(categoryId);
        draft.setAutoSaveTime(LocalDateTime.now());
        
        baseMapper.insert(draft);
        log.info("创建草稿成功，draftId: {}, articleId: {}, userId: {}", draft.getId(), articleId, userId);
        return draft.getId();
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void autoSave(Long articleId, Long userId, Map<String, Object> content, String title) {
        ArticleDraft draft = baseMapper.selectByArticleAndUser(articleId, userId);
        
        if (draft == null) {
            // 如果草稿不存在，创建新草稿（兼容旧逻辑）
            createDraft(userId, articleId, title, content, null, null);
        } else {
            // 更新草稿
            draft.setContent(content);
            draft.setTitle(title);
            draft.setAutoSaveTime(LocalDateTime.now());
            baseMapper.updateById(draft);
        }
        
        log.info("自动保存草稿成功，articleId: {}, userId: {}", articleId, userId);
    }
    
    @Override
    public ArticleDraft getDraft(Long articleId, Long userId) {
        return baseMapper.selectByArticleAndUser(articleId, userId);
    }
    
    @Override
    public ArticleDraft getDraftById(Long draftId) {
        return baseMapper.selectById(draftId);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteDraft(Long articleId, Long userId) {
        ArticleDraft draft = baseMapper.selectByArticleAndUser(articleId, userId);
        if (draft != null) {
            baseMapper.deleteById(draft.getId());
            log.info("删除草稿成功，articleId: {}, userId: {}", articleId, userId);
        }
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteDraftById(Long draftId) {
        ArticleDraft draft = baseMapper.selectById(draftId);
        if (draft != null) {
            baseMapper.deleteById(draftId);
            log.info("删除草稿成功，draftId: {}", draftId);
        }
    }
    
    @Override
    public boolean hasDraft(Long articleId, Long userId) {
        return baseMapper.existsByArticleAndUser(articleId, userId);
    }
    
    @Override
    public IPage<ArticleDraft> getDraftsByUserId(Long userId, Integer page, Integer limit) {
        if (userId == null) {
            throw new IllegalArgumentException("用户 ID 不能为空");
        }
        
        IPage<ArticleDraft> articlePage = new Page<>(page, limit);
        List<ArticleDraft> drafts = baseMapper.selectDraftsByUserId(userId, articlePage);
        
        IPage<ArticleDraft> resultPage = new Page<>(page, limit);
        resultPage.setRecords(drafts);
        resultPage.setTotal(articlePage.getTotal());
        
        log.info("查询用户草稿列表成功，userId: {}, 草稿数：{}", userId, drafts.size());
        return resultPage;
    }
}
