package com.gcs.dao;


import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.gcs.entity.FileRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

/**
 * 文件记录 DAO
 * @author 
 * @date 2026-04-05
 */
@Mapper
public interface FileRecordDao extends BaseMapper<FileRecord> {

    /**
     * 根据 MD5 查询文件记录
     * @param md5 文件 MD5
     * @return 文件记录
     */
    FileRecord selectByMd5(@Param("md5") String md5);

    /**
     * 统计用户上传的文件数量
     * @param uploaderId 上传者 ID
     * @return 文件数量
     */
    Integer countByUploader(@Param("uploaderId") Long uploaderId);

    /**
     * 分页查询文件列表（支持多条件）
     * @param params 查询参数
     * @return 文件列表
     */
    List<FileRecord> selectFileList(@Param("params") Map<String, Object> params);
}
