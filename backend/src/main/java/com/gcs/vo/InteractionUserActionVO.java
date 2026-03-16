package com.gcs.vo;

import com.gcs.enums.InteractionActionType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;

/**
 * 用户互动行为视图对象 - 用于检查用户是否已执行某项操作
 */
@Data
@Schema(description = "用户互动行为视图对象")
public class InteractionUserActionVO implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    @Schema(description = "内容 ID", example = "1")
    private Long contentId;
    
    @Schema(description = "操作类型", example = "21")
    private InteractionActionType actionType;
    
    @Schema(description = "是否已操作", example = "true")
    private Boolean hasAction;
    
    @Schema(description = "操作 ID（如果已操作）", example = "1")
    private Long interactionId;
}
