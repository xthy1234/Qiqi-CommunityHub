package com.gcs.converter;


import com.gcs.dto.FileUploadDTO;
import com.gcs.entity.FileRecord;
import com.gcs.vo.FileRecordVO;
import org.mapstruct.IterableMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;

import java.util.List;

/**
 * 文件记录转换器
 * @author 
 * @date 2026-04-05
 */
@Mapper(componentModel = "spring")
public interface FileRecordConverter {

    FileRecordConverter INSTANCE = Mappers.getMapper(FileRecordConverter.class);

    @Named("toVO")
    @Mapping(target = "fileUrl", expression = "java(buildFileUrl(entity))")
    @Mapping(target = "viewUrl", expression = "java(buildViewUrl(entity))")
    @Mapping(target = "downloadUrl", expression = "java(buildDownloadUrl(entity))")
    @Mapping(target = "thumbnailUrl", expression = "java(buildThumbnailUrl(entity))")
    @Mapping(target = "readableSize", expression = "java(formatFileSize(entity.getFileSize()))")
    FileRecordVO toVO(FileRecord entity);

    @IterableMapping(qualifiedByName = "toVO")
    List<FileRecordVO> toVOList(List<FileRecord> entities);

    default String buildFileUrl(FileRecord entity) {
        if (entity == null || entity.getId() == null) {
            return null;
        }
        return "/api/files/" + entity.getId();
    }

    default String buildViewUrl(FileRecord entity) {
        if (entity == null || entity.getId() == null) {
            return null;
        }
        return "/api/files/" + entity.getId() + "/view";
    }

    default String buildDownloadUrl(FileRecord entity) {
        if (entity == null || entity.getId() == null) {
            return null;
        }
        return "/api/files/" + entity.getId() + "/download";
    }

    default String buildThumbnailUrl(FileRecord entity) {
        if (entity == null || entity.getThumbnailPath() == null) {
            return null;
        }
        return "/api/files/" + entity.getId() + "/thumbnail";
    }

    default String formatFileSize(Long size) {
        if (size == null || size <= 0) {
            return "0 B";
        }
        final String[] units = new String[]{"B", "KB", "MB", "GB", "TB"};
        int digitGroups = (int) (Math.log10(size) / Math.log10(1024));
        return String.format("%.1f %s", size / Math.pow(1024, digitGroups), units[digitGroups]);
    }
}
