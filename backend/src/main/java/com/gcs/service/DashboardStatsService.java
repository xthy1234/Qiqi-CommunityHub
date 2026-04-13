package com.gcs.service;

import com.gcs.vo.ArticleAuditOverviewVO;
import com.gcs.vo.DashboardStatsVO;

/**
 * 仪表盘统计服务接口
 */
public interface DashboardStatsService {
    
    /**
     * 获取仪表盘统计数据
     *
     * @return 统计数据
     */
    DashboardStatsVO getDashboardStats();
    
    /**
     * 获取文章审核概览统计数据
     *
     * @return 文章审核概览数据
     */
    ArticleAuditOverviewVO getArticleAuditOverview();
}
