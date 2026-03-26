package com.gcs.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.gcs.dao.CircleMemberDao;
import com.gcs.dao.UserDao;
import com.gcs.entity.CircleMember;
import com.gcs.entity.User;
import com.gcs.enums.CommonStatus;
import com.gcs.service.CircleMemberService;
import com.gcs.service.NotificationService;
import com.gcs.dao.CircleDao;
import com.gcs.entity.Circle;
import com.gcs.enums.NotificationType;
import com.gcs.utils.NotificationBuilder;
import com.gcs.vo.CircleMemberVO;
import com.gcs.vo.UserSimpleVO;
import com.gcs.utils.PageUtils;
import com.gcs.utils.Query;
import com.gcs.enums.CircleMemberRole;

import com.gcs.vo.UserSimpleVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;

/**
 * 圈子成员服务实现类
 * @author
 * @date 2026-03-17
 */
@Slf4j
@Service("circleMemberService")
public class CircleMemberServiceImpl extends ServiceImpl<CircleMemberDao, CircleMember> implements CircleMemberService {

    @Autowired
    private CircleMemberDao circleMemberDao;

    @Autowired
    private UserDao userDao;
    
    @Autowired
    private NotificationService notificationService;
    
    @Autowired
    private CircleDao circleDao;

    @Override
    @Transactional
    public void addMember(Long circleId, Long userId, Integer role) {
        try {
            // 检查是否已是成员
            Boolean exists = circleMemberDao.isMember(circleId, userId);
            if (exists != null && exists) {
                throw new RuntimeException("用户已是圈子成员");
            }

            CircleMember member = new CircleMember();
            member.setCircleId(circleId);
            member.setUserId(userId);
            member.setRole(role);
            member.setJoinTime(LocalDateTime.now());
            member.setStatus(CommonStatus.ENABLED);

            this.save(member);
            log.info("用户{}加入圈子{}", userId, circleId);
            
            // 📢 发送通知给新成员（欢迎）和其他成员
            sendMemberJoinNotifications(circleId, userId, role);
        } catch (Exception e) {
            log.error("添加圈子成员失败，circleId: {}, userId: {}", circleId, userId, e);
            throw new RuntimeException("添加成员失败：" + e.getMessage());
        }
    }

    @Override
    @Transactional
    public void removeMember(Long circleId, Long userId) {
        try {
            // 先查询成员信息，用于发送通知
            LambdaQueryWrapper<CircleMember> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(CircleMember::getCircleId, circleId)
                    .eq(CircleMember::getUserId, userId);
            CircleMember member = this.getOne(queryWrapper);
            
            this.remove(queryWrapper);
            log.info("用户{}被移除出圈子{}", userId, circleId);
            
            // 📢 发送被移出圈子的通知
            if (member != null) {
                sendCircleRemovedNotification(circleId, userId);
            }
        } catch (Exception e) {
            log.error("移除圈子成员失败，circleId: {}, userId: {}", circleId, userId, e);
            throw new RuntimeException("移除成员失败：" + e.getMessage());
        }
    }

    @Override
    public List<Long> getMemberIds(Long circleId) {
        try {
            LambdaQueryWrapper<CircleMember> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(CircleMember::getCircleId, circleId)
                    .eq(CircleMember::getStatus, CommonStatus.ENABLED);

            List<CircleMember> members = this.list(queryWrapper);
            return members.stream()
                    .map(CircleMember::getUserId)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("获取圈子成员 ID 列表失败，circleId: {}", circleId, e);
            return List.of();
        }
    }

    @Override
    @Transactional
    public void updateMemberRole(Long circleId, Long userId, Integer role) {
        try {
            LambdaQueryWrapper<CircleMember> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(CircleMember::getCircleId, circleId)
                    .eq(CircleMember::getUserId, userId)
                    .eq(CircleMember::getStatus, CommonStatus.ENABLED);

            CircleMember member = this.getOne(queryWrapper);
            if (member == null) {
                throw new RuntimeException("用户不是圈子成员");
            }

            member.setRole(role);
            this.updateById(member);
            log.info("更新用户在圈子{}的角色为{}", circleId, role);
        } catch (Exception e) {
            log.error("更新成员角色失败，circleId: {}, userId: {}", circleId, userId, e);
            throw new RuntimeException("更新成员角色失败：" + e.getMessage());
        }
    }

    @Override
    public PageUtils getMemberPageWithUserInfo(Long circleId, Map<String, Object> params) {
        try {
            IPage<CircleMember> memberPage = new Query<CircleMember>(params).getPage();

            LambdaQueryWrapper<CircleMember> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(CircleMember::getCircleId, circleId)
                    .eq(CircleMember::getStatus, CommonStatus.ENABLED);

            // 角色过滤
            String role = (String) params.get("role");
            if (StringUtils.hasText(role)) {
                queryWrapper.eq(CircleMember::getRole, Integer.parseInt(role));
            }

            queryWrapper.orderByAsc(CircleMember::getJoinTime);

            IPage<CircleMember> resultPage = this.page(memberPage, queryWrapper);

            // 将实体列表转换为 VO 列表
            List<CircleMemberVO> voList = resultPage.getRecords().stream()
                    .map(member -> {
                        CircleMemberVO vo = new CircleMemberVO();
                        vo.setId(member.getId());
                        vo.setRole(member.getRole());
                        vo.setRoleDescription(getRoleDescription(member.getRole()));
                        vo.setJoinTime(member.getJoinTime());
                        vo.setStatus(member.getStatus());

                        // 填充用户信息
                        User user = userDao.selectById(member.getUserId());
                        UserSimpleVO userSimpleVO = new UserSimpleVO();
                        userSimpleVO.setId(user != null ? user.getId() : member.getUserId());
                        userSimpleVO.setNickname(user != null ? user.getNickname() : "未知用户");
                        userSimpleVO.setAvatar(user != null ? user.getAvatar() : "");
                        userSimpleVO.setLastOnlineTime(user != null ? user.getLastOnlineTime() : null);
                        vo.setUser(userSimpleVO);

                        return vo;
                    })
                    .collect(Collectors.toList());

            // 创建新的 PageUtils 对象
            PageUtils pageUtils = new PageUtils(voList, resultPage.getTotal(), resultPage.getSize(), resultPage.getCurrent());
            return pageUtils;
        } catch (Exception e) {
            log.error("获取成员列表失败，circleId: {}", circleId, e);
            throw new RuntimeException("获取成员列表失败：" + e.getMessage());
        }
    }

    @Override
    public PageUtils getMemberPage(Long circleId, Map<String, Object> params) {
        try {
            IPage<CircleMember> memberPage = new Query<CircleMember>(params).getPage();

            LambdaQueryWrapper<CircleMember> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(CircleMember::getCircleId, circleId)
                    .eq(CircleMember::getStatus, CommonStatus.ENABLED);

            // 角色过滤
            String role = (String) params.get("role");
            if (StringUtils.hasText(role)) {
                queryWrapper.eq(CircleMember::getRole, Integer.parseInt(role));
            }

            queryWrapper.orderByDesc(CircleMember::getJoinTime);

            IPage<CircleMember> resultPage = this.page(memberPage, queryWrapper);
            return new PageUtils(resultPage);
        } catch (Exception e) {
            log.error("获取成员列表失败，circleId: {}", circleId, e);
            throw new RuntimeException("获取成员列表失败：" + e.getMessage());
        }
    }


    @Override
    public CircleMemberVO getMemberDetail(Long circleId, Long userId) {
        try {
            LambdaQueryWrapper<CircleMember> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(CircleMember::getCircleId, circleId)
                    .eq(CircleMember::getUserId, userId)
                    .eq(CircleMember::getStatus, CommonStatus.ENABLED);

            CircleMember member = this.getOne(queryWrapper);
            if (member == null) {
                return null;
            }

            User user = userDao.selectById(userId);
            CircleMemberVO vo = new CircleMemberVO();
            vo.setId(member.getId());

            UserSimpleVO userSimpleVO = new UserSimpleVO();
            userSimpleVO.setId(user != null ? user.getId() : userId);
            userSimpleVO.setNickname(user != null ? user.getNickname() : "未知用户");
            userSimpleVO.setAvatar(user != null ? user.getAvatar() : "");
            userSimpleVO.setLastOnlineTime(user != null ? user.getLastOnlineTime() : null);
            vo.setUser(userSimpleVO);

            vo.setRole(member.getRole());
            vo.setRoleDescription(getRoleDescription(member.getRole()));
            vo.setJoinTime(member.getJoinTime());
            vo.setStatus(member.getStatus());

            return vo;
        } catch (Exception e) {
            log.error("获取成员详情失败，circleId: {}, userId: {}", circleId, userId, e);
            return null;
        }
    }

    @Override
    @Transactional
    public void leaveCircle(Long circleId, Long userId) {
        try {
            LambdaQueryWrapper<CircleMember> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(CircleMember::getCircleId, circleId)
                    .eq(CircleMember::getUserId, userId);

            CircleMember member = this.getOne(queryWrapper);
            if (member == null) {
                throw new RuntimeException("不是圈子成员");
            }

            // 圈主不能退出
            if (member.getRole() == CircleMemberRole.OWNER.getCode()) {
                throw new RuntimeException("圈主不能退出圈子，请先转让或解散");
            }

            // 软删除：更新状态为已禁用
            member.setStatus(CommonStatus.DISABLED);
            this.updateById(member);

            log.info("用户{}退出圈子{}", userId, circleId);
            
            // 📢 发送成员退出通知给圈主和管理员
            sendMemberQuitNotification(circleId, userId);
        } catch (Exception e) {
            log.error("退出圈子失败，circleId: {}, userId: {}", circleId, userId, e);
            throw new RuntimeException("退出圈子失败：" + e.getMessage());
        }
    }

    @Override
    public boolean hasManagePermission(Long circleId, Long userId) {
        try {
            Integer userRole = circleMemberDao.getUserRoleInCircle(circleId, userId);
            if (userRole == null) {
                return false;
            }

            // 管理员或圈主有管理权限
            return userRole == CircleMemberRole.ADMIN.getCode() ||
                    userRole == CircleMemberRole.OWNER.getCode();
        } catch (Exception e) {
            log.error("检查管理权限失败，circleId: {}, userId: {}", circleId, userId, e);
            return false;
        }
    }

    @Override
    public Boolean isMember(Long circleId, Long userId) {
        return circleMemberDao.isMember(circleId, userId);
    }

    @Override
    public Integer getUserRoleInCircle(Long circleId, Long userId) {
        return circleMemberDao.getUserRoleInCircle(circleId, userId);
    }

    /**
     * 获取角色描述
     */
    private String getRoleDescription(Integer role) {
        if (role == null) {
            return "未知";
        }
        switch (role) {
            case 0: return "成员";
            case 1: return "管理员";
            case 2: return "圈主";
            default: return "未知";
        }
    }
    
    /**
     * 发送新成员加入通知给圈主和管理员
     */
    private void sendMemberJoinNotifications(Long circleId, Long newMemberId, Integer role) {
        try {
            // 查询圈子信息
            Circle circle = circleDao.selectById(circleId);
            if (circle == null) {
                log.warn("圈子不存在，circleId: {}", circleId);
                return;
            }
            
            // 查询新成员信息
            User newMember = userDao.selectById(newMemberId);
            if (newMember == null) {
                log.warn("新成员不存在，userId: {}", newMemberId);
                return;
            }
            
            UserSimpleVO newMemberVO = new UserSimpleVO();
            newMemberVO.setId(newMember.getId());
            newMemberVO.setNickname(newMember.getNickname());
            newMemberVO.setAvatar(newMember.getAvatar());
            newMemberVO.setLastOnlineTime(newMember.getLastOnlineTime());
            
            // 查询圈主和管理员
            List<Long> adminIds = circleMemberDao.getAdminAndOwnerIds(circleId);
            
            for (Long adminId : adminIds) {
                // 不通知自己
                if (adminId.equals(newMemberId)) {
                    continue;
                }
                
                Map<String, Object> extra = NotificationBuilder.buildMemberJoinNotification(
                    circleId,
                    circle.getName(),
                    newMemberVO
                );
                
                notificationService.createNotification(
                    adminId,
                    NotificationType.MEMBER_JOIN.getCode(),
                    circleId,
                    null,
                    extra
                );
            }
            
            log.info("发送新成员加入通知，circleId: {}, newMemberId: {}", circleId, newMemberId);
            
        } catch (Exception e) {
            log.error("发送新成员加入通知失败，circleId: {}, memberId: {}", circleId, newMemberId, e);
        }
    }
    
    /**
     * 发送被移出圈子的通知
     */
    private void sendCircleRemovedNotification(Long circleId, Long removedUserId) {
        try {
            // 查询圈子信息
            Circle circle = circleDao.selectById(circleId);
            if (circle == null) {
                log.warn("圈子不存在，circleId: {}", circleId);
                return;
            }
            
            // 查询操作人（当前登录用户，这里需要根据实际情况获取）
            // 暂时设置为圈主
            List<Long> owners = circleMemberDao.getOwners(circleId);
            Long operatorId = owners.isEmpty() ? null : owners.get(0);
            
            UserSimpleVO operatorVO = null;
            if (operatorId != null) {
                User operator = userDao.selectById(operatorId);
                if (operator != null) {
                    operatorVO = new UserSimpleVO();
                    operatorVO.setId(operator.getId());
                    operatorVO.setNickname(operator.getNickname());
                    operatorVO.setAvatar(operator.getAvatar());
                    operatorVO.setLastOnlineTime(operator.getLastOnlineTime());
                }
            }
            
            Map<String, Object> extra = NotificationBuilder.buildCircleRemovedNotification(
                circleId,
                circle.getName(),
                operatorVO
            );
            
            notificationService.createNotification(
                removedUserId,
                NotificationType.CIRCLE_REMOVED.getCode(),
                circleId,
                null,
                extra
            );
            
            log.info("发送被移出圈子通知，circleId: {}, removedUserId: {}", circleId, removedUserId);
            
        } catch (Exception e) {
            log.error("发送被移出圈子通知失败，circleId: {}, userId: {}", circleId, removedUserId, e);
        }
    }
    
    /**
     * 发送成员退出通知给圈主
     */
    private void sendMemberQuitNotification(Long circleId, Long quitterId) {
        try {
            // 查询圈子信息
            Circle circle = circleDao.selectById(circleId);
            if (circle == null) {
                log.warn("圈子不存在，circleId: {}", circleId);
                return;
            }
            
            // 查询退出者信息
            User quitter = userDao.selectById(quitterId);
            if (quitter == null) {
                log.warn("退出者不存在，userId: {}", quitterId);
                return;
            }
            
            UserSimpleVO quitterVO = new UserSimpleVO();
            quitterVO.setId(quitter.getId());
            quitterVO.setNickname(quitter.getNickname());
            quitterVO.setAvatar(quitter.getAvatar());
            quitterVO.setLastOnlineTime(quitter.getLastOnlineTime());
            
            // 通知圈主
            List<Long> ownerIds = circleMemberDao.getOwners(circleId);
            for (Long ownerId : ownerIds) {
                Map<String, Object> extra = NotificationBuilder.buildMemberQuitNotification(
                    circleId,
                    circle.getName(),
                    quitterVO
                );
                
                notificationService.createNotification(
                    ownerId,
                    NotificationType.MEMBER_QUIT.getCode(),
                    circleId,
                    null,
                    extra
                );
            }
            
            log.info("发送成员退出通知，circleId: {}, quitterId: {}", circleId, quitterId);
            
        } catch (Exception e) {
            log.error("发送成员退出通知失败，circleId: {}, userId: {}", circleId, quitterId, e);
        }
    }
}
