package com.gcs.vo;


import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Date;

/**
 * 文章搜索视图对象 - 用于全文搜索结果展示（包含相关度排名）
 */
@Data
@Schema(description = "文章搜索视图对象")
public class ArticleSearchVO implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    @Schema(description = "主键 ID", example = "1")
    private Long id;
    
    @Schema(description = "标题", example = "我的第一篇帖子")
    private String title;
    
    @Schema(description = "封面图片 URL", example = "http://example.com/cover.jpg")
    private String coverUrl;
    
    @Schema(description = "分类 ID", example = "1")
    private Long categoryId;
    
    @Schema(description = "分类名称", example = "技术交流")
    private String categoryName;
    
    @Schema(description = "作者 ID", example = "1")
    private Long authorId;
    
    @Schema(description = "作者昵称", example = "张三")
    private String authorNickname;
    
    @Schema(description = "作者头像 URL", example = "http://example.com/avatar.jpg")
    private String authorAvatar;
    
    @Schema(description = "内容摘要", example = "这是帖子的内容摘要...")
    private String contentSummary;
    
    @Schema(description = "点赞数", example = "100")
    private Integer likeCount;
    
    @Schema(description = "点踩数", example = "5")
    private Integer dislikeCount;

    @Schema(description = "分享次数", example = "15")
    private Integer shareCount;

    @Schema(description = "点击次数", example = "500")
    private Integer viewCount;
    
    @Schema(description = "发布时间", example = "2026-01-01")
    @JsonFormat(locale = "zh", timezone = "GMT+8", pattern = "yyyy-MM-dd")
    private Date publishTime;
    
    @Schema(description = "审核状态 (0:待审核，1:已通过，2:已拒绝，3:草稿)", example = "1")
    private Integer auditStatus;
    
    @Schema(description = "相关度得分", example = "0.95")
    private BigDecimal rank;
    
    @Schema(description = "创建时间", example = "2026-01-01 12:00:00")
    @JsonFormat(locale = "zh", timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
