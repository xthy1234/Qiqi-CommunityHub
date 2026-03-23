package com.gcs.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.gcs.entity.PrivateMessage;
import com.gcs.utils.PageUtils;
import com.gcs.vo.MessageSendResponseVO;

import java.util.Map;

/**
 * 私信服务接口
 * @author 
 * @date 2026-03-16
 */
public interface PrivateMessageService extends IService<PrivateMessage> {
    
    /**
     * 发送私信
     * @param fromUserId 发送方用户 ID
     * @param toUserId 接收方用户 ID
     * @param content 消息内容
     * @param msgType 消息类型
     * @return 发送结果
     */
    MessageSendResponseVO sendMessage(Long fromUserId, Long toUserId, Map<String, Object> content, Integer msgType);
    
    /**
     * 获取与某用户的聊天记录
     * @param currentUserId 当前用户 ID
     * @param otherUserId 对方用户 ID
     * @param params 查询参数
     * @return 聊天记录分页列表
     */
    PageUtils getChatHistory(Long currentUserId, Long otherUserId, Map<String, Object> params);
    
    /**
     * 获取会话列表
     * @param userId 用户 ID
     * @param params 查询参数
     * @return 会话列表分页
     */
    PageUtils getConversations(Long userId, Map<String, Object> params);
    
    /**
     * 标记消息为已读
     * @param currentUserId 当前用户 ID
     * @param fromUserId 发送方用户 ID
     * @return 操作结果
     */
    boolean markAsRead(Long currentUserId, Long fromUserId);
    
    /**
     * 统计未读消息数
     * @param currentUserId 当前用户 ID
     * @param fromUserId 发送方用户 ID
     * @return 未读消息数量
     */
    Integer countUnreadMessages(Long currentUserId, Long fromUserId);
    
    /**
     * 保存消息（供 WebSocket 使用）
     * @param message 消息实体
     * @return 保存后的消息
     */
    PrivateMessage saveMessage(PrivateMessage message);
    
    /**
     * 删除消息
     * @param messageId 消息 ID
     * @param userId 用户 ID
     * @return 操作结果
     */
    boolean deleteMessage(Long messageId, Long userId);
    
    /**
     * 撤回消息
     * @param messageId 消息 ID
     * @param userId 操作用户 ID
     * @return 操作结果
     */
    boolean recallMessage(Long messageId, Long userId);
    
    /**
     * 获取聊天记录（包含已删除和撤回的消息）
     * @param currentUserId 当前用户 ID
     * @param otherUserId 对方用户 ID
     * @param params 查询参数
     * @return 聊天记录分页列表
     */
    PageUtils getChatHistoryWithDeleted(Long currentUserId, Long otherUserId, Map<String, Object> params);
}
