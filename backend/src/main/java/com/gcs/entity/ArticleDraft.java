package com.gcs.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Map;

import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;

import io.swagger.v3.oas.annotations.media.Schema;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "文章草稿实体")
@TableName(value = "article_draft", autoResultMap = true)
public class ArticleDraft implements Serializable {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    @Schema(description = "关联的文章 ID（可为空，表示新文章草稿）", example = "100")
    private Long articleId;
    
    @Schema(description = "草稿作者 ID", example = "1")
    private Long userId;
    
    @TableField(typeHandler = JacksonTypeHandler.class)
    @Schema(description = "草稿内容（JSONB）")
    private Map<String, Object> content;

    @TableField(typeHandler = JacksonTypeHandler.class)
    @Schema(description = "扩展信息")
    private Map<String, Object> extra;

    @Schema(description = "草稿标题", example = "我的文章")
    private String title;
    
    @Schema(description = "修改摘要", example = "更新了内容")
    private String changeSummary;
    
    @Schema(description = "最后自动保存时间")
    private LocalDateTime autoSaveTime;
    
    @Schema(description = "创建时间")
    private LocalDateTime createTime;
    
    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
}
