package com.gcs.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.gcs.dao.SwiperDao;
import com.gcs.entity.Swiper;
import com.gcs.enums.CommonStatus;
import com.gcs.service.SwiperService;
import com.gcs.utils.PageUtils;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * 轮播图服务实现类
 * @author
 * @date 2026-04-16
 */
@Service("swiperService")
public class SwiperServiceImpl extends ServiceImpl<SwiperDao, Swiper> implements SwiperService {

    @Override
    public PageUtils queryPage(Map<String, Object> params, Wrapper<Swiper> wrapper) {
        long limit = 10;
        long page = 1;

        if(params.get("page") != null){
            page = Long.parseLong(params.get("page").toString());
        }
        if(params.get("limit") != null){
            limit = Long.parseLong(params.get("limit").toString());
        }

        IPage<Swiper> pageData = new Page<>(page, limit);

        if (!(wrapper instanceof QueryWrapper)) {
            QueryWrapper<Swiper> tempWrapper = new QueryWrapper<>();
            if (wrapper != null) {
                tempWrapper.setEntity(wrapper.getEntity());
            }
            wrapper = tempWrapper;
        }

        QueryWrapper<Swiper> queryWrapper = (QueryWrapper<Swiper>) wrapper;
        IPage<Swiper> resultPage = this.page(pageData, queryWrapper);

        return new PageUtils(resultPage);
    }

    @Override
    public List<Swiper> getAllEnabledSwipers() {
        QueryWrapper<Swiper> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("status", 1)
                .orderByAsc("sort");
        return this.list(queryWrapper);
    }

    @Override
    public boolean updateStatus(Long swiperId, Integer status) {
        Swiper swiper = this.getById(swiperId);
        if (swiper == null) {
            return false;
        }
        CommonStatus commonStatus = CommonStatus.valueOf(status);
        swiper.setStatus(commonStatus);
        return this.updateById(swiper);
    }

    @Override
    public boolean deleteSwipers(List<Long> swiperIds) {
        return this.removeByIds(swiperIds);
    }

    @Override
    public Swiper selectView(Wrapper<Swiper> queryWrapper) {
        validateWrapper(queryWrapper);
        // Swiper 是单表查询，使用 QueryWrapper 即可
        if (queryWrapper instanceof QueryWrapper) {
            return ((SwiperDao) baseMapper).selectView((QueryWrapper<Swiper>) queryWrapper);
        } else {
            throw new IllegalArgumentException("不支持的 Wrapper 类型，请使用 QueryWrapper");
        }
    }

    /**
     * 验证包装器类型
     */
    private void validateWrapper(Wrapper<Swiper> queryWrapper) {
        if (queryWrapper == null) {
            throw new IllegalArgumentException("查询条件不能为空");
        }
    }
}
