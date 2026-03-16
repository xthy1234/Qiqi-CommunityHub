package com.gcs.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.gcs.entity.Interaction;
import com.gcs.entity.view.InteractionView;
import com.gcs.enums.ContentType;
import com.gcs.enums.InteractionActionType;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 互动记录 DAO
 */
public interface InteractionDao extends BaseMapper<Interaction> {

    /**
     * 根据用户和內容查询互动记录
     */
    Interaction selectByUserAndContent(@Param("userId") Long userId,
                                       @Param("contentId") Long contentId,
                                       @Param("actionType") InteractionActionType actionType,
                                       @Param("tableName") ContentType tableName);

    /**
     * 根据用户和內容查询互动记录（包括已删除的）
     */
    List<Interaction> selectByUserAndContentList(@Param("userId") Long userId,
                                                 @Param("contentId") Long contentId,
                                                 @Param("actionType") InteractionActionType actionType,
                                                 @Param("tableName") ContentType tableName);


    /**
     * 统计用户操作数量
     */
    Integer countByUser(@Param("userId") Long userId,
                        @Param("actionType") InteractionActionType actionType);

    /**
     * 批量删除用户操作
     */
    int deleteBatchByUser(@Param("userId") Long userId,
                          @Param("list") List<Long> actionIds);
    
    /**
     * 查询列表视图（分页）
     */
    List<InteractionView> selectListView(IPage<InteractionView> page, 
                                         @Param("ew") Wrapper<Interaction> queryWrapper);
    
    /**
     * 查询单个视图
     */
    InteractionView selectView(@Param("ew") Wrapper<Interaction> queryWrapper);
}
