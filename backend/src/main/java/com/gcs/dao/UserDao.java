package com.gcs.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.gcs.entity.User;
import com.gcs.entity.view.UserView;

import org.apache.ibatis.annotations.Param;
import java.util.List;

/**
 * 用户数据访问接口
 * 提供用户相关的数据库操作
 * @author 
 * @email 
 * @date 2026-04-16 11:22:10
 */
public interface UserDao extends BaseMapper<User> {
	
    /**
     * 查询用户列表视图
     * @param wrapper 查询条件包装器
     * @return 用户视图列表
     */
    List<UserView> selectListView(@Param("ew") Wrapper<User> wrapper);

    /**
     * 分页查询用户列表视图
     * @param page 分页对象
     * @param wrapper 查询条件包装器
     * @return 用户视图列表
     */
    List<UserView> selectListView(IPage<UserView> page, @Param("ew") Wrapper<User> wrapper);
	
    /**
     * 查询单个用户视图
     * @param wrapper 查询条件包装器
     * @return 用户视图
     */
    UserView selectView(@Param("ew") Wrapper<User> wrapper);

    /**
     * 根据账号查询用户
     * @param account 用户账号
     * @return 用户信息
     */
    User selectByAccount(@Param("account") String account);

    /**
     * 根据手机号查询用户
     * @param phone 手机号
     * @return 用户信息
     */
    User selectByPhone(@Param("phone") String phone);

    /**
     * 根据邮箱查询用户
     * @param email 邮箱
     * @return 用户信息
     */
    User selectByEmail(@Param("email") String email);

    /**
     * 统计用户数量
     * @param status 状态
     * @return 用户数量
     */
    Integer countUsers(@Param("status") Integer status);

    /**
     * 更新用户最后登录信息
     * @param userId 用户 ID
     * @param lastLoginIp 登录 IP
     * @return 更新结果
     */
    int updateLastLoginInfo(@Param("userId") Long userId, 
                           @Param("lastLoginIp") String lastLoginIp);
}
