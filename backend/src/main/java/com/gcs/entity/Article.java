package com.gcs.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.gcs.enums.AuditStatus;
import com.gcs.handler.AuditStatusTypeHandler;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Date;

import org.springframework.format.annotation.DateTimeFormat;
import com.fasterxml.jackson.annotation.JsonFormat;

import io.swagger.v3.oas.annotations.media.Schema;


/**
 * 帖子实体类
 * 数据库通用操作实体类（普通增删改查）
 * @author 
 * @email 
 * @date 2026-04-16 11:22:09
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "帖子实体")
@TableName("article")
public class Article implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 主键 ID
     */
    @Schema(description = "主键 ID", example = "1")
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 标题
     */
    @Schema(description = "标题", example = "我的第一篇帖子")
    private String title;

    /**
     * 封面图片 URL
     */
    @Schema(description = "封面图片 URL", example = "http://example.com/cover.jpg")
    private String coverUrl;

    /**
     * 内容分类 ID
     */
    @Schema(description = "内容分类 ID", example = "1")
   private Long categoryId;

    /**
     * 作者 ID
     */
    @Schema(description = "作者 ID", example = "1")
   private Long authorId;

    /**
     * 收藏数量
     */
    @Schema(description = "收藏数量", example = "10")
    private Integer favoriteCount;

    /**
     * 附件 URL
     */
    @Schema(description = "附件 URL", example = "http://example.com/attachment.zip")
    private String attachment;

    /**
     * 内容详情
     */
    @Schema(description = "内容详情", example = "这是帖子的具体内容...")
    private String content;

    /**
     * 发布时间
     */
    @Schema(description = "发布时间", example = "2026-01-01")
    @JsonFormat(locale = "zh", timezone = "GMT+8", pattern = "yyyy-MM-dd")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private Date publishTime;

    /**
     * 点赞数
     */
    @Schema(description = "点赞数", example = "100")
    private Integer likeCount;

    /**
     * 点踩数
     */
    @Schema(description = "点踩数", example = "5")
    private Integer dislikeCount;

    /**
     * 分享次数
     */
    @Schema(description = "分享次数", example = "15")
    private Integer shareCount = 0;

    /**
     * 审核状态 (0:待审核，1:已通过，2:已拒绝，3:草稿)
     */
    @Schema(description = "审核状态 (0:待审核，1:已通过，2:已拒绝，3:草稿)", example = "1")
    @TableField(typeHandler = AuditStatusTypeHandler.class)
    private AuditStatus auditStatus;

    /**
     * 审核回复内容
     */
    @Schema(description = "审核回复内容", example = "审核通过")
    private String auditReply;

    /**
     * 点击次数
     */
    @Schema(description = "点击次数", example = "500")
    private Integer viewCount;

    /**
     * 评论数量
     */
    @Schema(description = "评论数量", example = "20")
    private Integer commentCount = 0;
    
    /**
     * 逻辑删除标志 (false:未删除，true:已删除)
     */
    @Schema(description = "逻辑删除标志", example = "false")
    @TableLogic
    private Boolean deleted = false;
    
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

    // 构造函数用于泛型转换（保留向后兼容）
    public <T> Article(T source) {
        // 可以在这里实现属性复制逻辑
        // 建议使用更现代的映射工具如MapStruct替代BeanUtils
    }
}
