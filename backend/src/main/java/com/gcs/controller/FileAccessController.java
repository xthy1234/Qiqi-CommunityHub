package com.gcs.controller;

import com.gcs.annotation.IgnoreAuth;
import com.gcs.entity.FileRecord;
import com.gcs.service.FileRecordService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 文件访问控制器（兼容性）
 * 提供对旧版文件系统的兼容访问，以及新版 FileRecord 的统一访问入口
 * @author 
 * @date 2026-04-10
 */
@Slf4j
@Tag(name = "文件访问（兼容）", description = "支持新旧两种文件访问方式")
@RestController
@RequestMapping("/files")
public class FileAccessController {

    @Autowired
    private FileRecordService fileRecordService;

    @Value("${file.upload.path:#{systemProperties['user.dir']}/uploads}")
    private String uploadPath;

    /**
     * 统一文件访问接口
     * 支持两种方式：
     * 1. GET /files/{id} - 通过 FileRecord ID 访问（新版）
     * 2. GET /files/{fileName} - 通过文件名直接访问（旧版兼容）
     */
    @Operation(summary = "统一文件访问", description = "智能识别文件ID或文件名，支持新旧两种访问方式")
    @GetMapping(value = "/{identifier}", produces = MediaType.ALL_VALUE)
    @IgnoreAuth
    public ResponseEntity<Resource> accessFile(
            @Parameter(description = "文件ID或文件名", required = true) @PathVariable String identifier,
            HttpServletRequest request) {
        try {
            // 尝试 1: 如果是纯数字，尝试作为 FileRecord ID 访问
            if (identifier.matches("\\d+")) {
                try {
                    Long fileId = Long.parseLong(identifier);
                    FileRecord fileRecord = fileRecordService.getById(fileId);
                    
                    if (fileRecord != null) {
                        Path filePath = Paths.get(fileRecord.getFilePath());
                        
                        if (Files.exists(filePath)) {
                            Resource resource = new UrlResource(filePath.toUri());
                            
                            String contentType = fileRecord.getMimeType();
                            if (contentType == null || contentType.isEmpty()) {
                                contentType = Files.probeContentType(filePath);
                            }
                            if (contentType == null) {
                                contentType = "application/octet-stream";
                            }
                            
                            log.debug("通过 FileRecord ID 访问文件: ID={}, 文件名={}", fileId, fileRecord.getOriginalFileName());
                            
                            return ResponseEntity.ok()
                                    .contentType(MediaType.parseMediaType(contentType))
                                    .header(HttpHeaders.CACHE_CONTROL, "max-age=31536000")
                                    .body(resource);
                        } else {
                            log.warn("FileRecord 存在但物理文件不存在: ID={}, 路径={}", fileId, fileRecord.getFilePath());
                        }
                    }
                } catch (NumberFormatException e) {
                    log.debug("ID 转换失败，尝试文件系统访问");
                }
            }
            
            // 尝试 2: 作为文件名直接从文件系统访问（旧版兼容）
            Path filePath = Paths.get(uploadPath, identifier);
            
            if (!Files.exists(filePath)) {
                log.warn("文件不存在: {}", filePath.toString());
                return ResponseEntity.notFound().build();
            }
            
            Resource resource = new UrlResource(filePath.toUri());
            
            if (!resource.exists() || !resource.isReadable()) {
                return ResponseEntity.notFound().build();
            }
            
            String contentType = Files.probeContentType(filePath);
            if (contentType == null) {
                contentType = "application/octet-stream";
            }
            
            log.debug("通过文件名访问文件: {}", identifier);
            
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CACHE_CONTROL, "max-age=31536000")
                    .body(resource);
                    
        } catch (Exception e) {
            log.error("文件访问失败: identifier={}, error={}", identifier, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 缩略图访问接口（兼容）
     * GET /files/thumb/{id}
     */
    @Operation(summary = "缩略图访问", description = "通过文件ID访问缩略图")
    @GetMapping("/thumb/{id}")
    @IgnoreAuth
    public ResponseEntity<Resource> accessThumbnail(
            @Parameter(description = "文件ID", required = true) @PathVariable Long id) {
        try {
            FileRecord fileRecord = fileRecordService.getById(id);
            
            if (fileRecord == null) {
                return ResponseEntity.notFound().build();
            }
            
            if (fileRecord.getThumbnailPath() == null || fileRecord.getThumbnailPath().isEmpty()) {
                log.warn("文件没有缩略图: ID={}", id);
                return ResponseEntity.notFound().build();
            }
            
            Path thumbnailPath = Paths.get(fileRecord.getThumbnailPath());
            
            if (!Files.exists(thumbnailPath)) {
                log.warn("缩略图文件不存在: {}", thumbnailPath.toString());
                return ResponseEntity.notFound().build();
            }
            
            Resource resource = new UrlResource(thumbnailPath.toUri());
            
            if (!resource.exists() || !resource.isReadable()) {
                return ResponseEntity.notFound().build();
            }
            
            String contentType = Files.probeContentType(thumbnailPath);
            if (contentType == null) {
                contentType = "image/jpeg";
            }
            
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CACHE_CONTROL, "max-age=31536000")
                    .body(resource);
                    
        } catch (Exception e) {
            log.error("缩略图访问失败: ID={}, error={}", id, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
}
