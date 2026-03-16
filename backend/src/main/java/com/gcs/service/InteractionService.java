package com.gcs.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.extension.service.IService;
import com.gcs.enums.ContentType;
import com.gcs.enums.InteractionActionType;
import com.gcs.utils.PageUtils;
import com.gcs.entity.Interaction;
import com.gcs.entity.view.InteractionView;

import java.util.List;
import java.util.Map;

/**
 * 互动记录服务接口
 */
public interface InteractionService extends IService<Interaction> {

    PageUtils queryPage(Map<String, Object> params);

    List<InteractionView> selectListView(Wrapper<Interaction> queryWrapper);

    InteractionView selectView(Wrapper<Interaction> queryWrapper);

    PageUtils queryPage(Map<String, Object> params, Wrapper<Interaction> queryWrapper);


    public boolean addInteraction(Interaction interaction);

    /**
     * 取消互动操作
     */
    boolean removeInteraction(Long userId, Long contentId, InteractionActionType actionType, ContentType tableName);

    /**
     * 检查是否存在互动记录
     */
    boolean existsInteraction(Long userId, Long contentId, InteractionActionType actionType, ContentType contentType);

    /**
     * 检查是否有有效的互动记录
     */
    boolean hasValidInteraction(Long userId, Long contentId, InteractionActionType actionType, ContentType tableName);

    /**
     * 获取用户的互动记录列表（不分页）
     * @param userId 用户 ID
     * @param actionType 操作类型
     * @param tableName 表名（内容类型）
     * @return 互动记录列表
     */
    List<Interaction> getUserInteractionsList(Long userId, InteractionActionType actionType, ContentType tableName);

    PageUtils getUserInteractions(Long userId, InteractionActionType actionType, Map<String, Object> params);

    Integer countUserInteractions(Long userId, InteractionActionType actionType);

    boolean batchRemoveInteractions(Long userId, List<Long> interactionIds);
}
