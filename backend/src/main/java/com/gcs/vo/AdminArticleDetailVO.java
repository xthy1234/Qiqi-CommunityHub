
package com.gcs.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.gcs.enums.AuditStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.Map;

/**
 * 管理员查看文章详情 VO（包含审核信息等敏感字段）
 */
@Data
@Schema(description = "管理员查看文章详情 VO")
public class AdminArticleDetailVO {
    
    @Schema(description = "文章 ID", example = "1")
    private Long id;
    
    @Schema(description = "文章标题", example = "Java 并发编程指南")
    private String title;
    
    @Schema(description = "文章内容（TipTap JSON 格式）", example = "{\"type\":\"doc\",\"content\":[{\"type\":\"paragraph\"}]}")
    private Map<String, Object> content;
    
    @Schema(description = "封面 URL", example = "http://example.com/cover.jpg")
    private String coverUrl;
    
    @Schema(description = "分类 ID", example = "1")
    private Long categoryId;
    
    @Schema(description = "分类名称", example = "技术教程")
    private String categoryName;
    
    @Schema(description = "作者 ID", example = "5")
    private Long authorId;
    
    @Schema(description = "作者昵称", example = "张三")
    private String authorNickname;
    
    @Schema(description = "作者头像", example = "http://example.com/avatar.jpg")
    private String authorAvatar;
    
    @Schema(description = "浏览量", example = "1024")
    private Integer viewCount;
    
    @Schema(description = "点赞数", example = "56")
    private Integer likeCount;
    
    @Schema(description = "点踩数", example = "2")
    private Integer dislikeCount;
    
    @Schema(description = "收藏数", example = "23")
    private Integer favoriteCount;
    
    @Schema(description = "评论数", example = "15")
    private Integer commentCount;
    
    @Schema(description = "审核状态 (0:待审核，1:已通过，2:已拒绝)", example = "1")
    private Integer auditStatus;
    
    @Schema(description = "审核回复", example = "内容优质，通过审核")
    private String auditReply;
    
    @Schema(description = "编辑模式 (0:仅作者可编辑，1:所有人可建议)", example = "0")
    private Integer editMode;
    
    @Schema(description = "是否置顶", example = "false")
    private Boolean isTop;
    
    @Schema(description = "是否推荐", example = "true")
    private Boolean isFeatured;
    
    @Schema(description = "发布时间", example = "2026-01-01 12:00:00")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date publishTime;
    
    @Schema(description = "创建时间", example = "2026-01-01 10:00:00")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;
    
    @Schema(description = "更新时间", example = "2026-01-01 12:00:00")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime updateTime;
    
    @Schema(description = "大版本号", example = "1")
    private Integer majorVersion;
    
    @Schema(description = "小版本号", example = "2")
    private Integer minorVersion;
    
    @Schema(description = "贡献者数量", example = "3")
    private Integer contributorCount;
    
    @Schema(description = "版本数量", example = "5")
    private Integer versionCount;
}
