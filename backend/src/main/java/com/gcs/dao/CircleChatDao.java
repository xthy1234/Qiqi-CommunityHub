package com.gcs.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.gcs.entity.CircleChat;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 圈子聊天 DAO
 */
@Mapper
public interface CircleChatDao extends BaseMapper<CircleChat> {
    
    /**
     * 分页查询圈子聊天记录（包含已删除的消息）
     */
    List<CircleChat> selectChatHistoryIncludeDeleted(@Param("circleId") Long circleId);
    
    /**
     * 查询圈子最新消息
     */
    CircleChat selectLastMessage(Long circleId);
    
    /**
     * 根据消息 ID 查询消息
     */
    CircleChat selectMessageById(Long messageId);
    
    /**
     * 查询用户加入的圈子的会话列表
     */
    List<com.gcs.vo.CircleChatSessionVO> selectConversations(@Param("userId") Long userId);
    
    /**
     * 统计圈子的未读消息数
     */
    Integer countUnreadMessages(@Param("userId") Long userId, @Param("circleId") Long circleId);
    
    /**
     * 更新用户的最后阅读时间
     */
    int updateLastReadTime(@Param("userId") Long userId, @Param("circleId") Long circleId);
}
