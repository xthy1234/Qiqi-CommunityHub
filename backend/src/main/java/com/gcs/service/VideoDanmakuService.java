package com.gcs.service;


import com.baomidou.mybatisplus.extension.service.IService;
import com.gcs.entity.VideoDanmaku;
import com.gcs.utils.PageUtils;
import com.gcs.vo.VideoDanmakuDetailVO;
import com.gcs.vo.VideoDanmakuVO;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 视频弹幕服务接口
 * @author 
 * @date 2026-04-05
 */
public interface VideoDanmakuService extends IService<VideoDanmaku> {

    /**
     * 发送弹幕
     * @param userId 用户ID
     * @param ipAddress IP地址
     * @param dto 发送请求
     * @return 弹幕详情
     */
    VideoDanmakuDetailVO sendDanmaku(Long userId, String ipAddress, Object dto);

    /**
     * 获取视频弹幕列表（按时间范围）
     * @param videoUrl 视频URL
     * @param fromTime 开始时间
     * @param toTime 结束时间
     * @return 弹幕列表
     */
    List<VideoDanmakuDetailVO> getDanmakuByTimeRange(String videoUrl, BigDecimal fromTime, BigDecimal toTime);

    /**
     * 获取视频最新弹幕
     * @param videoUrl 视频URL
     * @param limit 数量限制
     * @return 弹幕列表
     */
    List<VideoDanmakuVO> getLatestDanmaku(String videoUrl, Integer limit);

    /**
     * 屏蔽弹幕（管理员操作）
     * @param danmakuId 弹幕ID
     * @return 是否成功
     */
    boolean blockDanmaku(Long danmakuId);

    /**
     * 分页查询弹幕列表
     * @param params 查询参数
     * @return 分页结果
     */
    PageUtils queryPage(Map<String, Object> params);

    /**
     * 统计视频弹幕数量
     * @param videoUrl 视频URL
     * @return 弹幕数量
     */
    Integer countByVideo(String videoUrl);
}
