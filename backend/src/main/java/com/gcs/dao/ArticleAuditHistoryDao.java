package com.gcs.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.gcs.entity.ArticleAuditHistory;
import org.apache.ibatis.annotations.Mapper;

/**
 * 文章审核历史 DAO
 */
@Mapper
public interface ArticleAuditHistoryDao extends BaseMapper<ArticleAuditHistory> {
}
