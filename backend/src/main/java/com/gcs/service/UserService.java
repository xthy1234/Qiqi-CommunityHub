package com.gcs.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.extension.service.IService;
import com.gcs.utils.PageUtils;
import com.gcs.entity.User;
import com.gcs.entity.view.UserView;
import com.gcs.vo.UserPublicProfileVO;

import java.util.List;
import java.util.Map;

/**
 * 用户服务接口
 * 提供用户相关的业务操作
 * @author 
 * @date 2026-04-16
 */
public interface UserService extends IService<User> {

    /**
     * 分页查询用户列表
     *
     * @param params 查询参数
     * @return 分页结果
     */
    PageUtils queryPage(Map<String, Object> params);

    /**
     * 查询用户列表视图
     *
     * @param queryWrapper 查询条件包装器
     * @return 用户视图列表
     */
    List<UserView> selectListView(Wrapper<User> queryWrapper);

    /**
     * 查询单个用户视图
     *
     * @param queryWrapper 查询条件包装器
     * @return 用户视图
     */
    UserView selectView(Wrapper<User> queryWrapper);

    /**
     * 带条件的分页查询用户列表
     *
     * @param params 查询参数
     * @param queryWrapper 查询条件包装器
     * @return 分页结果
     */
    PageUtils queryPage(Map<String, Object> params, Wrapper<User> queryWrapper);

    /**
     * 用户注册
     *
     * @param user 用户信息
     * @return 注册结果
     */
    boolean registerUser(User user);

    /**
     * 用户登录
     *
     * @param account 账号
     * @param password 密码
     * @param loginIp 登录 IP
     * @return 登录结果
     */
    User loginUser(String account, String password, String loginIp);

    /**
     * 管理员登录
     *
     * @param account 账号
     * @param password 密码
     * @return 用户信息
     */
    User adminLogin(String account, String password);

    /**
     * 根据账号查询用户（用于管理员登录）
     *
     * @param account 账号
     * @return 用户信息
     */
    User getUserByAccount(String account);

    /**
     * 根据昵称查询用户
     *
     * @param nickname 昵称
     * @return 用户信息
     */
    User getUserByNickname(String nickname);

    /**
     * 根据手机号查询用户
     *
     * @param phone 手机号
     * @return 用户信息
     */
    User getUserByPhone(String phone);

    /**
     * 根据邮箱查询用户
     *
     * @param email 邮箱
     * @return 用户信息
     */
    User getUserByEmail(String email);

    /**
     * 更新用户密码
     *
     * @param userId 用户 ID
     * @param newPassword 新密码
     * @return 更新结果
     */
    boolean updatePassword(Long userId, String newPassword);

    /**
     * 重置用户密码
     *
     * @param account 账号
     * @return 重置结果
     */
    boolean resetPassword(String account);

    /**
     * 修改密码
     *
     * @param userId 用户 ID
     * @param oldPassword 旧密码
     * @param newPassword 新密码
     * @return 修改结果
     */
    boolean changePassword(Long userId, String oldPassword, String newPassword);

    /**
     * 更新用户头像
     *
     * @param userId 用户 ID
     * @param avatarUrl 头像 URL
     * @return 更新结果
     */
    boolean updateAvatar(Long userId, String avatarUrl);

    /**
     * 统计用户数量
     *
     * @param status 状态
     * @return 用户数量
     */
    Integer countUsers(Integer status);

    /**
     * 检查账号唯一性
     *
     * @param account 账号
     * @param excludeUserId 排除的用户 ID
     * @return 是否唯一
     */
    boolean isAccountUnique(String account, Long excludeUserId);



    /**
     * 检查手机号唯一性
     *
     * @param phone 手机号
     * @param excludeUserId 排除的用户 ID
     * @return 是否唯一
     */
    boolean isPhoneUnique(String phone, Long excludeUserId);

    /**
     * 检查邮箱唯一性
     *
     * @param email 邮箱
     * @param excludeUserId 排除的用户 ID
     * @return 是否唯一
     */
    boolean isEmailUnique(String email, Long excludeUserId);

    /**
     * 检查昵称唯一性
     *
     * @param nickname 昵称
     * @param excludeUserId 排除的用户 ID
     * @return 是否唯一
     */
    boolean isNicknameUnique(String nickname, Long excludeUserId);

    /**
     * 创建用户（包含管理员）
     *
     * @param user 用户信息
     * @return 创建结果
     */
    boolean createUser(User user);

    /**
     * 更新用户信息
     *
     * @param user 用户信息
     * @return 更新结果
     */
    boolean updateUser(User user);

    /**
     * 启用/禁用用户
     *
     * @param userId 用户ID
     * @param status 状态（0:禁用 1:启用）
     * @return 操作结果
     */
    boolean updateStatus(Long userId, Integer status);
    public void disableUser(Long userId);

    /**
     * 获取用户公开主页信息
     *
     * @param userId 用户ID
     * @param currentUserId 当前登录用户ID（可选，用于判断关注状态）
     * @return 用户公开主页信息
     */
    UserPublicProfileVO getPublicProfile(Long userId, Long currentUserId);
}

