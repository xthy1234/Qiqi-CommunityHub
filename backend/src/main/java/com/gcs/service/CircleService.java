package com.gcs.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.gcs.entity.Circle;
import com.gcs.dto.*;
import com.gcs.utils.PageUtils;

import java.util.Map;

/**
 * 圈子服务接口
 * @author 
 * @date 2026-03-17
 */
public interface CircleService extends IService<Circle> {

    /**
     * 分页查询圈子列表
     * @param params 查询参数
     * @return 分页结果
     */
    PageUtils queryPage(Map<String, Object> params);
    
    /**
     * 创建圈子
     * @param createDTO 创建信息
     * @param userId 用户 ID
     * @return 创建的圈子
     */
    Circle createCircle(CircleCreateDTO createDTO, Long userId);
    
    /**
     * 获取圈子详情
     * @param circleId 圈子 ID
     * @param currentUserId 当前用户 ID
     * @return 圈子详情
     */
    CircleDetailVO getCircleDetail(Long circleId, Long currentUserId);
    
    /**
     * 更新圈子信息
     * @param circleId 圈子 ID
     * @param updateDTO 更新信息
     * @param userId 用户 ID
     */
    void updateCircle(Long circleId, CircleUpdateDTO updateDTO, Long userId);
    
    /**
     * 解散圈子
     * @param circleId 圈子 ID
     * @param userId 用户 ID
     */
    void dissolveCircle(Long circleId, Long userId);
    
    /**
     * 退出圈子
     * @param circleId 圈子 ID
     * @param userId 用户 ID
     */
    void leaveCircle(Long circleId, Long userId);
    
    /**
     * 获取用户加入的圈子列表
     * @param currentUserId 当前用户 ID
     * @param params 查询参数
     * @return 分页结果
     */
    PageUtils getMyCircles(Long currentUserId, Map<String, Object> params);
    
    /**
     * 发现公开圈子
     * @param params 查询参数（包含 keyword、page、limit）
     * @param currentUserId 当前用户 ID（可选）
     * @return 分页结果
     */
    PageUtils getPublicCircles(Map<String, Object> params, Long currentUserId);
    
    /**
     * 检查用户是否有权限管理圈子
     * @param circleId 圈子 ID
     * @param userId 用户 ID
     * @return 是否有权限
     */
    boolean hasManagePermission(Long circleId, Long userId);
}
