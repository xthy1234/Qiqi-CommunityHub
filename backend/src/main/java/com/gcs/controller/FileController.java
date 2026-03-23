package com.gcs.controller;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;

import jakarta.servlet.http.HttpServletResponse;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.ResourceUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.gcs.annotation.IgnoreAuth;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.gcs.entity.Config;
import com.gcs.service.ConfigService;
import com.gcs.utils.R;

import lombok.extern.slf4j.Slf4j;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

/**
 * 文件上传下载控制器
 * 提供文件上传和下载的 RESTful API 接口
 * @author 
 * @date 2026-04-16
 */
@Slf4j
@Tag(name = "文件管理", description = "文件上传、下载相关的 RESTful API 接口")
@RestController
@RequestMapping("/files")
public class FileController {
    
    @Autowired
    private ConfigService configService;
    
    @Value("${file.upload.path:#{systemProperties['user.dir']}/uploads}")
    private String uploadPath;
    
    @Value("${file.upload.max-size:10485760}")
    private long maxFileSize;
    
    private static final String FACE_FILE_CONFIG = "faceFile";
    private static final String TEMPLATE_SUFFIX = "_template";
    
    private static final List<String> ALLOWED_EXTENSIONS = Arrays.asList(
        "jpg", "jpeg", "png", "gif", "bmp", "webp",
        "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx",
        "txt", "csv", "zip", "rar", "7z"
    );
    
    private static final List<String> ALLOWED_MIME_TYPES = Arrays.asList(
        "image/jpeg", "image/png", "image/gif", "image/bmp", "image/webp",
        "application/pdf", "application/msword", 
        "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
        "text/plain", "text/csv",
        "application/zip", "application/x-zip-compressed", "application/x-rar-compressed",
        "application/x-7z-compressed",
        "application/vnd.ms-excel",
        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
        "application/vnd.ms-powerpoint",
        "application/vnd.openxmlformats-officedocument.presentationml.presentation"
    );

    /**
     * 上传文件
     */
    @Operation(summary = "上传文件", description = "上传文件到服务器，支持多种文件类型")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "上传成功"),
        @ApiResponse(responseCode = "400", description = "文件格式不支持或超过大小限制"),
        @ApiResponse(responseCode = "500", description = "上传失败")
    })
    @PostMapping
    @IgnoreAuth
    public R uploadFile(
        @Parameter(description = "上传的文件", required = true) @RequestParam("file") MultipartFile multipartFile, 
        @Parameter(description = "文件类型 (可选)") @RequestParam(required = false) String type) {
        try {
            validateFile(multipartFile);
            
            String fileExtension = getFileExtension(multipartFile.getOriginalFilename());
            validateFileExtension(fileExtension);
            
            File uploadDirectory = createUploadDirectory();
            
            String fileName = generateFileName(fileExtension, type);
            
            if (isTemplateFile(type)) {
                handleTemplateFile(uploadDirectory, fileName);
            }
            
            File destinationFile = new File(uploadDirectory, fileName);
            multipartFile.transferTo(destinationFile);
            
            if (isFaceFile(type)) {
                updateFaceFileConfig(fileName);
            }
            
            log.info("文件上传成功：{}, 大小：{} bytes", fileName, multipartFile.getSize());
            return R.ok("文件上传成功").put("fileName", fileName).put("size", multipartFile.getSize()).put("url", "/files/" + fileName);
            
        } catch (IOException e) {
            // 🔥 IOException 返回 400（客户端错误）
            log.error("文件上传失败：{}", e.getMessage(), e);
            return R.error(400, e.getMessage());
        } catch (Exception e) {
            // 🔥 其他异常返回 500（服务器错误）
            log.error("文件上传失败：{}", e.getMessage(), e);
            return R.error(500, "上传失败：" + e.getMessage());
        }
    }
    
    /**
     * 下载文件
     */
    @Operation(summary = "下载文件", description = "根据文件名下载文件")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "下载成功"),
        @ApiResponse(responseCode = "404", description = "文件不存在"),
        @ApiResponse(responseCode = "500", description = "下载失败")
    })
    @GetMapping("/{fileName}")
    @IgnoreAuth
    public ResponseEntity<Resource> downloadFile(
        @Parameter(description = "文件名", required = true) @PathVariable String fileName,
        HttpServletResponse response) {
        try {
            if (StringUtils.isBlank(fileName)) {
                return ResponseEntity.badRequest().build();
            }
            
            File uploadDirectory = createUploadDirectory();
            Path filePath = Paths.get(uploadDirectory.getAbsolutePath(), fileName);
            
            if (!Files.exists(filePath)) {
                log.warn("文件不存在：{}", filePath.toString());
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
                    .header(HttpHeaders.CONTENT_DISPOSITION, 
                           "attachment; filename=\"" + fileName + "\"")
                    .body(resource);
                    
        } catch (Exception e) {
            log.error("文件下载失败：{}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * 获取文件信息
     */
    @Operation(summary = "获取文件信息", description = "获取文件的详细信息（大小、修改时间等）")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "获取成功"),
        @ApiResponse(responseCode = "404", description = "文件不存在"),
        @ApiResponse(responseCode = "500", description = "获取失败")
    })
    @GetMapping("/{fileName}/info")
    @IgnoreAuth
    public R getFileInfo(
        @Parameter(description = "文件名", required = true) @PathVariable String fileName) {
        try {
            if (StringUtils.isBlank(fileName)) {
                return R.error("文件名不能为空");
            }
            
            File uploadDirectory = createUploadDirectory();
            File file = new File(uploadDirectory, fileName);
            
            if (!file.exists()) {
                return R.error("文件不存在");
            }
            
            return R.ok()
                    .put("fileName", fileName)
                    .put("size", file.length())
                    .put("readableSize", getReadableFileSize(file.length()))
                    .put("lastModified", file.lastModified())
                    .put("extension", FilenameUtils.getExtension(fileName));
                    
        } catch (Exception e) {
            log.error("获取文件信息失败：{}", e.getMessage(), e);
            return R.error("获取文件信息失败");
        }
    }
    
    /**
     * 删除文件
     */
    @Operation(summary = "删除文件", description = "根据文件名删除文件")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "删除成功"),
        @ApiResponse(responseCode = "404", description = "文件不存在"),
        @ApiResponse(responseCode = "500", description = "删除失败")
    })
    @DeleteMapping("/{fileName}")
    public R deleteFile(
        @Parameter(description = "文件名", required = true) @PathVariable String fileName) {
        try {
            if (StringUtils.isBlank(fileName)) {
                return R.error("文件名不能为空");
            }
            
            File uploadDirectory = createUploadDirectory();
            File file = new File(uploadDirectory, fileName);
            
            if (!file.exists()) {
                return R.error("文件不存在");
            }
            
            if (file.delete()) {
                log.info("文件删除成功：{}", fileName);
                return R.ok("文件删除成功");
            } else {
                return R.error("文件删除失败");
            }
            
        } catch (Exception e) {
            log.error("文件删除失败：{}", e.getMessage(), e);
            return R.error("文件删除失败");
        }
    }
    
    // ==================== 私有方法 ====================
    
    /**
     * 验证上传文件
     */
    private void validateFile(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IOException("上传文件不能为空");
        }
        
        if (file.getSize() > maxFileSize) {
            throw new IOException(String.format("文件大小超过限制：%s (最大：%s)", 
                getReadableFileSize(file.getSize()), 
                getReadableFileSize(maxFileSize)));
        }
        
        String contentType = file.getContentType();
        if (contentType != null && !ALLOWED_MIME_TYPES.contains(contentType.toLowerCase())) {
            throw new IOException("不支持的文件类型：" + contentType);
        }
    }
    
    /**
     * 验证文件扩展名
     */
    private void validateFileExtension(String extension) throws IOException {
        if (StringUtils.isBlank(extension)) {
            throw new IOException("无法识别文件类型");
        }
        
        if (!ALLOWED_EXTENSIONS.contains(extension.toLowerCase())) {
            throw new IOException("不支持的文件格式：" + extension);
        }
    }
    
    /**
     * 获取文件扩展名
     */
    private String getFileExtension(String originalFilename) {
        if (StringUtils.isBlank(originalFilename)) {
            throw new IllegalArgumentException("文件名不能为空");
        }
        
        return FilenameUtils.getExtension(originalFilename).toLowerCase();
    }
    
    /**
     * 创建上传目录
     */
    private File createUploadDirectory() throws IOException {
        // 直接使用配置的上传路径
        File uploadDir = new File(uploadPath);
        
        if (!uploadDir.exists()) {
            if (!uploadDir.mkdirs()) {
                throw new IOException("无法创建上传目录：" + uploadDir.getAbsolutePath());
            }
        }
        
        if (!uploadDir.isDirectory() || !uploadDir.canWrite()) {
            throw new IOException("上传目录不可写：" + uploadDir.getAbsolutePath());
        }
        
        return uploadDir;
    }
    
    /**
     * 生成文件名
     */
    private String generateFileName(String fileExtension, String type) {
        if (isTemplateFile(type)) {
            return type + "." + fileExtension;
        }
        
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String uuid = java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        return timestamp + "_" + uuid + "." + fileExtension;
    }
    
    /**
     * 判断是否为模板文件
     */
    private boolean isTemplateFile(String type) {
        return StringUtils.isNotBlank(type) && type.contains(TEMPLATE_SUFFIX);
    }
    
    /**
     * 判断是否为头像文件
     */
    private boolean isFaceFile(String type) {
        return StringUtils.isNotBlank(type) && type.equals("1");
    }
    
    /**
     * 处理模板文件
     */
    private void handleTemplateFile(File uploadDirectory, String fileName) {
        File templateFile = new File(uploadDirectory, fileName);
        log.debug("处理模板文件：{}", fileName);
    }
    
    /**
     * 更新头像文件配置
     */
    private void updateFaceFileConfig(String fileName) {
        try {
            Config config = configService.getOne(new QueryWrapper<Config>().eq("name", FACE_FILE_CONFIG));
            
            if (config == null) {
                config = new Config();
                config.setConfigKey(FACE_FILE_CONFIG);
                config.setConfigValue(fileName);
                configService.save(config);
            } else {
                config.setConfigValue(fileName);
                configService.updateById(config);
            }
        } catch (Exception e) {
            log.error("更新头像配置失败：{}", e.getMessage(), e);
        }
    }
    
    /**
     * 获取可读的文件大小
     */
    private String getReadableFileSize(long size) {
        if (size <= 0) return "0 B";
        
        final String[] units = new String[]{"B", "KB", "MB", "GB", "TB"};
        int digitGroups = (int) (Math.log10(size) / Math.log10(1024));
        
        return String.format("%.1f %s", size / Math.pow(1024, digitGroups), units[digitGroups]);
    }
}
