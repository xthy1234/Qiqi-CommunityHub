package com.gcs.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.extension.service.IService;
import com.gcs.entity.Swiper;
import com.gcs.utils.PageUtils;

import java.util.List;
import java.util.Map;

/**
 * 轮播图服务接口
 * 提供轮播图相关的业务操作
 * @author
 * @date 2026-04-16
 */
public interface SwiperService extends IService<Swiper> {

    /**
     * 分页查询轮播图列表
     *
     * @param params 查询参数
     * @param wrapper 查询条件包装器
     * @return 分页结果
     */
    PageUtils queryPage(Map<String, Object> params, Wrapper<Swiper> wrapper);

    /**
     * 获取所有启用的轮播图
     *
     * @return 轮播图列表
     */
    List<Swiper> getAllEnabledSwipers();

    /**
     * 启用/禁用轮播图
     *
     * @param swiperId 轮播图 ID
     * @param status 状态（0:隐藏 1:显示）
     * @return 操作结果
     */
    boolean updateStatus(Long swiperId, Integer status);

    /**
     * 批量删除轮播图
     *
     * @param swiperIds 轮播图 ID 列表
     * @return 删除结果
     */
    boolean deleteSwipers(List<Long> swiperIds);

    /**
     * 查询轮播图视图
     * @param queryWrapper 查询条件包装器
     * @return 轮播图视图
     */
    Swiper selectView(Wrapper<Swiper> queryWrapper);
}
