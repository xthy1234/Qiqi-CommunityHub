package com.gcs.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.gcs.dao.PointsTransactionDao;
import com.gcs.dao.UserDao;
import com.gcs.entity.PointsRule;
import com.gcs.entity.PointsTransaction;
import com.gcs.entity.User;
import com.gcs.service.PointsRuleService;
import com.gcs.service.PointsService;
import com.gcs.utils.PageUtils;
import com.gcs.utils.Query;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 积分服务实现
 */
@Slf4j
@Service("pointsService")
public class PointsServiceImpl extends ServiceImpl<PointsTransactionDao, PointsTransaction> 
        implements PointsService {

    @Autowired
    private PointsRuleService pointsRuleService;
    
    @Autowired
    private UserDao userDao;
    
    @Autowired
    private PointsTransactionDao pointsTransactionDao;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addPoints(Long userId, String ruleKey, Long sourceId, String description) {
        if (userId == null || ruleKey == null || ruleKey.isEmpty()) {
            throw new IllegalArgumentException("用户 ID 和规则标识不能为空");
        }
        

        PointsRule rule = pointsRuleService.getRule(ruleKey);
        if (rule == null || !rule.getEnabled()) {
            log.warn("积分规则 {} 未启用或不存在", ruleKey);
            return;
        }
        
        Integer basePoints = rule.getBasePoints();
        if (basePoints == null || basePoints <= 0) {
            log.warn("积分规则 {} 的积分值为无效值", ruleKey);
            return;
        }
        

        Integer dailyLimit = rule.getDailyLimit();
        if (dailyLimit != null && dailyLimit > 0) {
            int todayCount = countTodayByUserAndRule(userId, ruleKey);
            if (todayCount >= dailyLimit) {
                log.debug("用户 {} 今日已达到规则 {} 的上限 ({})", userId, ruleKey, dailyLimit);
                return;
            }
        }
        

        updatePointsAtomically(userId, basePoints, ruleKey, sourceId, description, true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addPoints(Long userId, Integer amount, String source, Long sourceId, String description) {
        if (userId == null || amount == null || amount <= 0) {
            throw new IllegalArgumentException("参数错误");
        }
        
        updatePointsAtomically(userId, amount, source, sourceId, description, true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deductPoints(Long userId, Integer amount, String source, Long sourceId, String description) {
        if (userId == null || amount == null || amount <= 0) {
            throw new IllegalArgumentException("参数错误");
        }
        
        Integer currentPoints = getUserPoints(userId);
        if (currentPoints < amount) {
            throw new RuntimeException("积分不足，当前积分：" + currentPoints);
        }
        
        updatePointsAtomically(userId, amount, source, sourceId, description, false);
    }

    @Override
    public Integer getUserPoints(Long userId) {
        if (userId == null) {
            return 0;
        }
        
        User user = userDao.selectById(userId);
        return user != null ? (user.getPoints() != null ? user.getPoints() : 0) : 0;
    }

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        Long userId = (Long) params.get("userId");
        Integer page = params.get("page") != null ? Integer.parseInt(params.get("page").toString()) : 1;
        Integer limit = params.get("limit") != null ? Integer.parseInt(params.get("limit").toString()) : 10;
        

        Page<PointsTransaction> pageObject = new Page<>(page, limit);
        
        QueryWrapper<PointsTransaction> wrapper = new QueryWrapper<>();
        if (userId != null) {
            wrapper.eq("user_id", userId);
        }
        wrapper.orderByDesc("create_time");
        

        IPage<PointsTransaction> resultPage = this.page(pageObject, wrapper);
        
        return new PageUtils(resultPage);
    }

    /**
     * 原子更新积分（核心方法）
     */
    private void updatePointsAtomically(Long userId, Integer amount, String source, 
                                       Long sourceId, String description, boolean isAdd) {

        int updated = userDao.incrementPoints(userId, isAdd ? amount : -amount);
        if (updated == 0) {
            throw new RuntimeException("更新用户积分失败，用户可能不存在");
        }
        

        User user = userDao.selectById(userId);
        Integer newBalance = user != null && user.getPoints() != null ? user.getPoints() : 0;
        

        PointsTransaction transaction = new PointsTransaction();
        transaction.setUserId(userId);
        transaction.setAmount(isAdd ? amount : -amount);
        transaction.setBalance(newBalance);
        transaction.setSource(source);
        transaction.setSourceId(sourceId);
        transaction.setDescription(description);
        transaction.setCreateTime(LocalDateTime.now());
        
        this.save(transaction);
        
        log.info("{}积分成功，userId: {}, rule: {}, amount: {}, balance: {}", 
                isAdd ? "增加" : "扣除", userId, source, amount, newBalance);
    }

    /**
     * 统计用户今日某规则的触发次数
     */
    private int countTodayByUserAndRule(Long userId, String ruleKey) {
        LocalDate today = LocalDate.now();
        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime endOfDay = today.plusDays(1).atStartOfDay();
        
        QueryWrapper<PointsTransaction> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId)
               .eq("source", ruleKey)
               .ge("create_time", startOfDay)
               .lt("create_time", endOfDay);
        
        return Math.toIntExact(this.baseMapper.selectCount(wrapper));
    }
}
