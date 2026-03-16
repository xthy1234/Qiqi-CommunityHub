package com.gcs.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.gcs.entity.Follow;
import com.gcs.vo.FollowUserVO;

import org.apache.ibatis.annotations.Param;
import java.util.List;

/**
 * 用户关注关系数据访问接口
 * 提供关注相关的数据库操作
 * @author 
 * @email 
 * @date 2026-03-16
 */
public interface FollowDao extends BaseMapper<Follow> {
	
    /**
     * 查询用户的关注列表（该用户关注的人）
     * @param page 分页对象
     * @param userId 用户ID
     * @return 关注列表
     */
    List<FollowUserVO> selectFollowingList(IPage<FollowUserVO> page, @Param("userId") Long userId);

    /**
     * 查询用户的粉丝列表（关注该用户的人）
     * @param page 分页对象
     * @param userId 用户ID
     * @return 粉丝列表
     */
    List<FollowUserVO> selectFollowerList(IPage<FollowUserVO> page, @Param("userId") Long userId);

    /**
     * 查询关注状态
     * @param followerId 关注者 ID
     * @param targetIds 目标用户ID 列表
     * @return 关注状态列表
     */
    List<Follow> selectFollowStatus(@Param("followerId") Long followerId, 
                                    @Param("targetIds") List<Long> targetIds);

    /**
     * 检查是否互相关注
     * @param userId1 用户ID1
     * @param userId2 用户ID2
     * @return 是否互相关注
     */
    boolean checkMutualFollow(@Param("userId1") Long userId1, 
                             @Param("userId2") Long userId2);

    /**
     * 统计用户关注数量
     * @param userId 用户ID
     * @return 关注数量
     */
    Integer countFollowing(@Param("userId") Long userId);

    /**
     * 统计用户粉丝数量
     * @param userId 用户ID
     * @return 粉丝数量
     */
    Integer countFollowers(@Param("userId") Long userId);
}
