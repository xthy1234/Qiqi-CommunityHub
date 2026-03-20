package com.gcs.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * WebSocket 查询用户在线状态请求
 */
@Data
@Schema(description = "查询用户在线状态请求")
public class QueryUserOnlineRequest {
    
    @Schema(description = "要查询的用户 ID 列表", required = true, example = "[4, 5, 6]")
    private List<Long> userIds;
    
    @Schema(description = "当前登录用户 ID（用于返回结果）", hidden = true)
    private Long currentUserId;
}
