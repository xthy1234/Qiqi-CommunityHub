package com.gcs.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.gcs.entity.Follow;
import com.gcs.utils.PageUtils;

import java.util.List;
import java.util.Map;

/**
 * 用户关注关系服务接口
 * 提供关注相关的业务操作
 * @author 
 * @date 2026-03-16
 */
public interface FollowService extends IService<Follow> {

    /**
     * 关注或取关用户
     *
     * @param followerId 关注者 ID
     * @param followingId 被关注者 ID
     * @param action 操作类型：follow-关注，unfollow-取关
     * @return 操作结果
     */
    boolean followOrUnfollow(Long followerId, Long followingId, String action);

    /**
     * 获取用户的关注列表
     *
     * @param params 查询参数
     * @param userId 用户ID
     * @return 分页结果
     */
    PageUtils getFollowingList(Map<String, Object> params, Long userId);

    /**
     * 获取用户的粉丝列表
     *
     * @param params 查询参数
     * @param userId 用户ID
     * @return 分页结果
     */
    PageUtils getFollowerList(Map<String, Object> params, Long userId);

    /**
     * 查询关注状态
     *
     * @param followerId 关注者 ID
     * @param targetIds 目标用户ID 列表
     * @return 关注状态 Map
     */
    Map<Long, Boolean> getFollowStatus(Long followerId, List<Long> targetIds);

    /**
     * 检查是否互相关注
     *
     * @param userId1 用户ID1
     * @param userId2 用户ID2
     * @return 是否互相关注
     */
    boolean isFriend(Long userId1, Long userId2);

    /**
     * 统计用户关注数量
     *
     * @param userId 用户ID
     * @return 关注数量
     */
    Integer countFollowing(Long userId);

    /**
     * 统计用户粉丝数量
     *
     * @param userId 用户ID
     * @return 粉丝数量
     */
    Integer countFollowers(Long userId);

    /**
     * 检查是否已关注
     *
     * @param followerId 关注者 ID
     * @param followingId 被关注者 ID
     * @return 是否已关注
     */
    boolean isFollowing(Long followerId, Long followingId);
}
