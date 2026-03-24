package com.gcs.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

/**
 * 标记已读请求 DTO
 */
@Data
@Schema(description = "标记已读请求数据")
public class MarkReadRequest {

    @NotEmpty(message = "通知 ID 列表不能为空")
    @Schema(description = "通知 ID 列表", required = true)
    private List<Long> notificationIds;
}
