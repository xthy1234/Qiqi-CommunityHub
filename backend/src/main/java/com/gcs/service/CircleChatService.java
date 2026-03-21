package com.gcs.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.gcs.vo.CircleChatMessage;
import com.gcs.entity.CircleChat;
import com.gcs.utils.PageUtils;

import java.util.Map;

/**
 * 圈子聊天服务接口
 * @author
 * @date 2026-03-20
 */
public interface CircleChatService extends IService<CircleChat> {

    /**
     * 发送圈子消息
     * @param circleId 圈子 ID
     * @param senderId 发送者 ID
     * @param content 消息内容
     * @param msgType 消息类型
     * @return 发送后的消息
     */
    CircleChatMessage sendMessage(Long circleId, Long senderId, String content, Integer msgType);

    /**
     * 获取圈子聊天记录
     * @param circleId 圈子 ID
     * @param currentUserId 当前用户 ID
     * @param params 查询参数
     * @return 聊天记录分页列表
     */
    PageUtils getChatHistory(Long circleId, Long currentUserId, Map<String, Object> params);

    /**
     * 获取圈子聊天记录（包含用户信息）
     * @param circleId 圈子 ID
     * @param currentUserId 当前用户 ID
     * @param params 查询参数
     * @return 聊天记录分页列表（VO 格式）
     */
    PageUtils getChatHistoryWithUserInfo(Long circleId, Long currentUserId, Map<String, Object> params);

    /**
     * 保存消息（供 WebSocket 使用）
     * @param chat 消息实体
     * @return 保存后的消息
     */
    CircleChat saveMessage(CircleChat chat);

    /**
     * 撤回消息
     * @param messageId 消息 ID
     * @param userId 操作用户 ID
     * @return 操作结果
     */
    boolean recallMessage(Long messageId, Long userId);

    /**
     * 删除消息（管理员/圈主权限）
     * @param messageId 消息 ID
     * @param userId 操作用户 ID
     * @return 操作结果
     */
    boolean deleteMessage(Long messageId, Long userId);

    /**
     * 获取圈子最新消息
     * @param circleId 圈子 ID
     * @return 最新消息
     */
    CircleChat getLastMessage(Long circleId);

    /**
     * 获取会话列表
     * @param userId 用户 ID
     * @param params 查询参数
     * @return 会话列表分页
     */
    PageUtils getConversations(Long userId, Map<String, Object> params);

    /**
     * 统计未读消息数
     * @param userId 用户 ID
     * @param circleId 圈子 ID
     * @return 未读消息数
     */
    Integer countUnreadMessages(Long userId, Long circleId);

    /**
     * 标记圈子消息为已读
     * @param userId 用户 ID
     * @param circleId 圈子 ID
     * @return 操作结果
     */
    boolean markAsRead(Long userId, Long circleId);
}
