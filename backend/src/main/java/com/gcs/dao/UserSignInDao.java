package com.gcs.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.gcs.entity.UserSignIn;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.time.LocalDate;

/**
 * 用户签到记录 DAO
 */
@Mapper
public interface UserSignInDao extends BaseMapper<UserSignIn> {
    
    /**
     * 查询用户指定日期的签到记录
     */
    UserSignIn selectByUserIdAndDate(@Param("userId") Long userId, 
                                      @Param("signDate") LocalDate signDate);
    
    /**
     * 查询用户最近一次签到记录
     */
    UserSignIn selectLastSignIn(@Param("userId") Long userId);
}
