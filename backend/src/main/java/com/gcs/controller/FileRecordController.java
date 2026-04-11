package com.gcs.controller;

import com.gcs.annotation.IgnoreAuth;
import com.gcs.converter.FileRecordConverter;
import com.gcs.entity.FileRecord;
import com.gcs.service.FileRecordService;
import com.gcs.utils.R;
import com.gcs.utils.SessionUtils;
import com.gcs.vo.FileRecordVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

/**
 * 文件记录控制器
 * 提供图片、视频、文件的上传、存储与访问功能
 * @author 
 * @date 2026-04-10
 */
@Slf4j
@Tag(name = "文件记录管理", description = "支持图片、视频、文件的上传、存储与访问")
@RestController
@RequestMapping("/api/files")
public class FileRecordController {

    @Autowired
    private FileRecordService fileRecordService;

    @Autowired
    private FileRecordConverter fileRecordConverter;

    @Autowired
    private SessionUtils sessionUtils;

    @Value("${file.upload.path:#{systemProperties['user.dir']}/uploads}")
    private String uploadPath;

    /**
     * 上传文件
     * POST /api/files/upload
     */
    @Operation(summary = "上传文件", description = "支持图片、视频、文档的上传，自动校验类型和大小，计算MD5去重")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "上传成功"),
        @ApiResponse(responseCode = "400", description = "文件格式不支持或超过大小限制"),
        @ApiResponse(responseCode = "500", description = "上传失败")
    })
    @PostMapping("/upload")
    public R uploadFile(
            HttpServletRequest request,
            @Parameter(description = "上传的文件", required = true) @RequestParam("file") MultipartFile file,
            @Parameter(description = "文件类型分类（image/video/document）", example = "image") @RequestParam(required = false) String fileType,
            @Parameter(description = "文件描述", example = "这是我的头像") @RequestParam(required = false) String description,
            @Parameter(description = "是否公开", example = "true") @RequestParam(required = false, defaultValue = "true") Boolean isPublic) {
        
        try {
            Long uploaderId = sessionUtils.getCurrentUserId(request);
            if (uploaderId == null) {
                return R.error(401, "请先登录");
            }
            
            FileRecord fileRecord = fileRecordService.uploadFile(file, uploaderId, fileType, description, isPublic);
            
            FileRecordVO fileRecordVO = fileRecordConverter.toVO(fileRecord);
            
            log.info("文件上传成功: ID={}, 文件名={}, 大小={} bytes", 
                    fileRecord.getId(), fileRecord.getOriginalFileName(), fileRecord.getFileSize());
            
            return R.ok("文件上传成功")
                    .put("file", fileRecordVO);

        } catch (Exception e) {
            log.error("文件上传失败: {}", e.getMessage(), e);
            return R.error(500, "上传失败: " + e.getMessage());
        }
    }

    /**
     * 获取文件信息
     * GET /api/files/{id}
     */
    @Operation(summary = "获取文件信息", description = "根据文件ID获取文件的详细信息")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "获取成功"),
        @ApiResponse(responseCode = "404", description = "文件不存在")
    })
    @GetMapping("/{id}")
    @IgnoreAuth
    public R getFileInfo(@Parameter(description = "文件ID", required = true) @PathVariable Long id) {
        FileRecordVO fileRecordVO = fileRecordService.getFileById(id);
        
        if (fileRecordVO == null) {
            return R.error("文件不存在");
        }
        
        return R.ok().put("file", fileRecordVO);
    }

    /**
     * 下载/访问文件
     * GET /api/files/{id}/download
     */
    @Operation(summary = "下载文件", description = "根据文件ID下载文件，自动增加下载次数")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "下载成功"),
        @ApiResponse(responseCode = "404", description = "文件不存在"),
        @ApiResponse(responseCode = "500", description = "下载失败")
    })
    @GetMapping("/{id}/download")
    @IgnoreAuth
    public ResponseEntity<Resource> downloadFile(
            @Parameter(description = "文件ID", required = true) @PathVariable Long id) {
        try {
            FileRecord fileRecord = fileRecordService.downloadFile(id);
            
            if (fileRecord == null) {
                return ResponseEntity.notFound().build();
            }
            
            Path filePath = Paths.get(fileRecord.getFilePath());
            
            if (!Files.exists(filePath)) {
                log.warn("文件不存在: {}", filePath.toString());
                return ResponseEntity.notFound().build();
            }
            
            Resource resource = new UrlResource(filePath.toUri());
            
            if (!resource.exists() || !resource.isReadable()) {
                return ResponseEntity.notFound().build();
            }
            
            String contentType = fileRecord.getMimeType();
            if (contentType == null || contentType.isEmpty()) {
                contentType = Files.probeContentType(filePath);
            }
            if (contentType == null) {
                contentType = "application/octet-stream";
            }
            
            String originalFileName = fileRecord.getOriginalFileName();
            String contentDisposition = "attachment; filename=\"" + originalFileName + "\"";
            
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition)
                    .body(resource);
                    
        } catch (Exception e) {
            log.error("文件下载失败: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 直接访问文件（用于图片、视频等在线预览）
     * GET /api/files/{id}/view
     */
    @Operation(summary = "在线预览文件", description = "直接在浏览器中查看文件，适用于图片、视频等")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "访问成功"),
        @ApiResponse(responseCode = "404", description = "文件不存在")
    })
    @GetMapping("/{id}/view")
    @IgnoreAuth
    public ResponseEntity<Resource> viewFile(
            @Parameter(description = "文件ID", required = true) @PathVariable Long id) {
        try {
            FileRecord fileRecord = fileRecordService.getById(id);
            
            if (fileRecord == null) {
                return ResponseEntity.notFound().build();
            }
            
            Path filePath = Paths.get(fileRecord.getFilePath());
            
            if (!Files.exists(filePath)) {
                log.warn("文件不存在: {}", filePath.toString());
                return ResponseEntity.notFound().build();
            }
            
            Resource resource = new UrlResource(filePath.toUri());
            
            if (!resource.exists() || !resource.isReadable()) {
                return ResponseEntity.notFound().build();
            }
            
            String contentType = fileRecord.getMimeType();
            if (contentType == null || contentType.isEmpty()) {
                contentType = Files.probeContentType(filePath);
            }
            if (contentType == null) {
                contentType = "application/octet-stream";
            }
            
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CACHE_CONTROL, "max-age=31536000")
                    .body(resource);
                    
        } catch (Exception e) {
            log.error("文件访问失败: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 获取缩略图
     * GET /api/files/{id}/thumbnail
     */
    @Operation(summary = "获取缩略图", description = "获取图片或视频的缩略图/封面")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "获取成功"),
        @ApiResponse(responseCode = "404", description = "缩略图不存在")
    })
    @GetMapping("/{id}/thumbnail")
    @IgnoreAuth
    public ResponseEntity<Resource> getThumbnail(
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
            log.error("缩略图获取失败: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 删除文件
     * DELETE /api/files/{id}
     */
    @Operation(summary = "删除文件", description = "删除文件记录及物理文件，仅作者或管理员可操作")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "删除成功"),
        @ApiResponse(responseCode = "403", description = "无权限删除"),
        @ApiResponse(responseCode = "404", description = "文件不存在")
    })
    @DeleteMapping("/{id}")
    public R deleteFile(HttpServletRequest request, @Parameter(description = "文件ID", required = true) @PathVariable Long id) {
        try {
            Long operatorId = sessionUtils.getCurrentUserId(request);
            if (operatorId == null) {
                return R.error(401, "请先登录");
            }
            
            boolean success = fileRecordService.deleteFile(id, operatorId);
            
            if (!success) {
                return R.error("删除失败，可能是文件不存在或无权限操作");
            }
            
            log.info("文件删除成功: ID={}, 操作者={}", id, operatorId);
            return R.ok("文件删除成功");
            
        } catch (Exception e) {
            log.error("文件删除失败: {}", e.getMessage(), e);
            return R.error("删除失败: " + e.getMessage());
        }
    }

    /**
     * 分页查询文件列表
     * GET /api/files/list
     */
    @Operation(summary = "分页查询文件列表", description = "支持按类型、上传者、状态等多条件查询")
    @GetMapping("/list")
    public R getFileList(@RequestParam Map<String, Object> params) {
        try {
            var pageUtils = fileRecordService.queryPage(params);
            return R.ok().put("page", pageUtils);
        } catch (Exception e) {
            log.error("查询文件列表失败: {}", e.getMessage(), e);
            return R.error("查询失败: " + e.getMessage());
        }
    }

    /**
     * 获取用户的文件列表
     * GET /api/files/user/{userId}
     */
    @Operation(summary = "获取用户文件列表", description = "获取指定用户上传的文件列表")
    @GetMapping("/user/{userId}")
    public R getUserFiles(
            @Parameter(description = "用户ID", required = true) @PathVariable Long userId,
            @Parameter(description = "文件类型（可选）", example = "image") @RequestParam(required = false) String fileType) {
        try {
            List<FileRecordVO> files = fileRecordService.getUserFiles(userId, fileType);
            return R.ok().put("files", files).put("total", files.size());
        } catch (Exception e) {
            log.error("获取用户文件列表失败: {}", e.getMessage(), e);
            return R.error("获取失败: " + e.getMessage());
        }
    }

    // ==================== 兼容性接口（支持原有的文件访问方式）====================

    /**
     * 兼容原有文件访问方式：GET /files/{fileName}
     * 支持通过文件名直接访问旧文件系统中的文件
     */
    @Operation(summary = "兼容旧版文件访问", description = "通过文件名访问旧文件系统中的文件（向后兼容）")
    @GetMapping(value = "/{fileName}", produces = MediaType.ALL_VALUE)
    @IgnoreAuth
    @RequestMapping("/files/{fileName}")
    public ResponseEntity<Resource> accessLegacyFile(
            @Parameter(description = "文件名", required = true) @PathVariable String fileName) {
        try {
            // 尝试从 FileRecord 数据库获取（如果是数字 ID）
            if (fileName.matches("\\d+")) {
                try {
                    Long fileId = Long.parseLong(fileName);
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
                            
                            return ResponseEntity.ok()
                                    .contentType(MediaType.parseMediaType(contentType))
                                    .header(HttpHeaders.CACHE_CONTROL, "max-age=31536000")
                                    .body(resource);
                        }
                    }
                } catch (NumberFormatException e) {
                    // 继续尝试文件系统访问
                }
            }
            
            // 尝试从文件系统直接访问（旧版方式）
            Path filePath = Paths.get(uploadPath, fileName);
            
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
            
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CACHE_CONTROL, "max-age=31536000")
                    .body(resource);
                    
        } catch (Exception e) {
            log.error("文件访问失败: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
}
