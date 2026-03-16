package com.gcs.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.gcs.enums.ContentType;
import com.gcs.enums.InteractionStatus;
import com.gcs.handler.ContentTypeHandler;
import com.gcs.handler.InteractionActionTypeHandler;
import com.gcs.handler.InteractionStatusTypeHandler;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import com.gcs.enums.InteractionActionType;
import java.io.Serializable;
import java.time.LocalDateTime;

import org.springframework.format.annotation.DateTimeFormat;
import com.fasterxml.jackson.annotation.JsonFormat;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 互动记录实体类
 * 数据库通用操作实体类（普通增删改查）
 * @author 
 * @email 
 * @date
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "互动记录实体（点赞、收藏、关注等）")
@TableName("interaction")
public class Interaction implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 主键 ID
     */
    @Schema(description = "主键 ID", example = "1")
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 关联内容 ID
     */
    @Schema(description = "关联内容 ID（帖子 ID、商品 ID 等）", example = "1")
    private Long contentId;

    /**
     * 关联表名（article:文章，comment:评论）
     */
    @Schema(description = "关联表名 (article:文章，comment:评论)", example = "article")
    @TableField(typeHandler = ContentTypeHandler.class)
    private ContentType tableName;

    /**
     * 操作类型 (1:收藏，2:点赞，3:点踩，4：分享)
     */
    @Schema(description = "操作类型 (1:收藏，2:点赞，3:点踩，4：分享)", example = "1")
    @TableField(typeHandler = InteractionActionTypeHandler.class)
    private InteractionActionType actionType;

    /**
     * 智能推荐类型
     */
    @Schema(description = "智能推荐类型", example = "hot")
    private String recommendType;

    /**
     * 备注信息
     */
    @Schema(description = "备注信息", example = "用户手动点赞")
    private String remark;

    /**
     * 用户 ID
     */
    @Schema(description = "用户 ID", example = "1")
    private Long userId;

    /**
     * 状态 (0:有效，1:无效)
     */
    @Schema(description = "状态 (0:有效，1:无效)", example = "0")
    @TableField(typeHandler = InteractionStatusTypeHandler.class)
    private InteractionStatus status = InteractionStatus.VALID;
    
    /**
     * 逻辑删除标志 (false:未删除，true:已删除)
     */
    @Schema(description = "逻辑删除标志", example = "false")
    @TableLogic
    private Boolean isDeleted = false;
    
    /**
     * 创建时间
     */
    @Schema(description = "创建时间", example = "2026-01-01 12:00:00")
    @JsonFormat(locale = "zh", timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @Schema(description = "更新时间", example = "2026-01-01 12:00:00")
    @JsonFormat(locale = "zh", timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

}
