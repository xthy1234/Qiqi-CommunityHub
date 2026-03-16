package com.gcs.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.gcs.entity.Token;

import org.apache.ibatis.annotations.Param;
import java.util.List;

/**
 * Token数据访问接口
 * 提供Token相关的数据库操作
 * @author 
 * @email 
 * @date 2026-04-16 11:22:10
 */
public interface TokenDao extends BaseMapper<Token> {
	
    /**
     * 查询Token列表视图
     * @param wrapper 查询条件包装器
     * @return Token列表
     */
    List<Token> selectListView(@Param("ew") Wrapper<Token> wrapper);

    /**
     * 分页查询Token列表视图
     * @param page 分页对象
     * @param wrapper 查询条件包装器
     * @return Token列表
     */
    List<Token> selectListView(IPage<Token> page, @Param("ew") Wrapper<Token> wrapper);

    /**
     * 根据用户 ID 和角色查询 Token
     * @param userId 用户 ID
     * @param role 用户角色
     * @return Token 信息
     */
    Token selectByUserIdAndRole(@Param("userId") Long userId, @Param("roleId") Long role);

    /**
     * 根据访问令牌查询Token
     * @param accessToken 访问令牌
     * @return Token信息
     */
    Token selectByAccessToken(@Param("accessToken") String accessToken);

    /**
     * 清理过期Token
     * @return 清理数量
     */
    int cleanExpiredTokens();

    /**
     * 批量删除用户Token
     * @param userId 用户ID
     * @return 删除结果
     */
    int deleteByUserId(@Param("userId") Long userId);

    /**
     * 插入 Token（处理 inet 类型转换）
     * @param token Token 实体
     * @return 插入结果
     */
    int insert(Token token);

    /**
     * 更新 Token（处理 inet 类型转换）
     * @param token Token 实体
     * @return 更新结果
     */
    int updateById(Token token);
}
