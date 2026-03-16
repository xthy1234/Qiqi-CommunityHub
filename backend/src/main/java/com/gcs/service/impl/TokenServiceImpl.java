package com.gcs.service.impl;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.List;
import java.util.Objects;

import com.gcs.enums.CommonStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import com.gcs.dao.TokenDao;
import com.gcs.entity.Token;
import com.gcs.service.TokenService;
import com.gcs.utils.PageUtils;
import com.gcs.utils.Query;

import lombok.extern.slf4j.Slf4j;

/**
 * Token服务实现类
 * 提供Token相关的业务逻辑处理
 * @author 
 * @date 2026-04-16
 */
@Slf4j
@Service("tokenService")
public class TokenServiceImpl extends ServiceImpl<TokenDao, Token> implements TokenService {

    /**
     * Token有效期（小时）
     */
    private static final int TOKEN_EXPIRE_HOURS = 24;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        validateParams(params);
        
        IPage<Token> tokenPage = new Query<Token>(params).getPage();
        IPage<Token> resultPage = this.page(tokenPage, new QueryWrapper<>());
        
        return new PageUtils(resultPage);
    }

    @Override
    public List<Token> selectListView(Wrapper<Token> wrapper) {
        validateWrapper(wrapper);
        return baseMapper.selectListView(wrapper);
    }

    @Override
    public PageUtils queryPage(Map<String, Object> params, Wrapper<Token> wrapper) {
        validateQueryParams(params, wrapper);
        
        IPage<Token> tokenPage = new Query<Token>(params).getPage();
        List<Token> tokens = baseMapper.selectListView(tokenPage, wrapper);
        tokenPage.setRecords(tokens);
        
        return new PageUtils(tokenPage);
    }

    @Override
    public String generateToken(Long userId, String tokenValue,Long roleId,String ip ,String deviceInfo) {
        if (userId == null || !StringUtils.hasText(tokenValue)) {
            throw new IllegalArgumentException("参数不能为空");
        }
        if (roleId == null) {
            throw new IllegalArgumentException("用户角色不能为空");
        }

        // 先使旧 token 无效
        invalidateTokenByUserId(userId);

        // 创建新 token
        Token newToken = new Token();
        newToken.setUserId(userId);
        newToken.setRoleId(roleId);
        newToken.setAccessToken(tokenValue);
        newToken.setExpireTime(LocalDateTime.now().plusHours(24));
        newToken.setStatus(CommonStatus.ENABLED); // 设置为有效状态
        if (ip != null && !ip.isEmpty()) {
            newToken.setIpAddress(ip);
        }
        if (deviceInfo != null && !deviceInfo.isEmpty()) {
            newToken.setDeviceInfo(deviceInfo);
        }
        
        boolean saved = this.save(newToken);
        return saved ? tokenValue : null;
    }

    @Override
    public boolean invalidateToken(String token) {
        if (!StringUtils.hasText(token)) {
            throw new IllegalArgumentException("token 不能为空");
        }

        Token tokenEntity = baseMapper.selectByAccessToken(token);
        if (tokenEntity == null) {
            return false;
        }

        tokenEntity.setStatus(CommonStatus.DISABLED); // 设置为无效状态
        tokenEntity.setUpdateTime(LocalDateTime.now());
        
        return this.updateById(tokenEntity);
    }

    @Override
    public boolean invalidateTokenByUserId(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("用户 ID 不能为空");
        }

        Token token = baseMapper.selectById(userId);
        if (token == null) {
            return false;
        }

        token.setStatus(CommonStatus.DISABLED); // 设置为无效状态
        token.setUpdateTime(LocalDateTime.now());
        
        return this.updateById(token);
    }

    @Override
    public int cleanExpiredTokens() {
        try {
            int cleanedCount = baseMapper.cleanExpiredTokens();
            log.info("清理过期Token数量: {}", cleanedCount);
            return cleanedCount;
        } catch (Exception e) {
            log.error("清理过期Token失败", e);
            return 0;
        }
    }

    @Override
    public boolean deleteUserTokens(Long userId) {
        if (userId == null) {
            return false;
        }

        try {
            int deletedCount = baseMapper.deleteByUserId(userId);
            log.info("删除用户Token数量: {}, userId: {}", deletedCount, userId);
            return true;
        } catch (Exception e) {
            log.error("删除用户Token失败, userId: {}", userId, e);
            return false;
        }
    }

    @Override
    public boolean isTokenValid(String accessToken) {
        if (!StringUtils.hasText(accessToken)) {
            return false;
        }

        Token token = baseMapper.selectByAccessToken(accessToken);
        return token != null && token.getStatus() == CommonStatus.ENABLED && // 0 表示有效
               token.getExpireTime().isAfter(LocalDateTime.now());
    }

    @Override
    public Token validateAndGetToken(String accessToken) {
        if (!StringUtils.hasText(accessToken)) {
            return null;
        }

        Token token = baseMapper.selectByAccessToken(accessToken);
        if (token == null || token.getStatus() != CommonStatus.ENABLED || 
            token.getExpireTime().isBefore(LocalDateTime.now())) {
            return null;
        }

        return token;
    }

    @Override
    public boolean refreshToken(String accessToken) {
        if (!StringUtils.hasText(accessToken)) {
            return false;
        }

        Token token = baseMapper.selectByAccessToken(accessToken);
        if (token == null || token.getStatus() != CommonStatus.ENABLED) {
            return false;
        }

        // 检查是否已过期
        if (token.getExpireTime().isBefore(LocalDateTime.now())) {
            return false;
        }

        // 刷新过期时间（延长 24 小时）
        token.setExpireTime(LocalDateTime.now().plusHours(TOKEN_EXPIRE_HOURS));
        token.setUpdateTime(LocalDateTime.now());
        
        return this.updateById(token);
    }

    // ==================== 私有验证方法 ====================

    /**
     * 验证查询参数
     */
    private void validateParams(Map<String, Object> params) {
        if (CollectionUtils.isEmpty(params)) {
            throw new IllegalArgumentException("查询参数不能为空");
        }
    }

    /**
     * 验证查询条件包装器
     */
    private void validateWrapper(Wrapper<Token> wrapper) {
        if (Objects.isNull(wrapper)) {
            throw new IllegalArgumentException("查询条件不能为空");
        }
    }

    /**
     * 验证查询参数和条件
     */
    private void validateQueryParams(Map<String, Object> params, Wrapper<Token> wrapper) {
        validateParams(params);
        validateWrapper(wrapper);
    }

    /**
     * 验证 Token 生成参数
     */
    private void validateTokenParams(Long userId, String account, Long roleId) {
        if (userId == null) {
            throw new IllegalArgumentException("用户 ID 不能为空");
        }
        if (!StringUtils.hasText(account)) {
            throw new IllegalArgumentException("用户账号不能为空");
        }
        if (roleId == null) {
            throw new IllegalArgumentException("用户角色不能为空");
        }
    }
}
