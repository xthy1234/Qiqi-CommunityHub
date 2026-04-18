package com.gcs.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 接受邀请 DTO
 */
@Data
@Schema(description = "接受邀请请求")
public class CircleAcceptInviteDTO {
    
    @NotBlank(message = "邀请码不能为空")
    @Schema(description = "邀请码", example = "ABC12345")
    private String inviteCode;
}
