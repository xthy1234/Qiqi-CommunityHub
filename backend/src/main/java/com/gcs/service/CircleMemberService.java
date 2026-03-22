package com.gcs.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.gcs.entity.CircleMember;
import com.gcs.vo.CircleMemberVO;
import com.gcs.utils.PageUtils;

import java.util.List;
import java.util.Map;

/**
 * 圈子成员服务接口
 * @author
 * @date 2026-03-17
 */
public interface CircleMemberService extends IService<CircleMember> {

    /**
     * 添加圈子成员
     * @param circleId 圈子 ID
     * @param userId 用户 ID
     * @param role 角色
     */
    void addMember(Long circleId, Long userId, Integer role);

    /**
     * 移除圈子成员
     * @param circleId 圈子 ID
     * @param userId 用户 ID
     */
    void removeMember(Long circleId, Long userId);

    /**
     * 获取圈子所有成员 ID
     * @param circleId 圈子 ID
     * @return 成员 ID 列表
     */
    List<Long> getMemberIds(Long circleId);

    /**
     * 更新成员角色
     * @param circleId 圈子 ID
     * @param userId 用户 ID
     * @param role 新角色
     */
    void updateMemberRole(Long circleId, Long userId, Integer role);

    /**
     * 分页获取成员列表（包含用户信息）
     * @param circleId 圈子 ID
     * @param params 查询参数
     * @return 分页结果
     */
    PageUtils getMemberPageWithUserInfo(Long circleId, Map<String, Object> params);

    /**
     * 分页获取成员列表
     * @param circleId 圈子 ID
     * @param params 查询参数
     * @return 分页结果
     */
    PageUtils getMemberPage(Long circleId, Map<String, Object> params);

    /**
     * 获取成员详情
     * @param circleId 圈子 ID
     * @param userId 用户 ID
     * @return 成员信息
     */
    CircleMemberVO getMemberDetail(Long circleId, Long userId);

    /**
     * 退出圈子
     * @param circleId 圈子 ID
     * @param userId 用户 ID
     */
    void leaveCircle(Long circleId, Long userId);

    /**
     * 检查用户是否有管理权限
     * @param circleId 圈子 ID
     * @param userId 用户 ID
     * @return 是否有管理权限
     */
    boolean hasManagePermission(Long circleId, Long userId);

    /**
     * 检查用户是否是圈子成员
     * @param circleId 圈子 ID
     * @param userId 用户 ID
     * @return 是否是成员
     */
    Boolean isMember(Long circleId, Long userId);

    /**
     * 获取用户在圈子中的角色
     * @param circleId 圈子 ID
     * @param userId 用户 ID
     * @return 角色类型
     */
    Integer getUserRoleInCircle(Long circleId, Long userId);
}
