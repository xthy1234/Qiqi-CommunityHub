package com.gcs.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 文件记录 VO
 * @author 
 * @date 2026-04-05
 */
@Data
@Schema(description = "文件记录响应")
public class FileRecordVO {

    @Schema(description = "文件 ID", example = "1")
    private Long id;

    @Schema(description = "文件名", example = "20260405_abc123.jpg")
    private String fileName;

    @Schema(description = "原始文件名", example = "我的照片.jpg")
    private String originalFileName;

    @Schema(description = "文件访问 URL", example = "/api/files/1/view")
    private String fileUrl;

    @Schema(description = "文件预览 URL（可直接用于 img 标签）", example = "/api/files/1/view")
    private String viewUrl;

    @Schema(description = "文件下载 URL", example = "/api/files/1/download")
    private String downloadUrl;

    @Schema(description = "缩略图 URL", example = "/api/files/thumb/1")
    private String thumbnailUrl;

    @Schema(description = "文件大小（字节）", example = "102400")
    private Long fileSize;

    @Schema(description = "可读文件大小", example = "100 KB")
    private String readableSize;

    @Schema(description = "MIME 类型", example = "image/jpeg")
    private String mimeType;

    @Schema(description = "文件类型", example = "image")
    private String fileType;

    @Schema(description = "宽度", example = "1920")
    private Integer width;

    @Schema(description = "高度", example = "1080")
    private Integer height;

    @Schema(description = "视频时长（秒）", example = "120")
    private Integer duration;

    @Schema(description = "上传者 ID", example = "1")
    private Long uploaderId;

    @Schema(description = "上传者昵称", example = "张三")
    private String uploaderNickname;

    @Schema(description = "上传时间", example = "2026-04-05 12:00:00")
    @JsonFormat(locale = "zh", timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime uploadTime;

    @Schema(description = "是否公开", example = "true")
    private Boolean isPublic;

    @Schema(description = "状态", example = "1")
    private Integer status;

    @Schema(description = "描述", example = "这是一张测试图片")
    private String description;

    @Schema(description = "下载次数", example = "10")
    private Integer downloadTimes;
}
