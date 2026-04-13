package com.gcs.service.impl;


import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.gcs.dao.ArticleDao;
import com.gcs.dao.CommentDao;
import com.gcs.dao.ReportDao;
import com.gcs.dao.UserDao;
import com.gcs.entity.Article;
import com.gcs.entity.Comment;
import com.gcs.entity.Report;
import com.gcs.entity.User;
import com.gcs.enums.AuditStatus;
import com.gcs.enums.CommentStatus;
import com.gcs.service.DashboardStatsService;
import com.gcs.vo.ArticleAuditOverviewVO;
import com.gcs.vo.DashboardStatsVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 仪表盘统计服务实现
 */
@Slf4j
@Service("dashboardStatsService")
public class DashboardStatsServiceImpl implements DashboardStatsService {

    @Autowired
    private UserDao userDao;
    
    @Autowired
    private ArticleDao articleDao;
    
    @Autowired
    private CommentDao commentDao;
    
    @Autowired
    private ReportDao reportDao;

    @Override
    public DashboardStatsVO getDashboardStats() {
        DashboardStatsVO stats = new DashboardStatsVO();
        
        LocalDate today = LocalDate.now();
        LocalDate yesterday = today.minusDays(1);
        
        LocalDateTime todayStart = today.atStartOfDay();
        LocalDateTime yesterdayStart = yesterday.atStartOfDay();
        LocalDateTime todayEnd = todayStart.plusDays(1);
        LocalDateTime yesterdayEnd = todayStart;
        
        stats.setTotalUsers(getTotalUsers());
        stats.setTodayNewUsers(getTodayNewUsers(todayStart, todayEnd));
        stats.setYesterdayNewUsers(getYesterdayNewUsers(yesterdayStart, yesterdayEnd));
        stats.setUserGrowthRate(calculateGrowthRate(stats.getTodayNewUsers(), stats.getYesterdayNewUsers()));
        
        stats.setTotalArticles(getTotalArticles());
        stats.setTodayNewArticles(getTodayNewArticles(todayStart, todayEnd));
        stats.setYesterdayNewArticles(getYesterdayNewArticles(yesterdayStart, yesterdayEnd));
        stats.setArticleGrowthRate(calculateGrowthRate(stats.getTodayNewArticles(), stats.getYesterdayNewArticles()));
        
        stats.setPendingAuditArticles(getPendingAuditArticles());
        stats.setPendingReports(getPendingReports());
        stats.setTotalComments(getTotalComments());
        stats.setTodayActiveUsers(getTodayActiveUsers(todayStart, todayEnd));
        
        return stats;
    }
    
    @Override
    public ArticleAuditOverviewVO getArticleAuditOverview() {
        ArticleAuditOverviewVO overview = new ArticleAuditOverviewVO();
        
        LocalDate today = LocalDate.now();
        LocalDateTime todayStart = today.atStartOfDay();
        LocalDateTime todayEnd = todayStart.plusDays(1);
        
        overview.setPendingCount(getPendingAuditArticles());
        overview.setTodayApproved(getTodayApprovedArticles(todayStart, todayEnd));
        overview.setTodayRejected(getTodayRejectedArticles(todayStart, todayEnd));
        overview.setTotalCount(getTotalArticles());
        
        return overview;
    }
    
    private Integer getTotalUsers() {
        try {
            Long count = userDao.selectCount(new QueryWrapper<>());
            return count != null ? count.intValue() : 0;
        } catch (Exception e) {
            log.error("获取总用户数失败", e);
            return 0;
        }
    }
    
    private Integer getTodayNewUsers(LocalDateTime start, LocalDateTime end) {
        try {
            QueryWrapper<User> wrapper = new QueryWrapper<>();
            wrapper.ge("create_time", start)
                   .lt("create_time", end);
            Long count = userDao.selectCount(wrapper);
            return count != null ? count.intValue() : 0;
        } catch (Exception e) {
            log.error("获取今日新增用户数失败", e);
            return 0;
        }
    }
    
    private Integer getYesterdayNewUsers(LocalDateTime start, LocalDateTime end) {
        try {
            QueryWrapper<User> wrapper = new QueryWrapper<>();
            wrapper.ge("create_time", start)
                   .lt("create_time", end);
            Long count = userDao.selectCount(wrapper);
            return count != null ? count.intValue() : 0;
        } catch (Exception e) {
            log.error("获取昨日新增用户数失败", e);
            return 0;
        }
    }
    
    private Integer getTotalArticles() {
        try {
            QueryWrapper<Article> wrapper = new QueryWrapper<>();
            wrapper.eq("audit_status", AuditStatus.APPROVED.getCode());
            Long count = articleDao.selectCount(wrapper);
            return count != null ? count.intValue() : 0;
        } catch (Exception e) {
            log.error("获取总文章数失败", e);
            return 0;
        }
    }
    
    private Integer getTodayNewArticles(LocalDateTime start, LocalDateTime end) {
        try {
            QueryWrapper<Article> wrapper = new QueryWrapper<>();
            wrapper.eq("audit_status", AuditStatus.APPROVED.getCode())
                   .ge("create_time", start)
                   .lt("create_time", end);
            Long count = articleDao.selectCount(wrapper);
            return count != null ? count.intValue() : 0;
        } catch (Exception e) {
            log.error("获取今日新增文章数失败", e);
            return 0;
        }
    }
    
    private Integer getYesterdayNewArticles(LocalDateTime start, LocalDateTime end) {
        try {
            QueryWrapper<Article> wrapper = new QueryWrapper<>();
            wrapper.eq("audit_status", AuditStatus.APPROVED.getCode())
                   .ge("create_time", start)
                   .lt("create_time", end);
            Long count = articleDao.selectCount(wrapper);
            return count != null ? count.intValue() : 0;
        } catch (Exception e) {
            log.error("获取昨日新增文章数失败", e);
            return 0;
        }
    }
    
    private Integer getPendingAuditArticles() {
        try {
            QueryWrapper<Article> wrapper = new QueryWrapper<>();
            wrapper.eq("audit_status", AuditStatus.PENDING.getCode());
            Long count = articleDao.selectCount(wrapper);
            return count != null ? count.intValue() : 0;
        } catch (Exception e) {
            log.error("获取待审核文章数失败", e);
            return 0;
        }
    }
    
    private Integer getPendingReports() {
        try {
            QueryWrapper<Report> wrapper = new QueryWrapper<>();
            wrapper.eq("review_status", AuditStatus.PENDING.getCode());
            Long count = reportDao.selectCount(wrapper);
            return count != null ? count.intValue() : 0;
        } catch (Exception e) {
            log.error("获取待处理举报数失败", e);
            return 0;
        }
    }
    
    private Integer getTotalComments() {
        try {
            QueryWrapper<Comment> wrapper = new QueryWrapper<>();
            wrapper.eq("status", CommentStatus.SHOW.getCode());
            Long count = commentDao.selectCount(wrapper);
            return count != null ? count.intValue() : 0;
        } catch (Exception e) {
            log.error("获取总评论数失败", e);
            return 0;
        }
    }
    
    private Integer getTodayActiveUsers(LocalDateTime start, LocalDateTime end) {
        try {
            QueryWrapper<User> wrapper = new QueryWrapper<>();
            wrapper.isNotNull("last_login_time")
                   .ge("last_login_time", start)
                   .lt("last_login_time", end);
            Long count = userDao.selectCount(wrapper);
            return count != null ? count.intValue() : 0;
        } catch (Exception e) {
            log.error("获取今日活跃用户数失败", e);
            return 0;
        }
    }
    
    private Integer getTodayApprovedArticles(LocalDateTime start, LocalDateTime end) {
        try {
            QueryWrapper<Article> wrapper = new QueryWrapper<>();
            wrapper.eq("audit_status", AuditStatus.APPROVED.getCode())
                   .ge("update_time", start)
                   .lt("update_time", end);
            Long count = articleDao.selectCount(wrapper);
            return count != null ? count.intValue() : 0;
        } catch (Exception e) {
            log.error("获取今日通过文章数失败", e);
            return 0;
        }
    }
    
    private Integer getTodayRejectedArticles(LocalDateTime start, LocalDateTime end) {
        try {
            QueryWrapper<Article> wrapper = new QueryWrapper<>();
            wrapper.eq("audit_status", AuditStatus.REJECTED.getCode())
                   .ge("update_time", start)
                   .lt("update_time", end);
            Long count = articleDao.selectCount(wrapper);
            return count != null ? count.intValue() : 0;
        } catch (Exception e) {
            log.error("获取今日拒绝文章数失败", e);
            return 0;
        }
    }
    
    private Double calculateGrowthRate(Integer today, Integer yesterday) {
        if (yesterday == null || yesterday == 0) {
            if (today != null && today > 0) {
                return 100.0;
            }
            return 0.0;
        }
        
        double rate = ((double) (today - yesterday) / yesterday) * 100;
        return Math.round(rate * 100.0) / 100.0;
    }
}
