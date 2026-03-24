package com.gcs.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Map;

/**
 * 通知创建 DTO
 */
@Data
@Schema(description = "通知创建数据")
public class NotificationCreateDTO {

    @NotNull(message = "用户 ID 不能为空")
    @Schema(description = "用户 ID", example = "1", required = true)
    private Long userId;

    @NotNull(message = "通知类型不能为空")
    @Schema(description = "通知类型（1:回复，2:点赞，3:关注，4:私聊消息，5:圈子更新，6:圈子聊天@，7:提交建议，8:建议审核结果）", example = "1", required = true)
    private Integer type;

    @Schema(description = "关联的业务 ID", example = "100")
    private Long sourceId;

    @Schema(description = "通知简要内容（JSON）")
    private Map<String, Object> content;

    @Schema(description = "额外内容（JSON）")
    private Map<String, Object> extra;
}
