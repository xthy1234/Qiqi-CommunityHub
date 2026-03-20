package com.gcs.controller;

import com.gcs.vo.CircleChatMessage;
import com.gcs.service.CircleChatService;
import com.gcs.utils.PageUtils;
import com.gcs.utils.R;
import com.gcs.vo.CircleChatMessageVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 圈子聊天 RESTful 控制器
 * 提供圈子聊天相关的 HTTP 接口
 */
@Slf4j
@Tag(name = "圈子聊天管理", description = "圈子聊天相关的 RESTful API 接口")
@RestController
@RequestMapping("/circles")
public class CircleChatController {

    @Autowired
    private CircleChatService circleChatService;

    /**
     * 获取当前登录用户 ID
     */
    private Long getCurrentUserId(HttpServletRequest request) {
        Object userIdObj = request.getSession().getAttribute("userId");
        if (userIdObj == null) {
            throw new RuntimeException("用户未登录");
        }
        return (Long) userIdObj;
    }

    /**
     * 获取会话列表
     */
    @GetMapping("/chat/conversations")
    @Operation(summary = "获取会话列表", description = "获取当前用户加入的所有圈子的聊天会话列表")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "获取成功"),
            @ApiResponse(responseCode = "401", description = "用户未登录")
    })
    public R getConversations(
            @Parameter(description = "查询参数（page, limit）")
            @RequestParam Map<String, Object> params,
            @Parameter(hidden = true) HttpServletRequest request) {
        try {
            Long currentUserId = getCurrentUserId(request);
            PageUtils page = circleChatService.getConversations(currentUserId, params);
            return R.ok().put("data", page);
        } catch (Exception e) {
            log.error("获取会话列表失败，userId: {}", getCurrentUserId(request), e);
            return R.error(e.getMessage());
        }
    }

    /**
     * 获取圈子聊天记录
     */
    @GetMapping("/{circleId}/chat/history")
    @Operation(summary = "获取圈子聊天记录", description = "分页获取圈子的历史聊天记录")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "获取成功"),
            @ApiResponse(responseCode = "403", description = "无权查看"),
            @ApiResponse(responseCode = "404", description = "圈子不存在")
    })
    public R getChatHistory(
            @Parameter(description = "圈子 ID", required = true)
            @PathVariable("circleId") Long circleId,
            @Parameter(description = "查询参数（page, limit）")
            @RequestParam Map<String, Object> params,
            @Parameter(hidden = true) HttpServletRequest request) {
        try {
            Long currentUserId = getCurrentUserId(request);
            PageUtils page = circleChatService.getChatHistory(circleId, currentUserId, params);
            return R.ok().put("data", page);
        } catch (Exception e) {
            log.error("获取聊天记录失败，circleId: {}, userId: {}", circleId, getCurrentUserId(request), e);
            return R.error(e.getMessage());
        }
    }

    /**
     * 统计未读消息数
     */
    @GetMapping("/{circleId}/chat/unread-count")
    @Operation(summary = "统计未读消息数", description = "统计某个圈子的未读消息数量")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "获取成功"),
            @ApiResponse(responseCode = "403", description = "无权查看")
    })
    public R countUnreadMessages(
            @Parameter(description = "圈子 ID", required = true)
            @PathVariable("circleId") Long circleId,
            @Parameter(hidden = true) HttpServletRequest request) {
        try {
            Long currentUserId = getCurrentUserId(request);
            Integer count = circleChatService.countUnreadMessages(currentUserId, circleId);
            return R.ok().put("data", count);
        } catch (Exception e) {
            log.error("统计未读消息失败，circleId: {}, userId: {}", circleId, getCurrentUserId(request), e);
            return R.error(e.getMessage());
        }
    }

    /**
     * 发送消息（HTTP 接口，可选）
     * 注：推荐使用 WebSocket 发送实时消息
     */
    @PostMapping("/{circleId}/chat/messages")
    @Operation(summary = "发送圈子消息", description = "通过 HTTP 接口发送圈子消息（建议使用 WebSocket）")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "发送成功"),
            @ApiResponse(responseCode = "403", description = "无权发送"),
            @ApiResponse(responseCode = "404", description = "圈子不存在")
    })
    public R sendMessage(
            @Parameter(description = "圈子 ID", required = true)
            @PathVariable("circleId") Long circleId,
            @Parameter(description = "消息内容", required = true)
            @Valid @RequestBody CircleChatMessage chatMessage,
            @Parameter(hidden = true) HttpServletRequest request) {
        try {
            Long currentUserId = getCurrentUserId(request);

            // 调用 Service 发送消息
            CircleChatMessage messageVO = circleChatService.sendMessage(
                    circleId,
                    currentUserId,
                    chatMessage.getContent(),
                    chatMessage.getMsgType()
            );

            // 转换为 RESTful VO
            CircleChatMessageVO vo = convertToRestfulVO(messageVO, currentUserId);

            return R.ok("发送成功").put("data", vo);
        } catch (Exception e) {
            log.error("发送消息失败，circleId: {}", circleId, e);
            return R.error(e.getMessage());
        }
    }

    /**
     * 撤回消息
     */
    @PutMapping("/{circleId}/chat/messages/{messageId}/recall")
    @Operation(summary = "撤回圈子消息", description = "撤回自己发送的消息")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "撤回成功"),
            @ApiResponse(responseCode = "403", description = "无权撤回"),
            @ApiResponse(responseCode = "404", description = "消息不存在")
    })
    public R recallMessage(
            @Parameter(description = "圈子 ID", required = true)
            @PathVariable("circleId") Long circleId,
            @Parameter(description = "消息 ID", required = true)
            @PathVariable("messageId") Long messageId,
            @Parameter(description = "撤回原因（可选）")
            @RequestParam(required = false) String reason,
            @Parameter(hidden = true) HttpServletRequest request) {
        try {
            Long currentUserId = getCurrentUserId(request);

            boolean success = circleChatService.recallMessage(messageId, currentUserId);

            if (!success) {
                return R.error("撤回失败，消息不存在或无权撤回");
            }

            return R.ok("撤回成功");
        } catch (Exception e) {
            log.error("撤回消息失败，circleId: {}, messageId: {}", circleId, messageId, e);
            return R.error(e.getMessage());
        }
    }

    /**
     * 获取圈子最新消息（可选接口）
     */
    @GetMapping("/{circleId}/chat/latest")
    @Operation(summary = "获取圈子最新消息", description = "获取圈子的最后一条消息")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "获取成功"),
            @ApiResponse(responseCode = "404", description = "圈子不存在或无消息")
    })
    public R getLatestMessage(
            @Parameter(description = "圈子 ID", required = true)
            @PathVariable("circleId") Long circleId,
            @Parameter(hidden = true) HttpServletRequest request) {
        try {
            Long currentUserId = getCurrentUserId(request);

            // 验证权限
            Boolean isMember = circleChatService.lambdaQuery()
                    .eq(com.gcs.entity.CircleChat::getCircleId, circleId)
                    .last("LIMIT 1")
                    .count() > 0;

            if (isMember == null || !isMember) {
                return R.error("无权查看");
            }

            var latestMessage = circleChatService.getLastMessage(circleId);

            if (latestMessage == null) {
                return R.ok("暂无消息");
            }

            CircleChatMessageVO vo = new CircleChatMessageVO();
            vo.setId(latestMessage.getId());
            vo.setCircleId(latestMessage.getCircleId());
            vo.setSenderId(latestMessage.getSenderId());
            vo.setContent(latestMessage.getIsRecalled() ? "" : latestMessage.getContent());
            vo.setMsgType(latestMessage.getMsgType());
            vo.setIsRecalled(latestMessage.getIsRecalled());
            vo.setCreateTime(latestMessage.getCreateTime());
            vo.setIsSelf(latestMessage.getSenderId().equals(currentUserId));

            return R.ok().put("data", vo);
        } catch (Exception e) {
            log.error("获取最新消息失败，circleId: {}", circleId, e);
            return R.error(e.getMessage());
        }
    }

    /**
     * 标记圈子消息为已读
     */
    @PostMapping("/{circleId}/chat/read")
    @Operation(summary = "标记圈子消息为已读", description = "用户进入圈子时，调用此接口标记消息为已读")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "标记成功"),
            @ApiResponse(responseCode = "403", description = "无权操作"),
            @ApiResponse(responseCode = "404", description = "圈子不存在")
    })
    public R markAsRead(
            @Parameter(description = "圈子 ID", required = true)
            @PathVariable("circleId") Long circleId,
            @Parameter(hidden = true) HttpServletRequest request) {
        try {
            Long currentUserId = getCurrentUserId(request);

            boolean success = circleChatService.markAsRead(currentUserId, circleId);

            return R.ok("标记成功");
        } catch (Exception e) {
            log.error("标记已读失败，circleId: {}, userId: {}", circleId, getCurrentUserId(request), e);
            return R.error(e.getMessage());
        }
    }

    /**
     * 转换为 RESTful VO
     */
    private CircleChatMessageVO convertToRestfulVO(CircleChatMessage message, Long currentUserId) {
        CircleChatMessageVO vo = new CircleChatMessageVO();
        vo.setId(message.getId());
        vo.setCircleId(message.getCircleId());
        vo.setSenderId(message.getSenderId());

        if (message.getSender() != null) {
            vo.setSenderNickname(message.getSender().getNickname());
            vo.setSenderAvatar(message.getSender().getAvatar());
        }

        vo.setContent(message.getContent());
        vo.setMsgType(message.getMsgType());
        vo.setCreateTime(message.getCreateTime());
        vo.setIsSelf(message.getIsSelf());

        return vo;
    }
}
