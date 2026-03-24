package com.gcs.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 通知 VO
 */
@Data
@Schema(description = "通知视图对象")
public class NotificationVO {

    @Schema(description = "通知 ID", example = "1")
    private Long id;

    @Schema(description = "用户 ID", example = "1")
    private Long userId;

    @Schema(description = "通知类型编码", example = "1")
    private Integer type;

    @Schema(description = "通知类型描述", example = "回复")
    private String typeName;

    @Schema(description = "关联的业务 ID", example = "100")
    private Long sourceId;

    /**
     * 通知简要内容
     */
    @Schema(description = "通知简要内容")
    private Map<String, Object> content;

    @Schema(description = "额外内容")
    private Map<String, Object> extra;


    @Schema(description = "是否已读", example = "false")
    private Boolean isRead;

    @Schema(description = "创建时间", example = "2026-01-01 12:00:00")
    private LocalDateTime createTime;
}
