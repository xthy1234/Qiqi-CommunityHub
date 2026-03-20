package com.gcs.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.gcs.dao.CircleChatDao;
import com.gcs.dao.CircleMemberDao;
import com.gcs.dao.UserDao;
import com.gcs.vo.CircleChatMessage;
import com.gcs.entity.Circle;
import com.gcs.entity.CircleChat;
import com.gcs.entity.User;
import com.gcs.enums.CommonStatus;
import com.gcs.service.CircleChatService;
import com.gcs.service.CircleService;
import com.gcs.utils.PageUtils;
import com.gcs.utils.Query;
import com.gcs.vo.CircleChatSessionVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 圈子聊天服务实现类
 * @author
 * @date 2026-03-20
 */
@Slf4j
@Service("circleChatService")
public class CircleChatServiceImpl extends ServiceImpl<CircleChatDao, CircleChat> implements CircleChatService {

    @Autowired
    private CircleChatDao circleChatDao;

    @Autowired
    private CircleService circleService;

    @Autowired
    private CircleMemberDao circleMemberDao;

    @Autowired
    private UserDao userDao;

    @Override
    @Transactional
    public CircleChatMessage sendMessage(Long circleId, Long senderId, String content, Integer msgType) {
        try {
            // 1. 验证圈子是否存在且正常
            Circle circle = circleService.getById(circleId);
            if (circle == null || circle.getStatus() == CommonStatus.DISABLED) {
                throw new RuntimeException("圈子不存在或已解散");
            }

            // 2. 验证用户是否是圈子成员
            Boolean isMember = circleMemberDao.isMember(circleId, senderId);
            if (isMember == null || !isMember) {
                throw new RuntimeException("只有圈子成员才能发送消息");
            }

            // 3. 创建消息对象
            CircleChat chat = new CircleChat();
            chat.setCircleId(circleId);
            chat.setSenderId(senderId);
            chat.setContent(content);
            chat.setMsgType(msgType != null ? msgType : 0);
            chat.setStatus(0); // 默认未读
            chat.setIsRecalled(false);
            chat.setCreateTime(LocalDateTime.now());

            // 4. 保存消息
            this.save(chat);
            log.info("✅ 圈子消息已保存，messageId={}, circleId={}, senderId={}",
                    chat.getId(), circleId, senderId);

            // 5. 转换为 WebSocket 消息对象
            return convertToWebSocketMessage(chat, senderId);
        } catch (Exception e) {
            log.error("发送圈子消息失败，circleId: {}, senderId: {}", circleId, senderId, e);
            throw new RuntimeException("发送消息失败：" + e.getMessage());
        }
    }

    @Override
    public PageUtils getChatHistory(Long circleId, Long currentUserId, Map<String, Object> params) {
        try {
            // 1. 验证权限：必须是圈子成员
            Boolean isMember = circleMemberDao.isMember(circleId, currentUserId);
            if (isMember == null || !isMember) {
                throw new RuntimeException("无权查看聊天记录，非圈子成员");
            }

            // 2. 分页查询
            IPage<CircleChat> chatPage = new Query<CircleChat>(params).getPage();
            LambdaQueryWrapper<CircleChat> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(CircleChat::getCircleId, circleId)
                    .orderByDesc(CircleChat::getCreateTime);

            IPage<CircleChat> resultPage = this.page(chatPage, queryWrapper);

            return new PageUtils(resultPage);
        } catch (Exception e) {
            log.error("获取聊天记录失败，circleId: {}, userId: {}", circleId, currentUserId, e);
            throw new RuntimeException("获取聊天记录失败：" + e.getMessage());
        }
    }

    @Override
    public CircleChat saveMessage(CircleChat chat) {
        try {
            this.save(chat);
            log.info("圈子消息已保存（WebSocket），messageId={}", chat.getId());
            return chat;
        } catch (Exception e) {
            log.error("保存圈子消息失败", e);
            throw new RuntimeException("保存消息失败：" + e.getMessage());
        }
    }

    @Override
    @Transactional
    public boolean recallMessage(Long messageId, Long userId) {
        try {
            // 1. 查询消息
            CircleChat message = this.getById(messageId);
            if (message == null) {
                log.warn("撤回消息不存在：messageId={}", messageId);
                return false;
            }

            // 2. 只有发送者才能撤回
            if (!message.getSenderId().equals(userId)) {
                log.warn("无权限撤回消息：messageId={}, userId={}", messageId, userId);
                return false;
            }

            // 3. 执行撤回操作
            message.setIsRecalled(true);
            message.setContent(""); // 根据规范，撤回时 content 置空
            message.setUpdateTime(LocalDateTime.now());
            this.updateById(message);

            log.info("✅ 圈子消息已撤回：messageId={}, circleId={}", messageId, message.getCircleId());
            return true;
        } catch (Exception e) {
            log.error("撤回圈子消息失败，messageId: {}, userId: {}", messageId, userId, e);
            return false;
        }
    }

    @Override
    public boolean deleteMessage(Long messageId, Long userId) {
        // TODO: 后续实现删除功能
        log.info("删除功能待实现：messageId={}, userId={}", messageId, userId);
        return false;
    }

    @Override
    public CircleChat getLastMessage(Long circleId) {
        try {
            return circleChatDao.selectLastMessage(circleId);
        } catch (Exception e) {
            log.error("获取最新消息失败，circleId: {}", circleId, e);
            return null;
        }
    }

    @Override
    public PageUtils getConversations(Long userId, Map<String, Object> params) {
        try {
            int page = Integer.parseInt(params.getOrDefault("page", "1").toString());
            int limit = Integer.parseInt(params.getOrDefault("limit", "20").toString());

            // 查询会话列表
            List<CircleChatSessionVO> conversations = circleChatDao.selectConversations(userId);

            // 手动分页
            int total = conversations.size();
            int fromIndex = (page - 1) * limit;
            int toIndex = Math.min(fromIndex + limit, total);

            List<CircleChatSessionVO> pagedConversations;
            if (fromIndex >= total) {
                pagedConversations = List.of();
            } else {
                pagedConversations = conversations.subList(fromIndex, toIndex);
            }

            PageUtils pageUtils = new PageUtils(pagedConversations, total, limit, page);
            log.info("获取会话列表成功，userId={}, total={}, page={}, limit={}", userId, total, page, limit);

            return pageUtils;
        } catch (Exception e) {
            log.error("获取会话列表失败，userId: {}", userId, e);
            throw new RuntimeException("获取会话列表失败：" + e.getMessage());
        }
    }

    @Override
    public Integer countUnreadMessages(Long userId, Long circleId) {
        try {
            Integer count = circleChatDao.countUnreadMessages(userId, circleId);
            log.debug("统计未读消息：userId={}, circleId={}, count={}", userId, circleId, count);
            return count != null ? count : 0;
        } catch (Exception e) {
            log.error("统计未读消息失败，userId: {}, circleId: {}", userId, circleId, e);
            return 0;
        }
    }

    @Override
    @Transactional
    public boolean markAsRead(Long userId, Long circleId) {
        try {
            // 验证用户是否是圈子成员
            Boolean isMember = circleMemberDao.isMember(circleId, userId);
            if (isMember == null || !isMember) {
                throw new RuntimeException("无权操作，非圈子成员");
            }

            // 更新最后阅读时间
            int rows = circleMemberDao.updateLastReadTime(userId, circleId);
            
            log.info("✅ 圈子消息已标记为已读：userId={}, circleId={}", userId, circleId);
            return rows > 0;
        } catch (Exception e) {
            log.error("标记已读失败，userId: {}, circleId: {}", userId, circleId, e);
            throw new RuntimeException("标记已读失败：" + e.getMessage());
        }
    }

    /**
     * 转换为 WebSocket 消息对象
     */
    private CircleChatMessage convertToWebSocketMessage(CircleChat chat, Long currentUserId) {
        CircleChatMessage messageVO = new CircleChatMessage();
        messageVO.setId(chat.getId());
        messageVO.setCircleId(chat.getCircleId());
        messageVO.setSenderId(chat.getSenderId());
        messageVO.setContent(chat.getIsRecalled() ? "" : chat.getContent());
        messageVO.setMsgType(chat.getMsgType());
        messageVO.setStatus(chat.getStatus());
        messageVO.setCreateTime(chat.getCreateTime());
        messageVO.setIsSelf(chat.getSenderId().equals(currentUserId));
        messageVO.setAction("SEND");

        // 获取发送者信息
        User sender = userDao.selectById(chat.getSenderId());
        if (sender != null) {
            messageVO.setSender(convertToUserSimpleVO(sender));
        }

        return messageVO;
    }

    /**
     * 将 User 转换为 UserSimpleVO
     */
    private com.gcs.vo.UserSimpleVO convertToUserSimpleVO(User user) {
        if (user == null) {
            return null;
        }
        com.gcs.vo.UserSimpleVO vo = new com.gcs.vo.UserSimpleVO();
        vo.setId(user.getId());
        vo.setNickname(user.getNickname());
        vo.setAvatar(user.getAvatar());
        return vo;
    }
}
