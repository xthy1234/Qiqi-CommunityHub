package com.gcs.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 加入圈子请求 DTO
 */
@Data
@Schema(description = "加入圈子请求对象")
public class CircleJoinDTO {
    
    @Schema(description = "邀请码（可选，私密圈子需要）", example = "INVITE123")
    private String inviteCode;
}
