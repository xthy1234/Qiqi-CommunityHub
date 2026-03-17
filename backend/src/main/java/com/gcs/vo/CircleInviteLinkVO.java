package com.gcs.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 邀请链接响应 VO
 */
@Data
@Schema(description = "邀请链接响应对象")
public class CircleInviteLinkVO {
    
    @Schema(description = "邀请链接", example = "http://example.com/circles/1/invite?code=ABC123")
    private String inviteLink;
    
    @Schema(description = "邀请码", example = "ABC123")
    private String inviteCode;
    
    @Schema(description = "过期时间", example = "2026-03-24 12:00:00")
    private String expireTime;
}
