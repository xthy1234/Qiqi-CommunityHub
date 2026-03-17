package com.gcs.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.NotNull;

/**
 * 更新成员角色请求 DTO
 */
@Data
@Schema(description = "更新成员角色请求对象")
public class CircleMemberRoleUpdateDTO {
    
    @NotNull(message = "角色不能为空")
    @Schema(description = "角色 (0:成员，1:管理员)", example = "1", required = true)
    private Integer role;
}
