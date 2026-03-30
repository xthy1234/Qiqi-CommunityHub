package com.gcs.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.gcs.entity.ArticleViewLog;

/**
 * 文章浏览记录服务
 */
public interface ArticleViewLogService extends IService<ArticleViewLog> {
    
    /**
     * 增加文章浏览量（带去重）
     * @param articleId 文章 ID
     * @param viewerKey 访问者标识
     * @return 是否成功增加（true 表示新增，false 表示已存在）
     */
    boolean incrementViewCount(Long articleId, String viewerKey);
}
