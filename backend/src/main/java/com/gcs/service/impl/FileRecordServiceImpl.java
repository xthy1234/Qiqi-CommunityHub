package com.gcs.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.gcs.converter.FileRecordConverter;
import com.gcs.dao.FileRecordDao;
import com.gcs.entity.FileRecord;
import com.gcs.service.FileRecordService;
import com.gcs.utils.PageUtils;
import com.gcs.utils.Query;
import com.gcs.vo.FileRecordVO;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FilenameUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
public class FileRecordServiceImpl extends ServiceImpl<FileRecordDao, FileRecord> implements FileRecordService {

    @Autowired
    private FileRecordDao fileRecordDao;

    @Autowired
    private FileRecordConverter fileRecordConverter;

    @Value("${file.upload.path:#{systemProperties['user.dir']}/uploads}")
    private String uploadPath;

    @Value("${file.upload.max-size:10485760}")
    private long maxFileSize;

    private static final List<String> IMAGE_EXTENSIONS = Arrays.asList("jpg", "jpeg", "png", "gif", "bmp", "webp");
    private static final List<String> VIDEO_EXTENSIONS = Arrays.asList("mp4", "avi", "mov", "wmv", "flv", "mkv");
    private static final List<String> DOCUMENT_EXTENSIONS = Arrays.asList("pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt", "csv");

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FileRecord uploadFile(MultipartFile file, Long uploaderId, String fileType, 
                                 String description, Boolean isPublic) {
        try {
            validateFile(file);

            String originalFilename = file.getOriginalFilename();
            String extension = FilenameUtils.getExtension(originalFilename).toLowerCase();
            
            if (fileType == null || fileType.isEmpty()) {
                fileType = classifyFileType(extension);
            }

            String md5 = calculateMD5(file);
            
            QueryWrapper<FileRecord> wrapper = new QueryWrapper<>();
            wrapper.eq("file_md5", md5);
            FileRecord existingFile = this.getOne(wrapper);
            
            if (existingFile != null) {
                log.info("文件已存在，返回已有记录: {}", existingFile.getFileName());
                return existingFile;
            }

            String storedFileName = generateFileName(extension);
            File uploadDirectory = new File(uploadPath);
            if (!uploadDirectory.exists()) {
                uploadDirectory.mkdirs();
            }

            File destFile = new File(uploadDirectory, storedFileName);
            file.transferTo(destFile);

            FileRecord fileRecord = new FileRecord();
            fileRecord.setFileName(storedFileName);
            fileRecord.setOriginalFileName(originalFilename);
            fileRecord.setFilePath(destFile.getAbsolutePath());
            fileRecord.setFileSize(file.getSize());
            fileRecord.setFileMd5(md5);
            fileRecord.setMimeType(file.getContentType());
            fileRecord.setFileType(fileType);
            fileRecord.setUploaderId(uploaderId);
            fileRecord.setUploadTime(LocalDateTime.now());
            fileRecord.setIsPublic(isPublic != null ? isPublic : true);
            fileRecord.setStatus(1);
            fileRecord.setDescription(description);
            fileRecord.setDownloadTimes(0);

            if ("image".equals(fileType) || "video".equals(fileType)) {
                extractMediaInfo(fileRecord, destFile, fileType);
            }

            save(fileRecord);
            log.info("文件上传成功: {}, 大小: {} bytes", storedFileName, file.getSize());
            
            return fileRecord;

        } catch (IOException e) {
            log.error("文件上传失败: {}", e.getMessage(), e);
            throw new RuntimeException("文件上传失败: " + e.getMessage());
        }
    }

    @Override
    public FileRecordVO getFileById(Long id) {
        FileRecord fileRecord = getById(id);
        if (fileRecord == null) {
            return null;
        }
        return fileRecordConverter.toVO(fileRecord);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FileRecord downloadFile(Long id) {
        FileRecord fileRecord = getById(id);
        if (fileRecord != null) {
            fileRecord.setDownloadTimes(fileRecord.getDownloadTimes() + 1);
            updateById(fileRecord);
        }
        return fileRecord;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteFile(Long id, Long operatorId) {
        FileRecord fileRecord = getById(id);
        if (fileRecord == null) {
            return false;
        }

        if (!fileRecord.getUploaderId().equals(operatorId)) {
            log.warn("用户 {} 无权删除文件 {}", operatorId, id);
            return false;
        }

        File file = new File(fileRecord.getFilePath());
        if (file.exists()) {
            file.delete();
        }

        if (fileRecord.getThumbnailPath() != null) {
            File thumbFile = new File(fileRecord.getThumbnailPath());
            if (thumbFile.exists()) {
                thumbFile.delete();
            }
        }

        return removeById(id);
    }

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        Query<FileRecord> query = new Query<>(params);
        Page<FileRecord> page = query.getPage();
        
        QueryWrapper<FileRecord> wrapper = new QueryWrapper<>();
        
        if (params.get("fileType") != null && !params.get("fileType").toString().isEmpty()) {
            wrapper.eq("file_type", params.get("fileType"));
        }
        if (params.get("uploaderId") != null) {
            wrapper.eq("uploader_id", params.get("uploaderId"));
        }
        if (params.get("status") != null) {
            wrapper.eq("status", params.get("status"));
        }
        if (params.get("keyword") != null && !params.get("keyword").toString().isEmpty()) {
            String keyword = params.get("keyword").toString();
            wrapper.and(w -> w.like("original_file_name", keyword)
                    .or().like("description", keyword));
        }
        
        wrapper.orderByDesc("upload_time");
        
        Page<FileRecord> resultPage = this.page(page, wrapper);
        return new PageUtils(resultPage);
    }

    @Override
    public FileRecord getByMd5(String md5) {
        QueryWrapper<FileRecord> wrapper = new QueryWrapper<>();
        wrapper.eq("file_md5", md5);
        return this.getOne(wrapper);
    }

    @Override
    public List<FileRecordVO> getUserFiles(Long uploaderId, String fileType) {
        QueryWrapper<FileRecord> wrapper = new QueryWrapper<>();
        wrapper.eq("uploader_id", uploaderId);
        if (fileType != null && !fileType.isEmpty()) {
            wrapper.eq("file_type", fileType);
        }
        wrapper.orderByDesc("upload_time");
        List<FileRecord> list = list(wrapper);
        return fileRecordConverter.toVOList(list);
    }

    private void validateFile(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IOException("上传文件不能为空");
        }
        if (file.getSize() > maxFileSize) {
            throw new IOException("文件大小超过限制: " + (maxFileSize / 1024 / 1024) + "MB");
        }
    }

    private String classifyFileType(String extension) {
        if (IMAGE_EXTENSIONS.contains(extension)) {
            return "image";
        } else if (VIDEO_EXTENSIONS.contains(extension)) {
            return "video";
        } else {
            return "document";
        }
    }

    private String calculateMD5(MultipartFile file) throws IOException {
        return DigestUtils.md5Hex(file.getInputStream());
    }

    private String generateFileName(String extension) {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
        String uuid = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        return timestamp + "_" + uuid + "." + extension;
    }

    private void extractMediaInfo(FileRecord fileRecord, File file, String fileType) {
        try {
            if ("image".equals(fileType)) {
                BufferedImage image = ImageIO.read(file);
                if (image != null) {
                    fileRecord.setWidth(image.getWidth());
                    fileRecord.setHeight(image.getHeight());
                }
            } else if ("video".equals(fileType)) {
                log.debug("视频元数据提取需要 FFmpeg 支持，暂不处理");
            }
        } catch (Exception e) {
            log.warn("提取媒体信息失败: {}", e.getMessage());
        }
    }
}
