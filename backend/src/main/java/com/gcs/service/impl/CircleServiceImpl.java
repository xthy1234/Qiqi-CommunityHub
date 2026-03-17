package com.gcs.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.gcs.dao.CircleDao;
import com.gcs.dao.CircleMemberDao;
import com.gcs.dao.UserDao;
import com.gcs.entity.Circle;
import com.gcs.entity.CircleMember;
import com.gcs.entity.User;
import com.gcs.dto.*;
import com.gcs.enums.CommonStatus;
import com.gcs.service.CircleService;
import com.gcs.service.CircleMemberService;
import com.gcs.utils.PageUtils;
import com.gcs.utils.Query;
import com.gcs.enums.CircleMemberRole;

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
    private CircleMemberDao circleMemberDao;

    @Autowired
    private UserDao userDao;

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
            // 创建圈子
            Circle circle = new Circle();
            circle.setName(createDTO.getName());
            circle.setDescription(createDTO.getDescription());
            circle.setAvatar(createDTO.getAvatar());
            circle.setOwnerId(userId);
            circle.setType(createDTO.getType());
            circle.setStatus(CommonStatus.ENABLED);

            this.save(circle);

            // 自动成为圈主
            CircleMember member = new CircleMember();
            member.setCircleId(circle.getId());
            member.setUserId(userId);
            member.setRole(CircleMemberRole.OWNER.getCode());
            member.setJoinTime(LocalDateTime.now());
            circleMemberService.save(member);

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

            // 检查权限：私密圈子只能成员查看
            if (circle.getType() == 0) { // 0:私密圈子
                Boolean isMember = circleMemberDao.isMember(circleId, currentUserId);
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

            // 获取圈主信息
            User owner = userDao.selectById(circle.getOwnerId());
            if (owner != null) {
                vo.setOwnerNickname(owner.getNickname());
                vo.setOwnerAvatar(owner.getAvatar());
            }

            // 获取成员数量
            vo.setMemberCount(circleMemberDao.getMemberCount(circleId));

            // 获取当前用户角色
            if (currentUserId != null) {
                Integer userRole = circleMemberDao.getUserRoleInCircle(circleId, currentUserId);
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

            // 检查权限：只有圈主或管理员可以更新
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

            // 只有圈主可以解散
            if (!circle.getOwnerId().equals(userId)) {
                throw new RuntimeException("只有圈主可以解散圈子");
            }

            circle.setStatus(CommonStatus.DISABLED); // 1:解散
            this.updateById(circle);

            // 删除所有成员关系
            LambdaQueryWrapper<CircleMember> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(CircleMember::getCircleId, circleId);
            circleMemberService.remove(queryWrapper);

            log.info("用户{}解散圈子{}", userId, circleId);
        } catch (Exception e) {
            log.error("解散圈子失败，circleId: {}", circleId, e);
            throw new RuntimeException("解散圈子失败：" + e.getMessage());
        }
    }

    @Override
    @Transactional
    public void leaveCircle(Long circleId, Long userId) {
        try {
            Circle circle = this.getById(circleId);
            if (circle == null || circle.getStatus() == CommonStatus.DISABLED) {
                throw new RuntimeException("圈子不存在或已解散");
            }

            // 圈主不能退出
            if (circle.getOwnerId().equals(userId)) {
                throw new RuntimeException("圈主不能退出圈子，请先转让或解散");
            }

            // 删除成员关系
            LambdaQueryWrapper<CircleMember> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(CircleMember::getCircleId, circleId)
                    .eq(CircleMember::getUserId, userId);
            circleMemberService.remove(queryWrapper);

            log.info("用户{}退出圈子{}", userId, circleId);
        } catch (Exception e) {
            log.error("退出圈子失败，circleId: {}", circleId, e);
            throw new RuntimeException("退出圈子失败：" + e.getMessage());
        }
    }

    @Override
    public PageUtils getMyCircles(Long currentUserId, Map<String, Object> params) {
        try {
            IPage<Circle> circlePage = new Query<Circle>(params).getPage();

            // 查询用户加入的所有圈子 ID
            List<Long> joinedCircleIds = circleMemberDao.getJoinedCircleIds(currentUserId);

            if (joinedCircleIds == null || joinedCircleIds.isEmpty()) {
                return new PageUtils(List.of(), 0, circlePage.getSize(), circlePage.getCurrent());
            }

            QueryWrapper<Circle> queryWrapper = new QueryWrapper<>();
            queryWrapper.in("id", joinedCircleIds)
                    .eq("status", CommonStatus.ENABLED.getCode());

            IPage<Circle> resultPage = this.page(circlePage, queryWrapper);
            return new PageUtils(resultPage);
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
            return new PageUtils(resultPage);
        } catch (Exception e) {
            log.error("获取公开圈子列表失败", e);
            throw new RuntimeException("获取公开圈子列表失败：" + e.getMessage());
        }
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
            Integer userRole = circleMemberDao.getUserRoleInCircle(circleId, userId);
            return userRole != null && userRole == CircleMemberRole.ADMIN.getCode();
        } catch (Exception e) {
            log.error("检查圈子管理权限失败，circleId: {}, userId: {}", circleId, userId, e);
            return false;
        }
    }
}
