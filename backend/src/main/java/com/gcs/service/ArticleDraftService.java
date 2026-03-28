package com.gcs.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.gcs.entity.ArticleDraft;
import java.util.Map;

public interface ArticleDraftService extends IService<ArticleDraft> {
    
    /**
     * 创建草稿（返回草稿 ID）
     * @param userId 用户 ID
     * @param articleId 文章 ID（可为空，表示新文章）
     * @param title 标题
     * @param content 内容
     * @param coverUrl 封面图片 URL（可选）
     * @param categoryId 分类 ID（可选）
     * @return 草稿 ID
     */
    Long createDraft(Long userId, Long articleId, String title, Map<String, Object> content, 
                     String coverUrl, Long categoryId);
    
    /**
     * 自动保存草稿
     * @param articleId 文章 ID
     * @param userId 用户 ID
     * @param content 内容
     * @param title 标题（可选）
     */
    void autoSave(Long articleId, Long userId, Map<String, Object> content, String title);
    
    /**
     * 获取草稿
     * @param articleId 文章 ID
     * @param userId 用户 ID
     * @return 草稿实体
     */
    ArticleDraft getDraft(Long articleId, Long userId);
    
    /**
     * 根据草稿 ID 获取草稿
     * @param draftId 草稿 ID
     * @return 草稿实体
     */
    ArticleDraft getDraftById(Long draftId);
    
    /**
     * 删除草稿
     * @param articleId 文章 ID
     * @param userId 用户 ID
     */
    void deleteDraft(Long articleId, Long userId);
    
    /**
     * 删除草稿（根据 ID）
     * @param draftId 草稿 ID
     */
    void deleteDraftById(Long draftId);
    
    /**
     * 检查是否存在草稿
     * @param articleId 文章 ID
     * @param userId 用户 ID
     * @return true/false
     */
    boolean hasDraft(Long articleId, Long userId);
    
    /**
     * 查询用户的所有草稿列表（分页）
     * @param userId 用户 ID
     * @param page 页码
     * @param limit 每页数量
     * @return 草稿列表
     */
    com.baomidou.mybatisplus.core.metadata.IPage<ArticleDraft> getDraftsByUserId(
        Long userId, Integer page, Integer limit);
}
