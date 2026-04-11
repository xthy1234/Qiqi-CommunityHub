package com.gcs.service;


import com.baomidou.mybatisplus.extension.service.IService;
import com.gcs.entity.VideoDanmaku;
import com.gcs.utils.PageUtils;
import com.gcs.vo.VideoDanmakuDetailVO;
import com.gcs.vo.VideoDanmakuVO;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public interface VideoDanmakuService extends IService<VideoDanmaku> {

    VideoDanmakuDetailVO sendDanmaku(Long userId, String ipAddress, Object dto);

    List<VideoDanmakuDetailVO> getDanmakuByTimeRange(Long articleId, String videoUrl, BigDecimal fromTime, BigDecimal toTime);

    List<VideoDanmakuVO> getLatestDanmaku(Long articleId, String videoUrl, Integer limit);

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

    Integer countByVideo(Long articleId, String videoUrl);
}
