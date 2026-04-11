package com.gcs.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

import org.springframework.format.annotation.DateTimeFormat;
import com.fasterxml.jackson.annotation.JsonFormat;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 文件记录实体类
 * @author 
 * @date 2026-04-05
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "文件记录实体")
@TableName("file_record")
public class FileRecord implements Serializable {
    private static final long serialVersionUID = 1L;

    @Schema(description = "主键 ID", example = "1")
    @TableId(type = IdType.AUTO)
    private Long id;

    @Schema(description = "文件名（存储名）", example = "20260405_abc123.jpg")
    private String fileName;

    @Schema(description = "原始文件名", example = "我的照片.jpg")
    private String originalFileName;

    @Schema(description = "文件存储路径", example = "/uploads/20260405_abc123.jpg")
    private String filePath;

    @Schema(description = "缩略图/封面路径", example = "/uploads/thumb_20260405_abc123.jpg")
    private String thumbnailPath;

    @Schema(description = "文件大小（字节）", example = "102400")
    private Long fileSize;

    @Schema(description = "文件 MD5 值", example = "d41d8cd98f00b204e9800998ecf8427e")
    private String fileMd5;

    @Schema(description = "MIME 类型", example = "image/jpeg")
    private String mimeType;

    @Schema(description = "文件类型分类（image/video/document）", example = "image")
    private String fileType;

    @Schema(description = "宽度（像素）", example = "1920")
    private Integer width;

    @Schema(description = "高度（像素）", example = "1080")
    private Integer height;

    @Schema(description = "视频时长（秒）", example = "120")
    private Integer duration;

    @Schema(description = "上传者 ID", example = "1")
    private Long uploaderId;

    @Schema(description = "上传时间", example = "2026-04-05 12:00:00")
    @JsonFormat(locale = "zh", timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime uploadTime;

    @Schema(description = "是否公开访问", example = "true")
    private Boolean isPublic;

    @Schema(description = "状态（1:正常 0:禁用）", example = "1")
    private Integer status;

    @Schema(description = "文件描述", example = "这是一张测试图片")
    private String description;

    @Schema(description = "下载次数", example = "10")
    private Integer downloadTimes;

    @Schema(description = "创建时间", example = "2026-04-05 12:00:00")
    @JsonFormat(locale = "zh", timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @Schema(description = "更新时间", example = "2026-04-05 12:00:00")
    @JsonFormat(locale = "zh", timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
