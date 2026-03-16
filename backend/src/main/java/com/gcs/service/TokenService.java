package com.gcs.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.extension.service.IService;
import com.gcs.entity.Token;
import com.gcs.utils.PageUtils;

import java.util.List;
import java.util.Map;

/**
 * Token服务接口
 * 提供Token相关的业务操作
 * @author 
 * @date 2026-04-16
 */
public interface TokenService extends IService<Token> {

    /**
     * 分页查询Token列表
     *
     * @param params 查询参数
     * @return 分页结果
     */
    PageUtils queryPage(Map<String, Object> params);

    /**
     * 查询Token列表视图
     *
     * @param queryWrapper 查询条件包装器
     * @return Token列表
     */
    List<Token> selectListView(Wrapper<Token> queryWrapper);

    /**
     * 带条件的分页查询Token列表
     *
     * @param params 查询参数
     * @param queryWrapper 查询条件包装器
     * @return 分页结果
     */
    PageUtils queryPage(Map<String, Object> params, Wrapper<Token> queryWrapper);

    /**
     * 生成访问令牌
     *
     * @param userId 用户 ID


     * @return 访问令牌
     */
    String generateToken(Long userId, String tokenValue,Long roleId,String  ip,String  device);
    boolean invalidateTokenByUserId(Long userId);
    /**
     * 验证并获取Token实体
     *
     * @param accessToken 访问令牌
     * @return Token实体
     */

    Token validateAndGetToken(String accessToken);

    /**
     * 刷新Token过期时间
     *
     * @param accessToken 访问令牌
     * @return 刷新结果
     */
    boolean refreshToken(String accessToken);

    /**
     * 使Token失效
     *
     * @param accessToken 访问令牌
     * @return 失效结果
     */
    boolean invalidateToken(String accessToken);

    /**
     * 清理过期Token
     *
     * @return 清理数量
     */
    int cleanExpiredTokens();

    /**
     * 删除用户所有Token
     *
     * @param userId 用户ID
     * @return 删除结果
     */
    boolean deleteUserTokens(Long userId);

    /**
     * 检查Token是否有效
     *
     * @param accessToken 访问令牌
     * @return 是否有效
     */
    boolean isTokenValid(String accessToken);
}
