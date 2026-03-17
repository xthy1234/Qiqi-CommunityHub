package com.gcs.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.gcs.entity.Circle;
import com.gcs.entity.view.CircleView;

import org.apache.ibatis.annotations.Param;
import java.util.List;

/**
 * 圈子数据访问接口
 * @author 
 * @date 2026-03-17
 */
public interface CircleDao extends BaseMapper<Circle> {
    
    /**
     * 查询圈子列表视图
     * @param wrapper 查询条件包装器
     * @return 圈子视图列表
     */
    List<CircleView> selectListView(@Param("ew") Wrapper<Circle> wrapper);

    /**
     * 分页查询圈子列表视图
     * @param page 分页对象
     * @param wrapper 查询条件包装器
     * @return 圈子视图列表
     */
    List<CircleView> selectListView(IPage<CircleView> page, @Param("ew") Wrapper<Circle> wrapper);
    
    /**
     * 查询单个圈子视图
     * @param wrapper 查询条件包装器
     * @return 圈子视图
     */
    CircleView selectView(@Param("ew") Wrapper<Circle> wrapper);
}
