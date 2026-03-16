package com.gcs.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 互动记录详情视图对象 - 包含完整信息
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Schema(description = "互动记录详情视图对象")
public class InteractionDetailVO extends InteractionVO {
    
    @Schema(description = "智能推荐类型", example = "hot")
    private String recommendType;
    
    @Schema(description = "备注信息", example = "用户手动点赞")
    private String remark;
    
    @Schema(description = "更新时间", example = "2026-01-01 12:00:00")
    @JsonFormat(locale = "zh", timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
    
    @Schema(description = "内容标题", example = "我的第一篇帖子")
    private String contentTitle;
    
    @Schema(description = "内容封面 URL", example = "http://example.com/cover.jpg")
    private String contentCoverUrl;
}
