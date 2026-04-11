package com.gcs.dao;


import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.gcs.entity.VideoDanmaku;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 视频弹幕 DAO
 * @author 
 * @date 2026-04-05
 */
@Mapper
public interface VideoDanmakuDao extends BaseMapper<VideoDanmaku> {

    /**
     * 根据视频URL和时间范围查询弹幕
     * @param videoUrl 视频URL
     * @param fromTime 开始时间
     * @param toTime 结束时间
     * @return 弹幕列表
     */
    List<VideoDanmaku> selectByVideoAndTimeRange(@Param("videoUrl") String videoUrl,
                                                   @Param("fromTime") BigDecimal fromTime,
                                                   @Param("toTime") BigDecimal toTime);

    /**
     * 根据文章ID、视频URL和时间范围查询弹幕
     * @param articleId 文章ID
     * @param videoUrl 视频URL
     * @param fromTime 开始时间
     * @param toTime 结束时间
     * @return 弹幕列表
     */
    List<VideoDanmaku> selectByArticleAndTimeRange(@Param("articleId") Long articleId, 
                                                    @Param("videoUrl") String videoUrl, 
                                                    @Param("fromTime") BigDecimal fromTime, 
                                                    @Param("toTime") BigDecimal toTime);

    /**
     * 根据视频URL查询最新弹幕
     * @param videoUrl 视频URL
     * @param limit 数量限制
     * @return 弹幕列表
     */
    List<VideoDanmaku> selectLatestByVideo(@Param("videoUrl") String videoUrl,
                                            @Param("limit") Integer limit);

    /**
     * 根据文章ID和视频URL查询最新弹幕
     * @param articleId 文章ID
     * @param videoUrl 视频URL
     * @param limit 数量限制
     * @return 弹幕列表
     */
    List<VideoDanmaku> selectLatestByArticleAndVideo(@Param("articleId") Long articleId,
                                                      @Param("videoUrl") String videoUrl,
                                                      @Param("limit") Integer limit);

    Integer countByArticleAndVideo(@Param("articleId") Long articleId, 
                                    @Param("videoUrl") String videoUrl);
}
