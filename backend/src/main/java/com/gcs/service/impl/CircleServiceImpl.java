package com.gcs.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.gcs.converter.UserConverter;
import com.gcs.dao.CircleDao;
import com.gcs.dao.CircleMemberDao;
import com.gcs.entity.Circle;
import com.gcs.entity.CircleMember;
import com.gcs.entity.User;
import com.gcs.dto.*;
import com.gcs.enums.CommonStatus;
import com.gcs.enums.MemberStatus;
import com.gcs.service.CircleService;
import com.gcs.service.CircleMemberService;
import com.gcs.service.UserService;
import com.gcs.utils.PageUtils;
import com.gcs.utils.Query;
import com.gcs.enums.CircleMemberRole;

import com.gcs.vo.CircleDetailVO;
import com.gcs.vo.CircleListVO;
import com.gcs.vo.CircleCreateResponseVO;
import com.gcs.vo.UserSimpleVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;

/**
 * 圈子服务实现类
 * @author
 * @date 2026-03-17
 */
@Slf4j
@Service("circleService")
public class CircleServiceImpl extends ServiceImpl<CircleDao, Circle> implements CircleService {

    @Autowired
    private CircleMemberService circleMemberService;

    @Autowired
    private UserService userService;

    @Autowired
    private UserConverter userConverter;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<Circle> circlePage = new Query<Circle>(params).getPage();
        QueryWrapper<Circle> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("status", CommonStatus.ENABLED.getCode());
        IPage<Circle> resultPage = this.page(circlePage, queryWrapper);

        return new PageUtils(resultPage);
    }

    @Override
    @Transactional
    public Circle createCircle(CircleCreateDTO createDTO, Long userId) {
        try {

            Circle circle = new Circle();
            circle.setName(createDTO.getName());
            circle.setDescription(createDTO.getDescription());
            circle.setAvatar(createDTO.getAvatar());
            circle.setOwnerId(userId);
            circle.setType(createDTO.getType());
            circle.setStatus(CommonStatus.ENABLED);

            this.save(circle);


            circleMemberService.addMember(circle.getId(), userId, CircleMemberRole.OWNER.getCode());

            log.info("用户{}创建圈子{}", userId, circle.getName());
            return circle;
        } catch (Exception e) {
            log.error("创建圈子失败", e);
            throw new RuntimeException("创建圈子失败：" + e.getMessage());
        }
    }

    @Override
    public CircleDetailVO getCircleDetail(Long circleId, Long currentUserId) {
        try {
            Circle circle = this.getById(circleId);
            if (circle == null || circle.getStatus()==CommonStatus.DISABLED) {
                throw new RuntimeException("圈子不存在或已解散");
            }


            if (circle.getType() == 0) {
                Boolean isMember = circleMemberService.isMember(circleId, currentUserId);
                if (isMember == null || !isMember) {
                    throw new RuntimeException("无权查看该圈子");
                }
            }

            CircleDetailVO vo = new CircleDetailVO();
            vo.setId(circle.getId());
            vo.setName(circle.getName());
            vo.setDescription(circle.getDescription());
            vo.setAvatar(circle.getAvatar());
            vo.setOwnerId(circle.getOwnerId());
            vo.setType(circle.getType());
            vo.setStatus(circle.getStatus());


            User owner = userService.getById(circle.getOwnerId());
            if (owner != null) {
                UserSimpleVO ownerVO = userConverter.toSimpleVO(owner);
                vo.setOwnerNickname(ownerVO.getNickname());
                vo.setOwnerAvatar(ownerVO.getAvatar());
            }


            vo.setMemberCount(circleMemberService.getActiveMemberCount(circleId));


            if (currentUserId != null) {
                Integer userRole = circleMemberService.getUserRoleInCircle(circleId, currentUserId);
                vo.setCurrentUserRole(userRole);
                vo.setIsJoined(userRole != null);
            } else {
                vo.setIsJoined(false);
            }

            if (circle.getCreateTime() != null) {
                vo.setCreateTime(circle.getCreateTime().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            }
            if (circle.getUpdateTime() != null) {
                vo.setUpdateTime(circle.getUpdateTime().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            }

            return vo;
        } catch (Exception e) {
            log.error("获取圈子详情失败，circleId: {}", circleId, e);
            throw new RuntimeException("获取圈子详情失败：" + e.getMessage());
        }
    }

    @Override
    @Transactional
    public void updateCircle(Long circleId, CircleUpdateDTO updateDTO, Long userId) {
        try {
            Circle circle = this.getById(circleId);
            if (circle == null || circle.getStatus() == CommonStatus.DISABLED) {
                throw new RuntimeException("圈子不存在或已解散");
            }


            if (!hasManagePermission(circleId, userId)) {
                throw new RuntimeException("无权限更新圈子信息");
            }

            if (StringUtils.hasText(updateDTO.getName())) {
                circle.setName(updateDTO.getName());
            }
            if (updateDTO.getDescription() != null) {
                circle.setDescription(updateDTO.getDescription());
            }
            if (StringUtils.hasText(updateDTO.getAvatar())) {
                circle.setAvatar(updateDTO.getAvatar());
            }
            if (updateDTO.getType() != null) {
                circle.setType(updateDTO.getType());
            }

            this.updateById(circle);
            log.info("用户{}更新圈子{}", userId, circleId);
        } catch (Exception e) {
            log.error("更新圈子失败，circleId: {}", circleId, e);
            throw new RuntimeException("更新圈子失败：" + e.getMessage());
        }
    }

    @Override
    @Transactional
    public void dissolveCircle(Long circleId, Long userId) {
        try {
            Circle circle = this.getById(circleId);
            if (circle == null || circle.getStatus() == CommonStatus.DISABLED) {
                throw new RuntimeException("圈子不存在或已解散");
            }


            if (!circle.getOwnerId().equals(userId)) {
                throw new RuntimeException("只有圈主可以解散圈子");
            }

            circle.setStatus(CommonStatus.DISABLED);
            this.updateById(circle);


            circleMemberService.removeAllMembers(circleId);

            log.info("用户{}解散圈子{}", userId, circleId);
        } catch (Exception e) {
            log.error("解散圈子失败，circleId: {}", circleId, e);
            throw new RuntimeException("解散圈子失败：" + e.getMessage());
        }
    }

    @Override
    public PageUtils getMyCircles(Long currentUserId, Map<String, Object> params) {
        try {
            IPage<Circle> circlePage = new Query<Circle>(params).getPage();


            List<Long> joinedCircleIds = circleMemberService.getJoinedCircleIds(currentUserId);

            if (joinedCircleIds == null || joinedCircleIds.isEmpty()) {
                return new PageUtils(List.of(), 0, circlePage.getSize(), circlePage.getCurrent());
            }

            QueryWrapper<Circle> queryWrapper = new QueryWrapper<>();
            queryWrapper.in("id", joinedCircleIds)
                    .eq("status", CommonStatus.ENABLED.getCode());

            IPage<Circle> resultPage = this.page(circlePage, queryWrapper);
            
            // 转换为 CircleListVO
            List<CircleListVO> voList = resultPage.getRecords().stream()
                .map(circle -> convertToCircleListVO(circle, currentUserId))
                .toList();
            
            return new PageUtils(voList, resultPage.getTotal(), circlePage.getSize(), circlePage.getCurrent());
        } catch (Exception e) {
            log.error("获取用户圈子列表失败，userId: {}", currentUserId, e);
            throw new RuntimeException("获取圈子列表失败：" + e.getMessage());
        }
    }

    @Override
    public PageUtils getPublicCircles(Map<String, Object> params, Long currentUserId) {
        try {
            IPage<Circle> circlePage = new Query<Circle>(params).getPage();

            QueryWrapper<Circle> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("status", CommonStatus.ENABLED.getCode());

            // 如果传入了关键词
            String keyword = (String) params.get("keyword");
            if (StringUtils.hasText(keyword)) {
                queryWrapper.and(wrapper ->
                        wrapper.like("name", keyword)
                                .or()
                                .like("description", keyword)
                );
            }

            IPage<Circle> resultPage = this.page(circlePage, queryWrapper);
            
            // 转换为 CircleListVO
            List<CircleListVO> voList = resultPage.getRecords().stream()
                .map(circle -> convertToCircleListVO(circle, currentUserId))
                .toList();
            
            return new PageUtils(voList, resultPage.getTotal(), circlePage.getSize(), circlePage.getCurrent());
        } catch (Exception e) {
            log.error("获取公开圈子列表失败", e);
            throw new RuntimeException("获取公开圈子列表失败：" + e.getMessage());
        }
    }

    /**
     * 将 Circle 转换为 CircleListVO
     */
    private CircleListVO convertToCircleListVO(Circle circle, Long currentUserId) {
        CircleListVO vo = new CircleListVO();
        vo.setId(circle.getId());
        vo.setName(circle.getName());
        vo.setDescription(circle.getDescription());
        vo.setAvatar(circle.getAvatar());
        vo.setOwnerId(circle.getOwnerId());
        vo.setType(circle.getType());
        vo.setStatus(circle.getStatus());
        
        User owner = userService.getById(circle.getOwnerId());
        if (owner != null) {
            UserSimpleVO ownerVO = userConverter.toSimpleVO(owner);
            vo.setOwnerNickname(ownerVO.getNickname());
            vo.setOwnerAvatar(ownerVO.getAvatar());
        }
        
        try {
            Integer memberCount = circleMemberService.getActiveMemberCount(circle.getId());
            vo.setMemberCount(memberCount != null ? memberCount : 0);
            log.debug("圈子{}的成员数量：{}", circle.getId(), memberCount);
        } catch (Exception e) {
            log.error("获取圈子{}的成员数量失败", circle.getId(), e);
            vo.setMemberCount(0);
        }
        
        if (currentUserId != null) {
            if (circle.getOwnerId().equals(currentUserId)) {
                vo.setIsJoined(true);
            } else {
                try {
                    Boolean isMember = circleMemberService.isMember(circle.getId(), currentUserId);
                    vo.setIsJoined(isMember != null && isMember);
                } catch (Exception e) {
                    log.error("检查用户{}是否在圈子{}中失败", currentUserId, circle.getId(), e);
                    vo.setIsJoined(false);
                }
            }
        } else {
            vo.setIsJoined(false);
        }
        
        vo.setUnreadCount(0);
        
        if (circle.getCreateTime() != null) {
            vo.setCreateTime(circle.getCreateTime().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        }
        
        return vo;
    }

    @Override
    public boolean hasManagePermission(Long circleId, Long userId) {
        try {
            Circle circle = this.getById(circleId);
            if (circle == null) {
                return false;
            }

            // 圈主直接有权限
            if (circle.getOwnerId().equals(userId)) {
                return true;
            }

            // 检查是否是管理员
            Integer userRole = circleMemberService.getUserRoleInCircle(circleId, userId);
            return userRole != null && userRole == CircleMemberRole.ADMIN.getCode();
        } catch (Exception e) {
            log.error("检查圈子管理权限失败，circleId: {}, userId: {}", circleId, userId, e);
            return false;
        }
    }
}
