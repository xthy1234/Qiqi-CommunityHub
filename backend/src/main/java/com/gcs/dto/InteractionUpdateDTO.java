package com.gcs.dto;

import com.gcs.enums.CommonStatus;
import com.gcs.enums.InteractionStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 互动记录更新请求对象
 */
@Data
@Schema(description = "互动记录更新请求对象")
public class InteractionUpdateDTO {
    
    @Schema(description = "主键 ID", required = true, example = "1")
    private Long id;
    
    @Schema(description = "智能推荐类型", example = "hot")
    private String recommendType;
    
    @Schema(description = "备注信息", example = "用户手动点赞")
    private String remark;
    
    @Schema(description = "状态 (0:有效，1:无效)", example = "0")
    private InteractionStatus status;
}
