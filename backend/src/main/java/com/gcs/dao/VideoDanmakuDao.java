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
     * 根据视频URL查询最新弹幕
     * @param videoUrl 视频URL
     * @param limit 数量限制
     * @return 弹幕列表
     */
    List<VideoDanmaku> selectLatestByVideo(@Param("videoUrl") String videoUrl,
                                            @Param("limit") Integer limit);

    /**
     * 分页查询弹幕列表（支持多条件）
     * @param params 查询参数
     * @return 弹幕列表
     */
    List<VideoDanmaku> selectDanmakuList(@Param("params") Map<String, Object> params);

    /**
     * 统计视频弹幕数量
     * @param videoUrl 视频URL
     * @return 弹幕数量
     */
    Integer countByVideo(@Param("videoUrl") String videoUrl);
}
