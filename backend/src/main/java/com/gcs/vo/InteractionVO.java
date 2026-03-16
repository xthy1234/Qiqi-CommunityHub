package com.gcs.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.gcs.enums.ContentType;
import com.gcs.enums.InteractionActionType;
import com.gcs.enums.InteractionStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 互动记录基础视图对象 - 用于列表展示
 */
@Data
@Schema(description = "互动记录基础视图对象")
public class InteractionVO implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    @Schema(description = "主键 ID", example = "1")
    private Long id;
    
    @Schema(description = "关联内容 ID", example = "1")
    private Long contentId;
    
    @Schema(description = "关联表名 (article:文章，comment:评论)", example = "article")
    private ContentType tableName;
    
    @Schema(description = "操作类型 (1:收藏，2:点赞，3:点踩，4：分享)", example = "2")
    private InteractionActionType actionType;
    
    @Schema(description = "用户 ID", example = "1")
    private Long userId;
    
    @Schema(description = "用户昵称", example = "张三")
    private String userNickname;
    
    @Schema(description = "用户头像", example = "http://example.com/avatar.jpg")
    private String userAvatar;
    
    @Schema(description = "状态 (0:有效，1:无效)", example = "0")
    private InteractionStatus status;
    
    @Schema(description = "创建时间", example = "2026-01-01 12:00:00")
    @JsonFormat(locale = "zh", timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
