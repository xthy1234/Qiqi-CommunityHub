package com.gcs.entity.view;

import com.gcs.entity.Swiper;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;

/**
 * 轮播图视图扩展类
 * 用于展示轮播图的扩展信息
 * @author
 * @date 2026-04-16
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("swiper")
public class SwiperView extends Swiper implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 扩展字段：状态描述
     */
    private String statusDescription;

    /**
     * 构造函数
     * @param swiper 原始轮播图对象
     */
    public SwiperView(Swiper swiper) {
        if (swiper != null) {
            // 手动复制属性，避免使用过时的 BeanUtils
            this.setId(swiper.getId());
            this.setTitle(swiper.getTitle());
            this.setImageUrl(swiper.getImageUrl());
            this.setLinkUrl(swiper.getLinkUrl());
            this.setSort(swiper.getSort());
            this.setStatus(swiper.getStatus());
            this.setDescription(swiper.getDescription());
            this.setCreateTime(swiper.getCreateTime());
            this.setUpdateTime(swiper.getUpdateTime());
        }
    }
}
