package com.gcs.controller;

import com.gcs.dto.MessageReadDTO;
import com.gcs.dto.MessageSendDTO;
import com.gcs.entity.PrivateMessage;
import com.gcs.entity.User;
import com.gcs.service.PrivateMessageService;
import com.gcs.service.UserService;
import com.gcs.utils.PageUtils;
import com.gcs.utils.R;
import com.gcs.vo.ConversationVO;
import com.gcs.vo.MessageSendResponseVO;
import com.gcs.vo.MessageVO;
import com.gcs.vo.UserSimpleVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 私信控制器
 * 提供私信相关的 RESTful API 接口
 * @author 
 * @date 2026-03-16
 */
@Slf4j
@Tag(name = "私信管理", description = "私信相关的 RESTful API 接口")
@RestController
@RequestMapping("/messages")
public class MessageController {
    
    @Autowired
    private PrivateMessageService privateMessageService;
    
    @Autowired
    private UserService userService;
    
    /**
     * 获取当前登录用户 ID
     */
    private Long getCurrentUserId(HttpServletRequest request) {
        String userIdStr = getSessionAttribute(request, "userId");
        return userIdStr != null ? Long.parseLong(userIdStr) : null;
    }
    private String getSessionAttribute(HttpServletRequest request, String attributeName) {
        Object attribute = request.getSession().getAttribute(attributeName);
        return attribute != null ? attribute.toString() : null;
    }
    
    /**
     * 发送私信
     */
    @PostMapping
    @Operation(summary = "发送私信", description = "当前登录用户向另一个用户发送一条私信")
    public R sendMessage(
            @Parameter(description = "发送请求", required = true) 
            @Valid @RequestBody MessageSendDTO sendDTO,
            @Parameter(hidden = true) HttpServletRequest request) {
        try {
            Long currentUserId = getCurrentUserId(request);
            if (currentUserId == null) {
                return R.error("用户未登录");
            }
            
            // 不能给自己发消息（可选）
            if (currentUserId.equals(sendDTO.getToUserId())) {
                return R.error("不能给自己发送消息");
            }
            
            MessageSendResponseVO responseVO = privateMessageService.sendMessage(
                currentUserId, 
                sendDTO.getToUserId(), 
                sendDTO.getContent(), 
                sendDTO.getMsgType()
            );
            
            return R.ok("发送成功").put("data", responseVO);
        } catch (Exception e) {
            log.error("发送私信失败", e);
            return R.error(e.getMessage());
        }
    }
    
    /**
     * 获取与某用户的聊天记录
     */
    @GetMapping("/with/{userId}")
    @Operation(summary = "获取聊天记录", description = "获取当前用户与指定用户之间的私信历史")
    public R getChatHistory(
            @Parameter(description = "对方用户 ID", required = true) 
            @PathVariable("userId") Long userId,
            @Parameter(description = "页码", example = "1") 
            @RequestParam(defaultValue = "1") Integer page,
            @Parameter(description = "每页数量", example = "20") 
            @RequestParam(defaultValue = "20") Integer limit,
            @Parameter(hidden = true) HttpServletRequest request) {
        try {
            Long currentUserId = getCurrentUserId(request);
            if (currentUserId == null) {
                return R.error("用户未登录");
            }
            
            Map<String, Object> params = new java.util.HashMap<>();
            params.put("page", page.toString());
            params.put("limit", limit.toString());
            
            PageUtils pageUtils = privateMessageService.getChatHistory(currentUserId, userId, params);
            
            // 转换为 MessageVO 并填充用户信息
            List<MessageVO> voList = convertToVOWithUserInfo((List<PrivateMessage>) pageUtils.getList(), currentUserId);
            pageUtils.setList(voList);
            
            return R.ok().put("data", pageUtils);
        } catch (Exception e) {
            log.error("获取聊天记录失败", e);
            return R.error("获取失败");
        }
    }
    
    /**
     * 获取会话列表
     */
    @GetMapping("/conversations")
    @Operation(summary = "获取会话列表", description = "获取当前用户的所有私信会话列表")
    public R getConversations(
            @Parameter(description = "页码", example = "1") 
            @RequestParam(defaultValue = "1") Integer page,
            @Parameter(description = "每页数量", example = "20") 
            @RequestParam(defaultValue = "20") Integer limit,
            @Parameter(hidden = true) HttpServletRequest request) {
        try {
            Long currentUserId = getCurrentUserId(request);
            if (currentUserId == null) {
                return R.error("用户未登录");
            }
            
            Map<String, Object> params = new java.util.HashMap<>();
            params.put("page", page.toString());
            params.put("limit", limit.toString());
            
            PageUtils pageUtils = privateMessageService.getConversations(currentUserId, params);
            
            return R.ok().put("data", pageUtils);
        } catch (Exception e) {
            log.error("获取会话列表失败", e);
            return R.error("获取失败");
        }
    }
    
    /**
     * 标记消息为已读
     */
    @PutMapping("/read")
    @Operation(summary = "标记消息为已读", description = "将当前用户与某人的所有未读消息标记为已读")
    public R markAsRead(
            @Parameter(description = "标记请求") 
            @RequestBody MessageReadDTO readDTO,
            @Parameter(hidden = true) HttpServletRequest request) {
        try {
            Long currentUserId = getCurrentUserId(request);
            if (currentUserId == null) {
                return R.error("用户未登录");
            }
            
            if (readDTO.getFromUserId() == null) {
                return R.error("参数错误");
            }
            
            boolean result = privateMessageService.markAsRead(currentUserId, readDTO.getFromUserId());
            if (result) {
                return R.ok("标记成功");
            } else {
                return R.error("标记失败");
            }
        } catch (Exception e) {
            log.error("标记消息已读失败", e);
            return R.error("标记失败");
        }
    }
    
    /**
     * 删除单条消息
     */
    @DeleteMapping("/{messageId}")
    @Operation(summary = "删除消息", description = "用户删除自己发送或接收的某条消息")
    @Transactional
    public R deleteMessage(
            @Parameter(description = "消息 ID", required = true) 
            @PathVariable("messageId") Long messageId,
            @Parameter(hidden = true) HttpServletRequest request) {
        try {
            Long currentUserId = getCurrentUserId(request);
            if (currentUserId == null) {
                return R.error("用户未登录");
            }
            
            boolean result = privateMessageService.deleteMessage(messageId, currentUserId);
            if (result) {
                return R.ok("删除成功");
            } else {
                return R.error("删除失败");
            }
        } catch (Exception e) {
            log.error("删除消息失败", e);
            return R.error("删除失败");
        }
    }
    
    /**
     * 批量删除消息
     */
    @PostMapping("/batch-delete")
    @Operation(summary = "批量删除消息", description = "批量删除多个消息")
    @Transactional
    public R batchDeleteMessages(
            @Parameter(description = "消息 ID 数组", required = true) 
            @RequestBody Long[] ids,
            @Parameter(hidden = true) HttpServletRequest request) {
        try {
            Long currentUserId = getCurrentUserId(request);
            if (currentUserId == null) {
                return R.error("用户未登录");
            }
            
            if (ids == null || ids.length == 0) {
                return R.error("请选择要删除的消息");
            }
            
            int successCount = 0;
            for (Long messageId : ids) {
                if (privateMessageService.deleteMessage(messageId, currentUserId)) {
                    successCount++;
                }
            }
            
            return R.ok("成功删除 " + successCount + " 条消息");
        } catch (Exception e) {
            log.error("批量删除消息失败", e);
            return R.error("批量删除失败");
        }
    }
    
    /**
     * 将 PrivateMessage 列表转换为 MessageVO 列表并填充用户信息
     */
    private List<MessageVO> convertToVOWithUserInfo(List<PrivateMessage> messages, Long currentUserId) {
        if (messages == null || messages.isEmpty()) {
            return new ArrayList<>();
        }
        
        // 收集所有需要查询的用户 ID
        Set<Long> userIds = new HashSet<>();
        for (PrivateMessage message : messages) {
            userIds.add(message.getFromUserId());
            userIds.add(message.getToUserId());
        }
        
        // 批量查询用户信息
        List<User> users = userService.listByIds(userIds);
        Map<Long, User> userMap = users.stream()
                .collect(Collectors.toMap(User::getId, u -> u));
        
        // 转换为 VO
        List<MessageVO> voList = new ArrayList<>();
        for (PrivateMessage message : messages) {
            MessageVO vo = new MessageVO();
            vo.setId(message.getId());
            vo.setFromUserId(message.getFromUserId());
            vo.setToUserId(message.getToUserId());
            vo.setContent(message.getContent());
            vo.setMsgType(message.getMsgType());
            vo.setStatus(message.getStatus().getCode());
            vo.setCreateTime(message.getCreateTime());
            
            // 设置是否为自己发送的消息
            vo.setIsSelf(message.getFromUserId().equals(currentUserId));
            
            // 填充发送方用户信息
            User fromUser = userMap.get(message.getFromUserId());
            if (fromUser != null) {
                vo.setFromUser(convertToUserSimpleVO(fromUser));
            } else {
                vo.setFromUser(createDefaultUserSimpleVO(message.getFromUserId()));
            }
            
            // 填充接收方用户信息
            User toUser = userMap.get(message.getToUserId());
            if (toUser != null) {
                vo.setToUser(convertToUserSimpleVO(toUser));
            } else {
                vo.setToUser(createDefaultUserSimpleVO(message.getToUserId()));
            }
            
            voList.add(vo);
        }
        
        return voList;
    }
    
    /**
     * 将 User 转换为 UserSimpleVO
     */
    private UserSimpleVO convertToUserSimpleVO(User user) {
        UserSimpleVO vo = new UserSimpleVO();
        vo.setId(user.getId());
        vo.setNickname(user.getNickname());
        vo.setAvatar(user.getAvatar());
        return vo;
    }
    
    /**
     * 创建默认用户简易信息（当用户不存在时）
     */
    private UserSimpleVO createDefaultUserSimpleVO(Long userId) {
        UserSimpleVO vo = new UserSimpleVO();
        vo.setId(userId);
        vo.setNickname("未知用户");
        vo.setAvatar("");
        return vo;
    }
}
