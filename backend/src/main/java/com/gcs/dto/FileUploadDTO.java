package com.gcs.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文件上传 DTO
 * @author 
 * @date 2026-04-05
 */
@Data
@Schema(description = "文件上传请求")
public class FileUploadDTO {

    @Schema(description = "上传的文件", required = true)
    private MultipartFile file;

    @Schema(description = "文件类型分类（image/video/document）", example = "image")
    private String fileType;

    @Schema(description = "文件描述", example = "这是我的头像")
    private String description;

    @Schema(description = "是否公开", example = "true")
    private Boolean isPublic = true;
}
