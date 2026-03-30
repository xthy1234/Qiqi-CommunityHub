package com.gcs.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.gcs.entity.UserSignIn;
import java.util.Map;

/**
 * 签到服务
 */
public interface SignInService extends IService<UserSignIn> {
    
    /**
     * 用户签到
     * @param userId 用户 ID
     * @return 签到结果（包含获得积分、连续天数等）
     */
    Map<String, Object> doSignIn(Long userId);
    
    /**
     * 检查用户今日是否已签到
     * @param userId 用户 ID
     * @return true-已签到，false-未签到
     */
    boolean hasSignedInToday(Long userId);
    
    /**
     * 获取用户的连续签到天数
     * @param userId 用户 ID
     * @return 连续签到天数
     */
    int getSignInStreak(Long userId);
}
