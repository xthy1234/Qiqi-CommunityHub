package com.gcs.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.gcs.dao.FollowDao;
import com.gcs.entity.Follow;
import com.gcs.service.FollowService;
import com.gcs.utils.PageUtils;
import com.gcs.vo.FollowUserVO;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 用户关注关系服务实现类
 */
@Service("followService")
public class FollowServiceImpl extends ServiceImpl<FollowDao, Follow> implements FollowService {

    @Override
    @Transactional
    public boolean followOrUnfollow(Long followerId, Long followingId, String action) {
        // 不能关注自己
        if (followerId.equals(followingId)) {
            throw new RuntimeException("不能关注自己");
        }

        // 检查被关注用户是否存在（可选）
        // 这里可以根据实际需求添加用户存在性检查

        QueryWrapper<Follow> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("follower_id", followerId)
                   .eq("following_id", followingId);

        Follow follow = this.getOne(queryWrapper);

        if ("follow".equalsIgnoreCase(action)) {
            // 关注操作
            if (follow == null) {
                // 不存在关注记录，创建新的关注
                follow = new Follow();
                follow.setFollowerId(followerId);
                follow.setFollowingId(followingId);
                follow.setStatus(0);  // 修改：0 表示关注中
                follow.setCreateTime(LocalDateTime.now());
                follow.setUpdateTime(LocalDateTime.now());
                return this.save(follow);
            } else if (follow.getStatus() == 1) {  // 修改：1 表示已取消关注
                // 已取消关注，重新关注
                follow.setStatus(0);  // 修改：恢复为关注中
                follow.setUpdateTime(LocalDateTime.now());
                return this.updateById(follow);
            } else {
                // 已经关注，无需操作
                return true;
            }
        } else if ("unfollow".equalsIgnoreCase(action)) {
            // 取关操作
            if (follow != null && follow.getStatus() == 0) {  // 修改：0 表示关注中
                follow.setStatus(1);  // 修改：1 表示已取消关注
                follow.setUpdateTime(LocalDateTime.now());
                return this.updateById(follow);
            }
            // 未关注，无需操作
            return true;
        } else {
            throw new RuntimeException("无效的操作类型");
        }
    }

    @Override
    public PageUtils getFollowingList(Map<String, Object> params, Long userId) {
        int page = params.get("page") != null ? Integer.parseInt(params.get("page").toString()) : 1;
        int limit = params.get("limit") != null ? Integer.parseInt(params.get("limit").toString()) : 20;

        IPage<FollowUserVO> pageObj = new Page<>(page, limit);
        List<FollowUserVO> list = baseMapper.selectFollowingList(pageObj, userId);
        
        // ✅ 修复：手动设置查询结果到 page 对象
        pageObj.setRecords(list);
        
        return new PageUtils(pageObj);
    }

    @Override
    public PageUtils getFollowerList(Map<String, Object> params, Long userId) {
        int page = params.get("page") != null ? Integer.parseInt(params.get("page").toString()) : 1;
        int limit = params.get("limit") != null ? Integer.parseInt(params.get("limit").toString()) : 20;

        IPage<FollowUserVO> pageObj = new Page<>(page, limit);
        List<FollowUserVO> list = baseMapper.selectFollowerList(pageObj, userId);
        
        // ✅ 修复：手动设置查询结果到 page 对象
        pageObj.setRecords(list);
        
        return new PageUtils(pageObj);
    }

    @Override
    public Map<Long, Boolean> getFollowStatus(Long followerId, List<Long> targetIds) {
        Map<Long, Boolean> statusMap = new HashMap<>();
        
        if (targetIds == null || targetIds.isEmpty()) {
            return statusMap;
        }

        List<Follow> follows = baseMapper.selectFollowStatus(followerId, targetIds);
        
        // 初始化所有目标用户为未关注
        for (Long targetId : targetIds) {
            statusMap.put(targetId, false);
        }
        
        // 更新已关注的用户（修改：0 表示关注中）
        for (Follow follow : follows) {
            if (follow.getStatus() == 0) {  // 修改：判断 0 而不是 1
                statusMap.put(follow.getFollowingId(), true);
            }
        }

        return statusMap;
    }

    @Override
    public boolean isFriend(Long userId1, Long userId2) {
        return baseMapper.checkMutualFollow(userId1, userId2);
    }

    @Override
    public Integer countFollowing(Long userId) {
        return baseMapper.countFollowing(userId);
    }

    @Override
    public Integer countFollowers(Long userId) {
        return baseMapper.countFollowers(userId);
    }

    @Override
    public boolean isFollowing(Long followerId, Long followingId) {
        QueryWrapper<Follow> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("follower_id", followerId)
                   .eq("following_id", followingId)
                   .eq("status", 0);  // 修改：0 表示关注中
        return this.count(queryWrapper) > 0;
    }
}
