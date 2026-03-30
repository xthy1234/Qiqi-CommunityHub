package com.gcs.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.gcs.dao.UserSignInDao;
import com.gcs.entity.User;
import com.gcs.entity.UserSignIn;
import com.gcs.service.SignInService;
import com.gcs.service.UserService;
import com.gcs.service.PointsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;

/**
 * 签到服务实现
 */
@Slf4j
@Service("signInService")
public class SignInServiceImpl extends ServiceImpl<UserSignInDao, UserSignIn> 
        implements SignInService {

    @Autowired
    private UserSignInDao userSignInDao;
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private PointsService pointsService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> doSignIn(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("用户 ID 不能为空");
        }
        
        LocalDate today = LocalDate.now();
        

        UserSignIn todaySignIn = userSignInDao.selectByUserIdAndDate(userId, today);
        if (todaySignIn != null) {
            throw new RuntimeException("今日已签到");
        }
        

        int streak = calculateSignInStreak(userId);
        

        int pointsEarned = calculateDailyBonus(streak);
        

        UserSignIn signIn = new UserSignIn();
        signIn.setUserId(userId);
        signIn.setSignDate(today);
        signIn.setPointsEarned(pointsEarned);
        this.save(signIn);
        

        pointsService.addPoints(userId, pointsEarned, "sign_in", null, "每日签到");
        

        User user = userService.getById(userId);
        if (user != null) {
            user.setSignInStreak(streak);
            userService.updateById(user);
        }
        

        Map<String, Object> result = new HashMap<>();
        result.put("pointsEarned", pointsEarned);
        result.put("streak", streak);
        result.put("message", getSignInMessage(streak));
        
        log.info("用户签到成功，userId: {}, 获得积分：{}, 连续天数：{}", userId, pointsEarned, streak);
        
        return result;
    }

    @Override
    public boolean hasSignedInToday(Long userId) {
        if (userId == null) {
            return false;
        }
        
        LocalDate today = LocalDate.now();
        UserSignIn todaySignIn = userSignInDao.selectByUserIdAndDate(userId, today);
        return todaySignIn != null;
    }

    @Override
    public int getSignInStreak(Long userId) {
        return calculateSignInStreak(userId);
    }

    /**
     * 计算连续签到天数
     */
    private int calculateSignInStreak(Long userId) {
        UserSignIn lastSignIn = userSignInDao.selectLastSignIn(userId);
        if (lastSignIn == null) {
            return 1;
        }
        
        LocalDate today = LocalDate.now();
        LocalDate lastSignDate = lastSignIn.getSignDate();
        

        long daysDiff = ChronoUnit.DAYS.between(lastSignDate, today);
        
        if (daysDiff <= 1) {

            User user = userService.getById(userId);
            if (user != null && user.getSignInStreak() != null) {
                return user.getSignInStreak() + 1;
            }
            return 1;
        } else {

            return 1;
        }
    }

    /**
     * 计算每日签到奖励
     */
    private int calculateDailyBonus(int streak) {
        int base = 10;
        
        if (streak >= 7) {
            return base + 20;
        } else if (streak >= 3) {
            return base + 10;
        } else if (streak >= 1) {
            return base;
        }
        
        return base;
    }

    /**
     * 获取签到提示信息
     */
    private String getSignInMessage(int streak) {
        if (streak >= 7) {
            return "恭喜！连续签到 7 天，获得额外奖励 20 积分！";
        } else if (streak >= 3) {
            return "不错哦！连续签到 3 天，获得额外奖励 10 积分！";
        } else {
            return "签到成功，获得 10 积分，继续加油！";
        }
    }
}
