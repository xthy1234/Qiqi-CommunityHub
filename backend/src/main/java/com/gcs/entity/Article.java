package com.gcs.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import com.gcs.enums.AuditStatus;
import com.gcs.handler.AuditStatusTypeHandler;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.Map;

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
@TableName(value = "article", autoResultMap = true)
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
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> content;
    
    /**
     * 文章当前的版本号
     */
    @Schema(description = "版本号", example = "1")
    private Integer currentVersion;
    
    /**
     * 扩展信息 (JSON 格式)
     */
    @Schema(description = "扩展信息 (JSON 格式)")
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Map<String, Object> extra;

    /**
     * 发布时间（仅在审核通过时填充）
     */
    @Schema(description = "发布时间（审核通过后自动生成）", example = "2026-01-01")
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
     * 审核状态 (0:待审核，1:已通过，2:已拒绝，3:草稿，4:违规屏蔽)
     */
    @Schema(description = "审核状态 (0:待审核，1:已通过，2:已拒绝，3:草稿，4:违规屏蔽)", example = "1")
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
     * 编辑模式（0-仅作者可编辑，1-所有人可建议）
     */
    @Schema(description = "编辑模式（0-仅作者可编辑，1-所有人可建议）", example = "0")
    private Integer editMode;
    
    /**
     * 是否推荐/置顶
     */
    @Schema(description = "是否推荐/置顶", example = "false")
    private Boolean isFeatured;
    
    /**
     * 推荐等级（0-普通，1-推荐，2-热门）
     */
    @Schema(description = "推荐等级（0-普通，1-推荐，2-热门）", example = "0")
    private Integer featuredLevel;
    
    /**
     * 逻辑删除标志 (false:未删除，true:已删除)
     */
    @Schema(description = "逻辑删除标志", example = "false")
    @TableLogic
    private Boolean deleted = false;

    @Schema(description = "是否存在草稿", example = "false")
    private Boolean draftExists;

 /**
     * 创建时间（系统自动生成）
     */
    @Schema(description = "创建时间（系统自动生成）", example = "2026-01-01 12:00:00")
    @JsonFormat(locale = "zh", timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 更新时间（系统自动生成）
     */
    @Schema(description = "更新时间（系统自动生成）", example = "2026-01-01 12:00:00")
    @JsonFormat(locale = "zh", timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /**
     * 增加点赞数
     */
    public void addLike() {
        this.likeCount = (this.likeCount == null ? 0 : this.likeCount) + 1;
    }

    /**
     * 减少点赞数
     */
    public void removeLike() {
        this.likeCount = Math.max(0, (this.likeCount == null ? 0 : this.likeCount) - 1);
    }

    /**
     * 增加点踩数
     */
    public void addDislike() {
        this.dislikeCount = (this.dislikeCount == null ? 0 : this.dislikeCount) + 1;
    }

    /**
     * 减少点踩数
     */
    public void removeDislike() {
        this.dislikeCount = Math.max(0, (this.dislikeCount == null ? 0 : this.dislikeCount) - 1);
    }

    /**
     * 增加收藏数
     */
    public void addFavorite() {
        this.favoriteCount = (this.favoriteCount == null ? 0 : this.favoriteCount) + 1;
    }

    /**
     * 减少收藏数
     */
    public void removeFavorite() {
        this.favoriteCount = Math.max(0, (this.favoriteCount == null ? 0 : this.favoriteCount) - 1);
    }

    /**
     * 增加分享数
     */
    public void addShare() {
        this.shareCount = (this.shareCount == null ? 0 : this.shareCount) + 1;
    }

    /**
     * 减少分享数
     */
    public void removeShare() {
        this.shareCount = Math.max(0, (this.shareCount == null ? 0 : this.shareCount) - 1);
    }
}
