package com.gcs.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 管理员发送通知请求对象
 */
@Data
@Schema(description = "管理员发送通知请求")
public class AdminSendNotificationDTO {

    @Schema(description = "接收用户 ID 列表（为空表示全员发送）", example = "[1, 2, 3]")
    private List<Long> userIds;

    @NotNull(message = "通知类型不能为空")
    @Schema(description = "通知类型（9:系统公告，10:活动通知，11:警告通知）", example = "9", required = true)
    private Integer type;

    @NotBlank(message = "通知标题不能为空")
    @Schema(description = "通知标题", example = "系统维护公告", required = true)
    private String title;

    @NotBlank(message = "通知内容不能为空")
    @Schema(description = "通知内容", example = "系统将于今晚 22:00 进行维护", required = true)
    private String content;

    @Schema(description = "跳转链接（可选）", example = "/article/123")
    private String linkUrl;

    @Schema(description = "额外数据（JSON 格式，可选）")
    private Map<String, Object> extra;

    @Schema(description = "是否置顶", example = "false")
    private Boolean isTop = false;

    @Schema(description = "优先级（1:普通，2:重要，3:紧急）", example = "1")
    private Integer priority = 1;
}
