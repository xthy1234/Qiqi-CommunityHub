package com.gcs.service;



import com.baomidou.mybatisplus.extension.service.IService;
import com.gcs.entity.ArticleVideoAnnotation;
import com.gcs.utils.PageUtils;
import com.gcs.vo.VideoAnnotationDetailVO;
import com.gcs.vo.VideoAnnotationVO;

import java.util.List;
import java.util.Map;

/**
 * 文章视频注释服务接口
 * @author 
 * @date 2026-04-05
 */
public interface ArticleVideoAnnotationService extends IService<ArticleVideoAnnotation> {

    /**
     * 创建视频注释
     * @param articleId 文章ID
     * @param creatorId 创建者ID
     * @param dto 创建请求
     * @return 注释详情
     */
    VideoAnnotationDetailVO createAnnotation(Long articleId, Long creatorId, Object dto);

    /**
     * 更新视频注释
     * @param articleId 文章ID
     * @param annotationId 注释ID
     * @param operatorId 操作者ID
     * @param dto 更新请求
     * @return 注释详情
     */
    VideoAnnotationDetailVO updateAnnotation(Long articleId, Long annotationId, Long operatorId, Object dto);

    /**
     * 删除视频注释
     * @param articleId 文章ID
     * @param annotationId 注释ID
     * @param operatorId 操作者ID
     * @return 是否成功
     */
    boolean deleteAnnotation(Long articleId, Long annotationId, Long operatorId);

    /**
     * 获取文章的注释列表
     * @param articleId 文章ID
     * @param videoUrl 视频URL（可选）
     * @return 注释列表
     */
    List<VideoAnnotationDetailVO> getAnnotationsByArticle(Long articleId, String videoUrl);

    /**
     * 获取单个注释详情
     * @param annotationId 注释ID
     * @return 注释详情
     */
    VideoAnnotationDetailVO getAnnotationById(Long annotationId);

    /**
     * 分页查询注释列表
     * @param params 查询参数
     * @return 分页结果
     */
    PageUtils queryPage(Map<String, Object> params);

    /**
     * 验证权限（仅作者或管理员可操作）
     * @param annotationId 注释ID
     * @param operatorId 操作者ID
     * @return 是否有权限
     */
    boolean checkPermission(Long annotationId, Long operatorId);
}
