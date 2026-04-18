package com.gcs.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.gcs.converter.UserConverter;
import com.gcs.dao.CircleMemberDao;
import com.gcs.dao.UserDao;
import com.gcs.entity.CircleMember;
import com.gcs.entity.User;
import com.gcs.enums.CommonStatus;
import com.gcs.enums.MemberStatus;
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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.HashMap;
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
    
    @Autowired
    private UserConverter userConverter;

    @Override
    @Transactional
    public void addMember(Long circleId, Long userId, Integer role) {
        try {

            Boolean exists = circleMemberDao.isMember(circleId, userId);
            if (exists != null && exists) {
                throw new RuntimeException("用户已是圈子成员");
            }

            CircleMember member = new CircleMember();
            member.setCircleId(circleId);
            member.setUserId(userId);
            member.setRole(role);
            member.setJoinTime(LocalDateTime.now());
            member.setStatus(MemberStatus.ACTIVE.getCode());

            this.save(member);
            log.info("用户{}加入圈子{}", userId, circleId);
            
            // 发送通知给新成员（欢迎）和其他成员
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
            LambdaQueryWrapper<CircleMember> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(CircleMember::getCircleId, circleId)
                    .eq(CircleMember::getUserId, userId);
            CircleMember member = this.getOne(queryWrapper);
            
            if (member == null) {
                throw new RuntimeException("用户不是圈子成员");
            }
            
            // 软删除：更新状态为已退出/被移除
            member.setStatus(MemberStatus.INACTIVE.getCode());
            this.updateById(member);
            
            log.info("用户{}被移除出圈子{}", userId, circleId);
            
            // 发送被移出圈子的通知
            sendCircleRemovedNotification(circleId, userId);
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
                    .eq(CircleMember::getStatus, MemberStatus.ACTIVE.getCode());

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
                    .eq(CircleMember::getStatus, MemberStatus.ACTIVE.getCode());

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
                    .eq(CircleMember::getStatus, MemberStatus.ACTIVE.getCode());

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
                        UserSimpleVO userSimpleVO = userConverter.toSimpleVO(user);
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
                    .eq(CircleMember::getStatus, MemberStatus.ACTIVE.getCode());

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
                    .eq(CircleMember::getStatus, MemberStatus.ACTIVE.getCode());

            CircleMember member = this.getOne(queryWrapper);
            if (member == null) {
                return null;
            }

            User user = userDao.selectById(userId);
            CircleMemberVO vo = new CircleMemberVO();
            vo.setId(member.getId());

            UserSimpleVO userSimpleVO = userConverter.toSimpleVO(user);
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

            // 软删除：更新状态为已退出
            member.setStatus(MemberStatus.INACTIVE.getCode());
            this.updateById(member);

            log.info("用户{}退出圈子{}", userId, circleId);
            
            // 发送成员退出通知给圈主和管理员
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
     * 转换为 VO 并填充用户信息
     */
    private CircleMemberVO convertToVOWithUser(CircleMember member) {
        CircleMemberVO vo = new CircleMemberVO();
        vo.setId(member.getId());
        vo.setRole(member.getRole());
        vo.setRoleDescription(getRoleDescription(member.getRole()));
        vo.setJoinTime(member.getJoinTime());
        vo.setStatus(member.getStatus());
        
        User user = userDao.selectById(member.getUserId());
        UserSimpleVO userSimpleVO = userConverter.toSimpleVO(user);
        vo.setUser(userSimpleVO);
        
        return vo;
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
            
            UserSimpleVO newMemberVO = userConverter.toSimpleVO(newMember);
            
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
                operatorVO = userConverter.toSimpleVO(operator);
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
            
            UserSimpleVO quitterVO = userConverter.toSimpleVO(quitter);
            
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

    @Override
    @Transactional
    public void applyToJoin(Long circleId, Long userId) {
        try {
            // 检查是否已有申请
            Boolean hasPending = circleMemberDao.hasPendingApplication(circleId, userId);
            if (hasPending != null && hasPending) {
                throw new RuntimeException("已有待审核的申请");
            }
            
            // 检查是否已是成员
            Boolean isMember = circleMemberDao.isMember(circleId, userId);
            if (isMember != null && isMember) {
                throw new RuntimeException("已是圈子成员");
            }
            
            // 创建待审核记录
            CircleMember member = new CircleMember();
            member.setCircleId(circleId);
            member.setUserId(userId);
            member.setRole(CircleMemberRole.MEMBER.getCode());
            member.setJoinTime(LocalDateTime.now());
            member.setStatus(MemberStatus.PENDING.getCode());
            
            this.save(member);
            
            // 通知圈主和管理员有人申请加入
            sendCircleJoinApplicationNotification(circleId, userId);
            
            log.info("用户{}申请加入圈子{}", userId, circleId);
        } catch (Exception e) {
            log.error("申请加入圈子失败，circleId: {}, userId: {}", circleId, userId, e);
            throw new RuntimeException("申请加入失败：" + e.getMessage());
        }
    }

    @Override
    @Transactional
    public void reviewJoinApplication(Long circleId, Long userId, boolean approved, Long operatorId) {
        try {
            LambdaQueryWrapper<CircleMember> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(CircleMember::getCircleId, circleId)
                    .eq(CircleMember::getUserId, userId)
                    .eq(CircleMember::getStatus, MemberStatus.PENDING.getCode());
            
            CircleMember member = this.getOne(queryWrapper);
            if (member == null) {
                throw new RuntimeException("未找到待审核的申请");
            }
            
            if (approved) {
                // 通过申请
                member.setStatus(MemberStatus.ACTIVE.getCode());
                this.updateById(member);
                
                // 通知申请人审核通过
                sendApplicationApprovedNotification(circleId, userId);
                
                // 通知其他成员有新成员加入
                sendMemberJoinNotifications(circleId, userId, member.getRole());
                
                log.info("用户{}的加入申请已通过，审核人：{}", userId, operatorId);
            } else {
                // 拒绝申请，删除记录
                this.removeById(member.getId());
                
                // 通知申请人审核被拒
                sendApplicationRejectedNotification(circleId, userId, operatorId);
                
                log.info("用户{}的加入申请已被拒绝，审核人：{}", userId, operatorId);
            }
        } catch (Exception e) {
            log.error("审核加入申请失败，circleId: {}, userId: {}", circleId, userId, e);
            throw new RuntimeException("审核失败：" + e.getMessage());
        }
    }

    @Override
    @Transactional
    public void acceptInvite(Long circleId, Long userId, String inviteCode) {
        try {
            // TODO: 验证邀请码有效性（需要从邀请表中查询）
            // 这里简化处理，直接添加为成员
            
            Boolean isMember = circleMemberDao.isMember(circleId, userId);
            if (isMember != null && isMember) {
                throw new RuntimeException("已是圈子成员");
            }
            
            // 直接添加为活跃成员（无需审核）
            addMember(circleId, userId, CircleMemberRole.MEMBER.getCode());
            
            log.info("用户{}通过邀请码{}加入圈子{}", userId, inviteCode, circleId);
        } catch (Exception e) {
            log.error("接受邀请失败，circleId: {}, userId: {}", circleId, userId, e);
            throw new RuntimeException("接受邀请失败：" + e.getMessage());
        }
    }

    @Override
    public PageUtils getPendingMembers(Long circleId, Map<String, Object> params) {
        try {
            IPage<CircleMember> page = new Query<CircleMember>(params).getPage();
            
            List<CircleMember> members = circleMemberDao.getPendingMembers(circleId);
            
            // 手动分页
            int total = members.size();
            int current = (int) page.getCurrent();
            int size = (int) page.getSize();
            int fromIndex = (current - 1) * size;
            int toIndex = Math.min(fromIndex + size, total);
            
            List<CircleMember> pagedMembers = fromIndex < total ? 
                members.subList(fromIndex, toIndex) : List.of();
            
            // 转换为 VO
            List<CircleMemberVO> voList = pagedMembers.stream()
                .map(member -> convertToVOWithUser(member))
                .collect(Collectors.toList());
            
            return new PageUtils(voList, total, size, current);
        } catch (Exception e) {
            log.error("获取待审核成员列表失败，circleId: {}", circleId, e);
            throw new RuntimeException("获取待审核成员列表失败：" + e.getMessage());
        }
    }

    @Override
    public PageUtils getActiveMembers(Long circleId, Map<String, Object> params) {
        try {
            IPage<CircleMember> page = new Query<CircleMember>(params).getPage();
            
            List<CircleMember> members = circleMemberDao.getActiveMembers(circleId);
            
            // 手动分页
            int total = members.size();
            int current = (int) page.getCurrent();
            int size = (int) page.getSize();
            int fromIndex = (current - 1) * size;
            int toIndex = Math.min(fromIndex + size, total);
            
            List<CircleMember> pagedMembers = fromIndex < total ? 
                members.subList(fromIndex, toIndex) : List.of();
            
            // 转换为 VO
            List<CircleMemberVO> voList = pagedMembers.stream()
                .map(member -> convertToVOWithUser(member))
                .collect(Collectors.toList());
            
            return new PageUtils(voList, total, size, current);
        } catch (Exception e) {
            log.error("获取活跃成员列表失败，circleId: {}", circleId, e);
            throw new RuntimeException("获取活跃成员列表失败：" + e.getMessage());
        }
    }

    /**
     * 发送圈子加入申请通知给圈主和管理员
     */
    private void sendCircleJoinApplicationNotification(Long circleId, Long applicantId) {
        try {
            Circle circle = circleDao.selectById(circleId);
            if (circle == null) {
                return;
            }
            
            User applicant = userDao.selectById(applicantId);
            if (applicant == null) {
                return;
            }
            
            UserSimpleVO applicantVO = new UserSimpleVO();
            applicantVO.setId(applicant.getId());
            applicantVO.setNickname(applicant.getNickname());
            applicantVO.setAvatar(applicant.getAvatar());
            applicantVO.setLastOnlineTime(applicant.getLastOnlineTime());
            
            List<Long> adminIds = circleMemberDao.getAdminAndOwnerIds(circleId);
            
            for (Long adminId : adminIds) {
                Map<String, Object> extra = NotificationBuilder.buildCircleJoinNotification(
                    circleId,
                    circle.getName(),
                    applicantVO
                );
                
                notificationService.createNotification(
                    adminId,
                    NotificationType.CIRCLE_JOIN.getCode(),
                    circleId,
                    null,
                    extra
                );
            }
            
            log.info("发送圈子加入申请通知，circleId: {}, applicantId: {}", circleId, applicantId);
        } catch (Exception e) {
            log.error("发送圈子加入申请通知失败", e);
        }
    }

    /**
     * 发送申请通过通知
     */
    private void sendApplicationApprovedNotification(Long circleId, Long userId) {
        try {
            Circle circle = circleDao.selectById(circleId);
            if (circle == null) {
                return;
            }
            
            Map<String, Object> extra = new HashMap<>();
            extra.put("circleId", circleId);
            extra.put("circleName", circle.getName());
            extra.put("result", "通过");
            
            notificationService.createNotification(
                userId,
                NotificationType.SYSTEM_MESSAGE.getCode(),
                circleId,
                null,
                extra
            );
            
            log.info("发送申请通过通知，circleId: {}, userId: {}", circleId, userId);
        } catch (Exception e) {
            log.error("发送申请通过通知失败", e);
        }
    }

    /**
     * 发送申请被拒通知
     */
    private void sendApplicationRejectedNotification(Long circleId, Long userId, Long operatorId) {
        try {
            Circle circle = circleDao.selectById(circleId);
            if (circle == null) {
                return;
            }
            
            User operator = userDao.selectById(operatorId);
            
            Map<String, Object> extra = new HashMap<>();
            extra.put("circleId", circleId);
            extra.put("circleName", circle.getName());
            extra.put("result", "拒绝");
            if (operator != null) {
                extra.put("operator", Map.of(
                    "id", operator.getId(),
                    "nickname", operator.getNickname(),
                    "avatar", operator.getAvatar()
                ));
            }
            
            notificationService.createNotification(
                userId,
                NotificationType.SYSTEM_MESSAGE.getCode(),
                circleId,
                null,
                extra
            );
            
            log.info("发送申请被拒通知，circleId: {}, userId: {}", circleId, userId);
        } catch (Exception e) {
            log.error("发送申请被拒通知失败", e);
        }
    }

    @Override
    public Integer getActiveMemberCount(Long circleId) {
        try {
            return circleMemberDao.getMemberCount(circleId);
        } catch (Exception e) {
            log.error("获取活跃成员数量失败，circleId: {}", circleId, e);
            return 0;
        }
    }

    @Override
    public List<Long> getJoinedCircleIds(Long userId) {
        try {
            return circleMemberDao.getJoinedCircleIds(userId);
        } catch (Exception e) {
            log.error("获取用户加入的圈子列表失败，userId: {}", userId, e);
            return List.of();
        }
    }

    @Override
    @Transactional
    public void removeAllMembers(Long circleId) {
        try {
            LambdaQueryWrapper<CircleMember> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(CircleMember::getCircleId, circleId);
            
            List<CircleMember> members = this.list(queryWrapper);
            for (CircleMember member : members) {
                member.setStatus(MemberStatus.INACTIVE.getCode());
                this.updateById(member);
            }
            
            log.info("批量移除圈子{}的所有成员，共{}人", circleId, members.size());
        } catch (Exception e) {
            log.error("批量移除圈子成员失败，circleId: {}", circleId, e);
            throw new RuntimeException("批量移除成员失败：" + e.getMessage());
        }
    }

}
