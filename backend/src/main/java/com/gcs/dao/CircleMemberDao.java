package com.gcs.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.gcs.entity.CircleMember;
import org.apache.ibatis.annotations.Param;
import java.util.List;

/**
 * 圈子成员数据访问接口
 * @author
 * @date 2026-03-17
 */
public interface CircleMemberDao extends BaseMapper<CircleMember> {

    /**
     * 查询用户在圈子中的角色
     * @param circleId 圈子 ID
     * @param userId 用户 ID
     * @return 角色类型
     */
    Integer getUserRoleInCircle(@Param("circleId") Long circleId, @Param("userId") Long userId);

    /**
     * 查询圈子成员数量
     * @param circleId 圈子 ID
     * @return 成员数量
     */
    Integer getMemberCount(@Param("circleId") Long circleId);

    /**
     * 查询用户加入的所有圈子 ID
     * @param userId 用户 ID
     * @return 圈子 ID 列表
     */
    List<Long> getJoinedCircleIds(@Param("userId") Long userId);

    /**
     * 检查用户是否是圈子成员
     * @param circleId 圈子 ID
     * @param userId 用户 ID
     * @return 是否存在
     */
    Boolean isMember(@Param("circleId") Long circleId, @Param("userId") Long userId);

    /**
     * 查询圈子成员列表
     * @param circleId 圈子 ID
     * @param role 角色（可选）
     * @return 成员列表
     */
    List<CircleMember> selectMemberList(@Param("circleId") Long circleId,
                                        @Param("role") Integer role);

    /**
     * 更新用户的最后阅读时间
     * @param userId 用户 ID
     * @param circleId 圈子 ID
     * @return 影响行数
     */
    int updateLastReadTime(@Param("userId") Long userId, @Param("circleId") Long circleId);
    
    /**
     * 查询圈子的所有成员 ID
     */
    List<Long> selectMemberIds(@Param("circleId") Long circleId);
    
    /**
     * 查询圈子的管理员和圈主 ID 列表
     */
    List<Long> getAdminAndOwnerIds(@Param("circleId") Long circleId);
    
    /**
     * 查询圈主 ID 列表
     */
    List<Long> getOwners(@Param("circleId") Long circleId);
}
