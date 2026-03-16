package com.gcs.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.gcs.entity.Swiper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 轮播图数据访问接口
 * 提供轮播图相关的数据库操作
 * @author
 * @email
 * @date 2026-04-16
 */
public interface SwiperDao extends BaseMapper<Swiper> {

    /**
     * 查询轮播图列表视图
     * @param wrapper 查询条件包装器
     * @return 轮播图列表
     */
    List<Swiper> selectListView(@Param("ew") com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<Swiper> wrapper);

    /**
     * 分页查询轮播图列表视图
     * @param page 分页对象
     * @param wrapper 查询条件包装器
     * @return 轮播图列表
     */
    List<Swiper> selectListView(com.baomidou.mybatisplus.core.metadata.IPage<Swiper> page, @Param("ew") com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<Swiper> wrapper);

    /**
     * 根据 ID 查询轮播图视图
     * @param wrapper 查询条件包装器
     * @return 轮播图视图
     */
    Swiper selectView(@Param("ew") com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Swiper> wrapper);

    /**
     * 根据 ID 查询轮播图视图（支持 QueryWrapper）
     * @param wrapper 查询条件包装器
     * @return 轮播图视图
     */
    Swiper selectView(@Param("ew") com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<Swiper> wrapper);

    /**
     * 获取所有启用的轮播图
     * @return 轮播图列表
     */
    List<Swiper> selectAllEnabled();
}
