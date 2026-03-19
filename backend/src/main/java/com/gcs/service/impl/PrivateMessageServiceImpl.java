package com.gcs.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.gcs.dao.PrivateMessageDao;
import com.gcs.entity.PrivateMessage;
import com.gcs.enums.MessageStatus;
import com.gcs.service.PrivateMessageService;
import com.gcs.utils.PageUtils;
import com.gcs.vo.ConversationVO;
import com.gcs.vo.MessageSendResponseVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 私信服务实现类
 * @author 
 * @date 2026-03-16
 */
@Slf4j
@Service
public class PrivateMessageServiceImpl extends ServiceImpl<PrivateMessageDao, PrivateMessage> 
        implements PrivateMessageService {
    
    @Autowired
    private PrivateMessageDao privateMessageDao;
    
    @Override
    public MessageSendResponseVO sendMessage(Long fromUserId, Long toUserId, String content, Integer msgType) {
        // 验证接收方用户是否存在（可选）
        
        PrivateMessage message = new PrivateMessage();
        message.setFromUserId(fromUserId);
        message.setToUserId(toUserId);
        message.setContent(content);
        message.setMsgType(msgType);
        message.setStatus(MessageStatus.UNREAD);
        
        this.save(message);
        
        MessageSendResponseVO responseVO = new MessageSendResponseVO();
        responseVO.setMessageId(message.getId());
        responseVO.setCreateTime(message.getCreateTime());
        
        return responseVO;
    }
    
    @Override
    public PageUtils getChatHistory(Long currentUserId, Long otherUserId, Map<String, Object> params) {
        int page = Integer.parseInt(params.getOrDefault("page", "1").toString());
        int limit = Integer.parseInt(params.getOrDefault("limit", "20").toString());
        
        IPage<PrivateMessage> pageObj = new Page<>(page, limit);
        List<PrivateMessage> messages = privateMessageDao.selectChatHistory(pageObj, currentUserId, otherUserId);
        
        // 将消息标记为已读（如果是接收到的消息）
        markMessagesAsRead(currentUserId, otherUserId);
        
        PageUtils pageUtils = new PageUtils(messages, pageObj.getTotal(), limit, page);
        return pageUtils;
    }
    
    @Override
    public PageUtils getConversations(Long userId, Map<String, Object> params) {
        int page = Integer.parseInt(params.getOrDefault("page", "1").toString());
        int limit = Integer.parseInt(params.getOrDefault("limit", "20").toString());
        
        // 查询会话列表
        List<ConversationVO> conversations = privateMessageDao.selectConversations(userId);
        
        // 手动分页（如果数据量大，建议优化 SQL 使用数据库分页）
        int total = conversations.size();
        int fromIndex = (page - 1) * limit;
        int toIndex = Math.min(fromIndex + limit, total);
        
        List<ConversationVO> pagedConversations = conversations.subList(fromIndex, toIndex);
        
        PageUtils pageUtils = new PageUtils(pagedConversations, total, limit, page);
        return pageUtils;
    }
    
    @Override
    @Transactional
    public boolean markAsRead(Long currentUserId, Long fromUserId) {
        int rows = privateMessageDao.markAsRead(currentUserId, fromUserId);
        log.info("标记已读：currentUserId={}, fromUserId={}, 更新行数={}", 
                currentUserId, fromUserId, rows);
        return rows > 0;
    }
    
    @Override
    public Integer countUnreadMessages(Long currentUserId, Long fromUserId) {
        return privateMessageDao.countUnreadMessages(currentUserId, fromUserId);
    }
    
    @Transactional
    public PrivateMessage saveMessage(PrivateMessage message) {
        this.save(message);
        return message;
    }
    
    @Override
    @Transactional
    public boolean deleteMessage(Long messageId, Long userId) {
        PrivateMessage message = this.getById(messageId);
        if (message == null) {
            log.warn("消息不存在：messageId={}", messageId);
            return false;
        }
        
        // 标记发送方或接收方的删除标志
        if (message.getFromUserId().equals(userId)) {
            message.setDeletedBySender(true);
            log.info("发送方 {} 删除了消息 {}", userId, messageId);
        } else if (message.getToUserId().equals(userId)) {
            message.setDeletedByRecipient(true);
            log.info("接收方 {} 删除了消息 {}", userId, messageId);
        } else {
            log.warn("无权删除消息：userId={}, messageId={}", userId, messageId);
            return false; // 无权删除
        }
        
        this.updateById(message);
        
        return true;
    }
    
    @Override
    @Transactional
    public boolean recallMessage(Long messageId, Long userId) {
        PrivateMessage message = this.getById(messageId);
        if (message == null) {
            return false;
        }
        
        // 只有发送方才能撤回消息
        if (!message.getFromUserId().equals(userId)) {
            return false;
        }
        
        // 设置撤回标志
        message.setIsRecalled(true);
//        message.setContent("消息已撤回");
        this.updateById(message);
        
        return true;
    }
    
    @Override
    public PageUtils getChatHistoryWithDeleted(Long currentUserId, Long otherUserId, Map<String, Object> params) {
        int page = Integer.parseInt(params.getOrDefault("page", "1").toString());
        int limit = Integer.parseInt(params.getOrDefault("limit", "20").toString());
        
        IPage<PrivateMessage> pageObj = new Page<>(page, limit);
        List<PrivateMessage> messages = privateMessageDao.selectChatHistoryWithDeleted(pageObj, currentUserId, otherUserId, currentUserId);
        
        // 将消息标记为已读（如果是接收到的消息）
        markMessagesAsRead(currentUserId, otherUserId);
        
        PageUtils pageUtils = new PageUtils(messages, pageObj.getTotal(), limit, page);
        return pageUtils;
    }
    
    /**
     * 将当前用户收到的消息标记为已读
     */
    private void markMessagesAsRead(Long currentUserId, Long otherUserId) {
        QueryWrapper<PrivateMessage> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("to_user_id", currentUserId)
                   .eq("from_user_id", otherUserId)
                   .eq("status", MessageStatus.UNREAD.getCode());
        
        List<PrivateMessage> unreadMessages = this.list(queryWrapper);
        if (!unreadMessages.isEmpty()) {
            for (PrivateMessage message : unreadMessages) {
                message.setStatus(MessageStatus.READ);
            }
            this.updateBatchById(unreadMessages);
        }
    }
}
