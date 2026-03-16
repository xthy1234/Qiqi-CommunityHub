package com.gcs.dto;

import com.gcs.enums.ContentType;
import com.gcs.enums.InteractionActionType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 点赞/点踩请求对象
 */
@Data
@Schema(description = "点赞/点踩请求对象")
public class InteractionLikeDTO {
    
    @NotNull(message = "内容 ID 不能为空")
    @Schema(description = "内容 ID", required = true, example = "1")
    private Long contentId;
    
    @NotNull(message = "操作类型不能为空")
    @Schema(description = "操作类型 (2:点赞，3:点踩)", required = true, example = "2")
    private InteractionActionType actionType;
    
    @Schema(description = "内容类型 (article:文章，comment:评论，可选，默认 article)", example = "article")
    private ContentType tableName;
}
