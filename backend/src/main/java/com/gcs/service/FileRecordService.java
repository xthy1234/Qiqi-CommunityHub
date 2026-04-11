package com.gcs.service;


import com.baomidou.mybatisplus.extension.service.IService;
import com.gcs.entity.FileRecord;
import com.gcs.utils.PageUtils;
import com.gcs.vo.FileRecordVO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * 文件记录服务接口
 * @author 
 * @date 2026-04-05
 */
public interface FileRecordService extends IService<FileRecord> {

    /**
     * 上传文件并记录
     * @param file 上传的文件
     * @param uploaderId 上传者 ID
     * @param fileType 文件类型分类
     * @param description 文件描述
     * @param isPublic 是否公开
     * @return 文件记录
     */
    FileRecord uploadFile(MultipartFile file, Long uploaderId, String fileType, 
                         String description, Boolean isPublic);

    /**
     * 根据 ID 获取文件记录
     * @param id 文件 ID
     * @return 文件记录 VO
     */
    FileRecordVO getFileById(Long id);

    /**
     * 下载文件（增加下载次数）
     * @param id 文件 ID
     * @return 文件记录
     */
    FileRecord downloadFile(Long id);

    /**
     * 删除文件（逻辑删除或物理删除）
     * @param id 文件 ID
     * @param operatorId 操作者 ID
     * @return 是否成功
     */
    boolean deleteFile(Long id, Long operatorId);

    /**
     * 分页查询文件列表
     * @param params 查询参数
     * @return 分页结果
     */
    PageUtils queryPage(Map<String, Object> params);

    /**
     * 根据 MD5 查询文件
     * @param md5 文件 MD5
     * @return 文件记录
     */
    FileRecord getByMd5(String md5);

    /**
     * 获取用户的文件列表
     * @param uploaderId 上传者 ID
     * @param fileType 文件类型（可选）
     * @return 文件列表
     */
    List<FileRecordVO> getUserFiles(Long uploaderId, String fileType);
}
