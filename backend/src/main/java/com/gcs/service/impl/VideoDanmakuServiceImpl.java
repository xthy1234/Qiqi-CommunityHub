package com.gcs.service.impl;


import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.gcs.converter.VideoDanmakuConverter;
import com.gcs.dao.VideoDanmakuDao;
import com.gcs.dto.DanmakuSendDTO;
import com.gcs.entity.User;
import com.gcs.entity.VideoDanmaku;
import com.gcs.service.UserService;
import com.gcs.service.VideoDanmakuService;
import com.gcs.utils.PageUtils;
import com.gcs.utils.Query;
import com.gcs.vo.UserSimpleVO;
import com.gcs.vo.VideoDanmakuDetailVO;
import com.gcs.vo.VideoDanmakuVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 视频弹幕服务实现
 * @author 
 * @date 2026-04-05
 */
@Slf4j
@Service
public class VideoDanmakuServiceImpl extends ServiceImpl<VideoDanmakuDao, VideoDanmaku> implements VideoDanmakuService {

    @Autowired
    private VideoDanmakuDao danmakuDao;

    @Autowired
    private VideoDanmakuConverter danmakuConverter;

    @Autowired
    private UserService userService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public VideoDanmakuDetailVO sendDanmaku(Long userId, String ipAddress, Object dto) {
        if (!(dto instanceof DanmakuSendDTO)) {
            throw new IllegalArgumentException("无效的请求参数");
        }

        DanmakuSendDTO sendDTO = (DanmakuSendDTO) dto;

        // 校验内容长度
        if (sendDTO.getContent().length() > 200) {
            throw new IllegalArgumentException("弹幕内容不能超过200字符");
        }

        // TODO: 敏感词过滤（可集成现有工具或调用外部API）
        // if (SensitiveWordFilter.containsSensitiveWord(sendDTO.getContent())) {
        //     throw new SecurityException("弹幕内容包含敏感词");
        // }

        // 校验时间范围
        if (sendDTO.getTime().compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("弹幕时间不能为负数");
        }

        // 校验位置
        if (sendDTO.getPosition() != null && (sendDTO.getPosition() < 0 || sendDTO.getPosition() > 2)) {
            throw new IllegalArgumentException("弹幕位置只能是 0(滚动)、1(顶部)、2(底部)");
        }

        VideoDanmaku danmaku = danmakuConverter.toEntity(sendDTO);
        danmaku.setUserId(userId);
        danmaku.setIpAddress(ipAddress);
        danmaku.setStatus(1);
        danmaku.setIsAdmin(false);

        // 判断是否为管理员
        User user = userService.getById(userId);
        if (user != null && user.getRoleId() != null && user.getRoleId() == 1L) {
            danmaku.setIsAdmin(true);
        }

        save(danmaku);
        log.info("发送弹幕成功: danmakuId={}, userId={}, videoUrl={}", 
                danmaku.getId(), userId, sendDTO.getVideoUrl());

        return buildDetailVO(danmaku);
    }

    @Override
    public List<VideoDanmakuDetailVO> getDanmakuByTimeRange(String videoUrl, BigDecimal fromTime, BigDecimal toTime) {
        if (fromTime == null) {
            fromTime = BigDecimal.ZERO;
        }
        if (toTime == null) {
            toTime = new BigDecimal("999999");
        }

        List<VideoDanmaku> list = danmakuDao.selectByVideoAndTimeRange(videoUrl, fromTime, toTime);
        return danmakuConverter.toDetailVOList(list);
    }

    @Override
    public List<VideoDanmakuVO> getLatestDanmaku(String videoUrl, Integer limit) {
        if (limit == null || limit <= 0) {
            limit = 100;
        }
        if (limit > 500) {
            limit = 500; // 限制最大返回数量
        }

        List<VideoDanmaku> list = danmakuDao.selectLatestByVideo(videoUrl, limit);
        return danmakuConverter.toVOList(list);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean blockDanmaku(Long danmakuId) {
        VideoDanmaku danmaku = getById(danmakuId);
        if (danmaku == null) {
            throw new IllegalArgumentException("弹幕不存在");
        }

        danmaku.setStatus(0);
        boolean success = updateById(danmaku);
        
        if (success) {
            log.info("屏蔽弹幕成功: danmakuId={}", danmakuId);
        }
        return success;
    }

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        Query<VideoDanmaku> query = new Query<>(params);
        Page<VideoDanmaku> page = query.getPage();

        QueryWrapper<VideoDanmaku> wrapper = new QueryWrapper<>();

        if (params.get("videoUrl") != null && !params.get("videoUrl").toString().isEmpty()) {
            wrapper.eq("video_url", params.get("videoUrl"));
        }
        if (params.get("userId") != null) {
            wrapper.eq("user_id", params.get("userId"));
        }
        if (params.get("articleId") != null) {
            wrapper.eq("article_id", params.get("articleId"));
        }
        if (params.get("status") != null) {
            wrapper.eq("status", params.get("status"));
        }
        if (params.get("keyword") != null && !params.get("keyword").toString().isEmpty()) {
            wrapper.like("content", params.get("keyword").toString());
        }

        wrapper.orderByDesc("create_time");

        Page<VideoDanmaku> resultPage = this.page(page, wrapper);
        return new PageUtils(resultPage);
    }

    @Override
    public Integer countByVideo(String videoUrl) {
        return danmakuDao.countByVideo(videoUrl);
    }

    private VideoDanmakuDetailVO buildDetailVO(VideoDanmaku danmaku) {
        VideoDanmakuDetailVO detailVO = danmakuConverter.toDetailVO(danmaku);
        
        if (danmaku.getUserId() != null) {
            User sender = userService.getById(danmaku.getUserId());
            if (sender != null) {
                UserSimpleVO userVO = new UserSimpleVO();
                userVO.setId(sender.getId());
                userVO.setNickname(sender.getNickname());
                userVO.setAvatar(sender.getAvatar());
                detailVO.setSender(userVO);
            }
        }
        
        return detailVO;
    }
}
