package com.gcs.dto;

import com.gcs.enums.ContentType;
import com.gcs.enums.InteractionActionType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 互动记录创建请求对象
 */
@Data
@Schema(description = "互动记录创建请求对象")
public class InteractionCreateDTO {
    
    @NotNull(message = "内容 ID 不能为空")
    @Schema(description = "关联内容 ID（帖子 ID、商品 ID 等）", required = true, example = "1")
    private Long contentId;
    
    @NotNull(message = "表名不能为空")
    @Schema(description = "关联表名", required = true, example = "article")
    private ContentType tableName;
    
    @NotNull(message = "操作类型不能为空")
    @Schema(description = "操作类型 (1:收藏，2:点赞，3:点踩，4：分享)", required = true, example = "2")
    private InteractionActionType actionType;
    
    @Schema(description = "智能推荐类型", example = "hot")
    private String recommendType;
    
    @Schema(description = "备注信息", example = "用户手动点赞")
    private String remark;
}
