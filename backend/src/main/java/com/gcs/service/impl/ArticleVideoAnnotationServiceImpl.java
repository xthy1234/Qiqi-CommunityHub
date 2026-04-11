package com.gcs.service.impl;


import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.gcs.converter.VideoAnnotationConverter;
import com.gcs.dao.ArticleVideoAnnotationDao;
import com.gcs.dto.VideoAnnotationCreateDTO;
import com.gcs.dto.VideoAnnotationUpdateDTO;
import com.gcs.entity.Article;
import com.gcs.entity.ArticleVideoAnnotation;
import com.gcs.entity.User;
import com.gcs.service.ArticleService;
import com.gcs.service.ArticleVideoAnnotationService;
import com.gcs.service.UserService;
import com.gcs.utils.PageUtils;
import com.gcs.utils.Query;
import com.gcs.vo.UserSimpleVO;
import com.gcs.vo.VideoAnnotationDetailVO;
import com.gcs.vo.VideoAnnotationVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * 文章视频注释服务实现
 * @author 
 * @date 2026-04-05
 */
@Slf4j
@Service
public class ArticleVideoAnnotationServiceImpl extends ServiceImpl<ArticleVideoAnnotationDao, ArticleVideoAnnotation> implements ArticleVideoAnnotationService {

    @Autowired
    private ArticleVideoAnnotationDao annotationDao;

    @Autowired
    private VideoAnnotationConverter annotationConverter;

    @Autowired
    private ArticleService articleService;

    @Autowired
    private UserService userService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public VideoAnnotationDetailVO createAnnotation(Long articleId, Long creatorId, Object dto) {
        if (!(dto instanceof VideoAnnotationCreateDTO)) {
            throw new IllegalArgumentException("无效的请求参数");
        }
        
        VideoAnnotationCreateDTO createDTO = (VideoAnnotationCreateDTO) dto;

        Article article = articleService.getById(articleId);
        if (article == null) {
            throw new IllegalArgumentException("文章不存在");
        }

        if (createDTO.getEndTime() != null && createDTO.getEndTime().compareTo(createDTO.getStartTime()) < 0) {
            throw new IllegalArgumentException("结束时间不能小于开始时间");
        }

        ArticleVideoAnnotation annotation = annotationConverter.toEntity(createDTO);
        annotation.setArticleId(articleId);
        annotation.setCreatorId(creatorId);
        annotation.setStatus(1);
        annotation.setIsDeleted(false);

        save(annotation);
        log.info("创建视频注释成功: annotationId={}, articleId={}, creatorId={}", 
                annotation.getId(), articleId, creatorId);

        return buildDetailVO(annotation);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public VideoAnnotationDetailVO updateAnnotation(Long articleId, Long annotationId, Long operatorId, Object dto) {
        if (!(dto instanceof VideoAnnotationUpdateDTO)) {
            throw new IllegalArgumentException("无效的请求参数");
        }

        VideoAnnotationUpdateDTO updateDTO = (VideoAnnotationUpdateDTO) dto;

        ArticleVideoAnnotation annotation = getById(annotationId);
        if (annotation == null || annotation.getIsDeleted()) {
            throw new IllegalArgumentException("注释不存在");
        }

        if (!annotation.getArticleId().equals(articleId)) {
            throw new IllegalArgumentException("注释不属于该文章");
        }

        if (!checkPermission(annotationId, operatorId)) {
            throw new SecurityException("无权限修改此注释");
        }

        if (updateDTO.getEndTime() != null && updateDTO.getStartTime() != null) {
            if (updateDTO.getEndTime().compareTo(updateDTO.getStartTime()) < 0) {
                throw new IllegalArgumentException("结束时间不能小于开始时间");
            }
        }

        annotationConverter.updateEntity(updateDTO, annotation);
        updateById(annotation);
        
        log.info("更新视频注释成功: annotationId={}, operatorId={}", annotationId, operatorId);
        return buildDetailVO(annotation);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteAnnotation(Long articleId, Long annotationId, Long operatorId) {
        ArticleVideoAnnotation annotation = getById(annotationId);
        if (annotation == null || annotation.getIsDeleted()) {
            throw new IllegalArgumentException("注释不存在");
        }

        if (!annotation.getArticleId().equals(articleId)) {
            throw new IllegalArgumentException("注释不属于该文章");
        }

        if (!checkPermission(annotationId, operatorId)) {
            throw new SecurityException("无权限删除此注释");
        }

        boolean success = removeById(annotationId);
        if (success) {
            log.info("删除视频注释成功: annotationId={}, operatorId={}", annotationId, operatorId);
        }
        return success;
    }

    @Override
    public List<VideoAnnotationDetailVO> getAnnotationsByArticle(Long articleId, String videoUrl) {
        List<ArticleVideoAnnotation> annotations;
        
        if (videoUrl != null && !videoUrl.isEmpty()) {
            annotations = annotationDao.selectByArticleAndVideo(articleId, videoUrl);
        } else {
            annotations = annotationDao.selectByArticleId(articleId);
        }

        return annotationConverter.toDetailVOList(annotations);
    }

    @Override
    public VideoAnnotationDetailVO getAnnotationById(Long annotationId) {
        ArticleVideoAnnotation annotation = getById(annotationId);
        if (annotation == null || annotation.getIsDeleted()) {
            return null;
        }
        return buildDetailVO(annotation);
    }

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        Query<ArticleVideoAnnotation> query = new Query<>(params);
        Page<ArticleVideoAnnotation> page = query.getPage();

        QueryWrapper<ArticleVideoAnnotation> wrapper = new QueryWrapper<>();
        wrapper.eq("is_deleted", false);

        if (params.get("articleId") != null) {
            wrapper.eq("article_id", params.get("articleId"));
        }
        if (params.get("videoUrl") != null && !params.get("videoUrl").toString().isEmpty()) {
            wrapper.eq("video_url", params.get("videoUrl"));
        }
        if (params.get("creatorId") != null) {
            wrapper.eq("creator_id", params.get("creatorId"));
        }
        if (params.get("status") != null) {
            wrapper.eq("status", params.get("status"));
        }
        if (params.get("keyword") != null && !params.get("keyword").toString().isEmpty()) {
            String keyword = params.get("keyword").toString();
            wrapper.and(w -> w.like("title", keyword).or().like("content", keyword));
        }

        String sortBy = params.get("sortBy") != null ? params.get("sortBy").toString() : "startTime";
        if ("createTime".equals(sortBy)) {
            wrapper.orderByDesc("create_time");
        } else {
            wrapper.orderByAsc("sort_order").orderByAsc("start_time");
        }

        Page<ArticleVideoAnnotation> resultPage = this.page(page, wrapper);
        return new PageUtils(resultPage);
    }

    @Override
    public boolean checkPermission(Long annotationId, Long operatorId) {
        ArticleVideoAnnotation annotation = getById(annotationId);
        if (annotation == null) {
            return false;
        }

        User user = userService.getById(operatorId);
        if (user == null) {
            return false;
        }

        Article article = articleService.getById(annotation.getArticleId());
        if (article == null) {
            return false;
        }

        boolean isAuthor = article.getAuthorId().equals(operatorId);
        boolean isAdmin = user.getRoleId() != null && user.getRoleId() == 1L;

        return isAuthor || isAdmin;
    }

    private VideoAnnotationDetailVO buildDetailVO(ArticleVideoAnnotation annotation) {
        VideoAnnotationDetailVO detailVO = annotationConverter.toDetailVO(annotation);
        
        if (annotation.getCreatorId() != null) {
            User creator = userService.getById(annotation.getCreatorId());
            if (creator != null) {
                UserSimpleVO userVO = new UserSimpleVO();
                userVO.setId(creator.getId());
                userVO.setNickname(creator.getNickname());
                userVO.setAvatar(creator.getAvatar());
                detailVO.setCreator(userVO);
            }
        }
        
        return detailVO;
    }
}
