package com.gcs.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.gcs.dao.FollowDao;
import com.gcs.entity.Follow;
import com.gcs.enums.CommonStatus;
import com.gcs.service.FollowService;
import com.gcs.service.NotificationService;
import com.gcs.dao.UserDao;
import com.gcs.entity.User;
import com.gcs.enums.NotificationType;
import com.gcs.utils.NotificationBuilder;
import com.gcs.utils.PageUtils;
import com.gcs.vo.FollowUserVO;
import com.gcs.vo.UserSimpleVO;

import org.springframework.beans.factory.annotation.Autowired;
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

    @Autowired
    private NotificationService notificationService;
    
    @Autowired
    private UserDao userDao;

    @Override
    @Transactional
    public boolean followOrUnfollow(Long followerId, Long followingId, String action) {

        if (followerId.equals(followingId)) {
            throw new RuntimeException("不能关注自己");
        }




        QueryWrapper<Follow> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("follower_id", followerId)
                   .eq("following_id", followingId);

        Follow follow = this.getOne(queryWrapper);

        if ("follow".equalsIgnoreCase(action)) {

            if (follow == null) {

                follow = new Follow();
                follow.setFollowerId(followerId);
                follow.setFollowingId(followingId);
                follow.setStatus(CommonStatus.ENABLED);
                follow.setCreateTime(LocalDateTime.now());
                follow.setUpdateTime(LocalDateTime.now());
                boolean result = this.save(follow);
                
                if (result) {

                    sendFollowNotification(followerId, followingId);
                }
                
                return result;
            } else if (follow.getStatus() == CommonStatus.DISABLED) {

                follow.setStatus(CommonStatus.ENABLED);
                follow.setUpdateTime(LocalDateTime.now());
                boolean result = this.updateById(follow);
                
                if (result) {

                    sendFollowNotification(followerId, followingId);
                }
                
                return result;
            } else {

                return true;
            }
        } else if ("unfollow".equalsIgnoreCase(action)) {

            if (follow != null && follow.getStatus() == CommonStatus.ENABLED) {
                follow.setStatus(CommonStatus.DISABLED);
                follow.setUpdateTime(LocalDateTime.now());
                return this.updateById(follow);
            }

            return true;
        } else {
            throw new RuntimeException("无效的操作类型");
        }
    }
    
    /**
     * 发送关注通知
     */
    private void sendFollowNotification(Long followerId, Long followingId) {
        try {

            User follower = userDao.selectById(followerId);
            if (follower == null) {

                return;
            }
            
            UserSimpleVO followerVO = new UserSimpleVO();
            followerVO.setId(follower.getId());
            followerVO.setNickname(follower.getNickname());
            followerVO.setAvatar(follower.getAvatar());
            followerVO.setLastOnlineTime(follower.getLastOnlineTime());
            

            Map<String, Object> extra = NotificationBuilder.buildFollowNotification(followerVO);
            

            notificationService.createNotification(
                followingId,
                NotificationType.FOLLOW.getCode(),
                followerId,
                null,
                extra
            );
            

            
        } catch (Exception e) {


        }
    }

    @Override
    public PageUtils getFollowingList(Map<String, Object> params, Long userId) {
        int page = params.get("page") != null ? Integer.parseInt(params.get("page").toString()) : 1;
        int limit = params.get("limit") != null ? Integer.parseInt(params.get("limit").toString()) : 20;

        IPage<FollowUserVO> pageObj = new Page<>(page, limit);
        List<FollowUserVO> list = baseMapper.selectFollowingList(pageObj, userId);
        

        pageObj.setRecords(list);
        
        return new PageUtils(pageObj);
    }

    @Override
    public PageUtils getFollowerList(Map<String, Object> params, Long userId) {
        int page = params.get("page") != null ? Integer.parseInt(params.get("page").toString()) : 1;
        int limit = params.get("limit") != null ? Integer.parseInt(params.get("limit").toString()) : 20;

        IPage<FollowUserVO> pageObj = new Page<>(page, limit);
        List<FollowUserVO> list = baseMapper.selectFollowerList(pageObj, userId);
        

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
        

        for (Long targetId : targetIds) {
            statusMap.put(targetId, false);
        }
        

        for (Follow follow : follows) {
            if (follow.getStatus() == CommonStatus.ENABLED) {
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
                   .eq("status", CommonStatus.ENABLED);
        return this.count(queryWrapper) > 0;
    }
}
