package com.gcs.dto;

import com.gcs.enums.ContentType;
import com.gcs.enums.InteractionActionType;
import com.gcs.enums.InteractionStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 互动记录通用数据传输对象
 */
@Data
@Schema(description = "互动记录数据传输对象")
public class InteractionDTO {
    
    @Schema(description = "主键 ID", example = "1")
    private Long id;
    
    @NotNull(message = "内容 ID 不能为空")
    @Schema(description = "关联内容 ID", required = true, example = "1")
    private Long contentId;
    
    @NotBlank(message = "表名不能为空")
    @Schema(description = "关联表名", required = true, example = "articles")
    private ContentType tableName;
    
    @NotBlank(message = "操作类型不能为空")
    @Schema(description = "操作类型 (1:收藏，21:点赞，22:点踩，31:竞拍参与，41:关注)", required = true, example = "21")
    private InteractionActionType actionType;
    
    @Schema(description = "智能推荐类型", example = "hot")
    private String recommendType;
    
    @Schema(description = "备注信息", example = "用户手动点赞")
    private String remark;
    
    @NotNull(message = "用户 ID 不能为空")
    @Schema(description = "用户 ID", required = true, example = "1")
    private Long userId;
    
    @Schema(description = "状态 (0:有效，1:无效)", example = "0")
    private InteractionStatus status;
}
