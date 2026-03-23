package com.gcs.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Map;

/**
 * 发送私信请求 DTO
 */
@Data
@Schema(description = "发送私信请求")
public class MessageSendDTO {
    
    @NotNull(message = "接收方用户 ID 不能为空")
    @Schema(description = "接收方用户 ID", example = "456", required = true)
    private Long toUserId;
    
    @NotBlank(message = "消息内容不能为空")
    @Schema(description = "消息内容", example = "你好！", required = true)
    private Map<String, Object> content;

    @Schema(description = "扩展信息 (JSON格式)")
    private Map<String, Object> extra;

    @NotNull(message = "消息类型不能为空")
    @Schema(description = "消息类型 (0-文本，1-图片，2-文件)", example = "0", required = true)
    private Integer msgType;
}
