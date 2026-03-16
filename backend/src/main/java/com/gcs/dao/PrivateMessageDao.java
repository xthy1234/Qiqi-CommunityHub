package com.gcs.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.gcs.entity.PrivateMessage;
import com.gcs.vo.ConversationVO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 私信数据访问接口
 * @author 
 * @date 2026-03-16
 */
public interface PrivateMessageDao extends BaseMapper<PrivateMessage> {
    
    /**
     * 分页查询两个用户之间的聊天记录
     * @param page 分页对象
     * @param userId1 用户 ID1
     * @param userId2 用户 ID2
     * @return 聊天记录列表
     */
    List<PrivateMessage> selectChatHistory(IPage<PrivateMessage> page, 
                                           @Param("userId1") Long userId1, 
                                           @Param("userId2") Long userId2);
    
    /**
     * 查询会话列表（每个用户的最新消息）
     * @param userId 当前用户 ID
     * @return 会话列表
     */
    List<ConversationVO> selectConversations(@Param("userId") Long userId);
    
    /**
     * 统计与某用户的未读消息数
     * @param currentUserId 当前用户 ID
     * @param otherUserId 对方用户 ID
     * @return 未读消息数
     */
    Integer countUnreadMessages(@Param("currentUserId") Long currentUserId, 
                                @Param("otherUserId") Long otherUserId);
    
    /**
     * 标记消息为已读
     * @param currentUserId 当前用户 ID
     * @param fromUserId 发送方用户 ID
     * @return 更新行数
     */
    int markAsRead(@Param("currentUserId") Long currentUserId, 
                   @Param("fromUserId") Long fromUserId);
}
