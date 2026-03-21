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
import com.gcs.vo.CircleChatMessageVO;
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

            // 🔥 5. 更新发送者的 last_read_time（避免自己发送的消息显示为未读）
            circleMemberDao.updateLastReadTime(senderId, circleId);
            log.debug("✅ 已更新发送者的最后阅读时间：userId={}, circleId={}", senderId, circleId);

            // 6. 转换为 WebSocket 消息对象
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
    public PageUtils getChatHistoryWithUserInfo(Long circleId, Long currentUserId, Map<String, Object> params) {
        try {
            // 1. 验证权限：必须是圈子成员
            Boolean isMember = circleMemberDao.isMember(circleId, currentUserId);
            if (isMember == null || !isMember) {
                throw new RuntimeException("无权查看聊天记录，非圈子成员");
            }

            // 2. 🔥 使用 DAO 的自定义查询（包含已删除的消息）
            int page = Integer.parseInt(params.getOrDefault("page", "1").toString());
            int limit = Integer.parseInt(params.getOrDefault("limit", "20").toString());
            
            // 直接查询所有消息（包括已删除的）
            List<CircleChat> allChats = circleChatDao.selectChatHistoryIncludeDeleted(circleId);
            
            // 3. 转换为 VO 并填充用户信息
            List<CircleChatMessageVO> voList = new java.util.ArrayList<>();
            for (CircleChat chat : allChats) {
                CircleChatMessageVO vo = convertToRestfulVO(chat, currentUserId);
                voList.add(vo);
            }
            
            // 4. 手动分页
            int total = voList.size();
            int fromIndex = (page - 1) * limit;
            int toIndex = Math.min(fromIndex + limit, total);
            
            List<CircleChatMessageVO> pagedList;
            if (fromIndex >= total) {
                pagedList = List.of();
            } else {
                pagedList = voList.subList(fromIndex, toIndex);
            }

            // 5. 设置回 PageUtils
            PageUtils pageUtils = new PageUtils(pagedList, total, limit, page);
            
            log.info("获取圈子聊天记录成功，circleId: {}, userId: {}, total: {}", 
                     circleId, currentUserId, total);
            
            return pageUtils;
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
    @Transactional
    public boolean deleteMessage(Long messageId, Long userId) {
        try {
            // 1. 查询消息
            CircleChat message = this.getById(messageId);
            if (message == null) {
                log.warn("删除消息不存在：messageId={}", messageId);
                return false;
            }

            // 2. 查询用户在圈子中的角色
            var member = circleMemberDao.selectOne(
                new LambdaQueryWrapper<com.gcs.entity.CircleMember>()
                    .eq(com.gcs.entity.CircleMember::getCircleId, message.getCircleId())
                    .eq(com.gcs.entity.CircleMember::getUserId, userId)
            );

            if (member == null) {
                log.warn("删除失败：用户不是圈子成员，userId={}, circleId={}", 
                        userId, message.getCircleId());
                return false;
            }

            // 3. 验证权限：只有圈主 (2) 或管理员 (1) 可以删除
            Integer role = member.getRole();
            if (role == null || role < 1) {
                log.warn("删除失败：用户无权限，userId={}, role={}", userId, role);
                return false;
            }

            // 4. 执行删除操作（标记 deleted_by_admin=true）
            message.setDeletedByAdmin(true);
            message.setDeletedBy(userId);  // 🔥 记录删除者 ID
            message.setDeletedTime(LocalDateTime.now());  // 🔥 记录删除时间
            message.setUpdateTime(LocalDateTime.now());
            this.updateById(message);

            log.info("✅ 圈子消息已删除（管理员操作）：messageId={}, circleId={}, operatorId={}, role={}", 
                    messageId, message.getCircleId(), userId, role);
            return true;
        } catch (Exception e) {
            log.error("删除圈子消息失败，messageId: {}, userId: {}", messageId, userId, e);
            return false;
        }
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
     * 转换为 RESTful VO（用于历史记录）
     */
    private CircleChatMessageVO convertToRestfulVO(CircleChat chat, Long currentUserId) {
        CircleChatMessageVO vo = new CircleChatMessageVO();
        vo.setId(chat.getId());
        vo.setCircleId(chat.getCircleId());
        vo.setSenderId(chat.getSenderId());
        
        // 获取发送者信息
        User sender = userDao.selectById(chat.getSenderId());
        if (sender != null) {
            vo.setSender(convertToUserSimpleVO(sender));
        }
        
        // 🔥 处理撤回和删除的消息内容
        if (chat.getDeletedByAdmin()) {
            vo.setContent("");  // 删除后内容为空
        } else if (chat.getIsRecalled()) {
            vo.setContent("");  // 撤回后内容为空
        } else {
            vo.setContent(chat.getContent());
        }
        
        vo.setMsgType(chat.getMsgType());
        vo.setIsRecalled(chat.getIsRecalled());
        vo.setDeletedByAdmin(chat.getDeletedByAdmin());
        
        // 🔥 添加删除者信息（如果是删除状态）
        if (chat.getDeletedByAdmin() && chat.getDeletedBy() != null) {
            User deleter = userDao.selectById(chat.getDeletedBy());
            if (deleter != null) {
                vo.setDeleter(convertToUserSimpleVO(deleter));
            }
        }
        
        vo.setDeletedTime(chat.getDeletedTime());
        vo.setCreateTime(chat.getCreateTime());
        vo.setIsSelf(chat.getSenderId().equals(currentUserId));
        
        return vo;
    }

    /**
     * 转换为 WebSocket 消息对象
     */
    private CircleChatMessage convertToWebSocketMessage(CircleChat chat, Long currentUserId) {
        CircleChatMessage messageVO = new CircleChatMessage();
        messageVO.setId(chat.getId());
        messageVO.setCircleId(chat.getCircleId());
        messageVO.setSenderId(chat.getSenderId());
        
        // 🔥 处理撤回和删除的消息内容
        if (chat.getDeletedByAdmin()) {
            messageVO.setContent("");  // 删除后内容为空
        } else if (chat.getIsRecalled()) {
            messageVO.setContent("");  // 撤回后内容为空
        } else {
            messageVO.setContent(chat.getContent());
        }
        
        messageVO.setMsgType(chat.getMsgType());
        messageVO.setStatus(chat.getStatus());
        messageVO.setCreateTime(chat.getCreateTime());
        messageVO.setIsSelf(chat.getSenderId().equals(currentUserId));
        messageVO.setIsRecalled(chat.getIsRecalled());
        messageVO.setDeletedByAdmin(chat.getDeletedByAdmin());
        
        // 🔥 添加删除者信息（如果是删除状态）
        if (chat.getDeletedByAdmin() && chat.getDeletedBy() != null) {
            User deleter = userDao.selectById(chat.getDeletedBy());
            if (deleter != null) {
                messageVO.setDeleter(convertToUserSimpleVO(deleter));
            }
        }
        
        messageVO.setDeletedTime(chat.getDeletedTime());
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
