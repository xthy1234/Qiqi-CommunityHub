package com.gcs.entity.view;

import com.baomidou.mybatisplus.annotation.TableName;
import com.gcs.entity.Circle;
import com.gcs.enums.CommonStatus;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 圈子视图实体类
 * 后端返回视图实体辅助类（通常后端关联的表或者自定义的字段需要返回使用）
 * @author 
 * @email 
 * @date 2026-03-17
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("circle")
public class CircleView extends Circle implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 扩展字段：圈子类型描述
     */
    private String typeDescription;

    /**
     * 扩展字段：圈子状态描述
     */
    private String statusDescription;

    /**
     * 扩展字段：成员数量
     */
    private Integer memberCount;

    /**
     * 扩展字段：圈主昵称
     */
    private String ownerNickname;

    /**
     * 构造函数
     * @param circle 原始圈子对象
     */
    public CircleView(Circle circle) {
        if (circle != null) {
            // 手动复制属性
            this.setId(circle.getId());
            this.setName(circle.getName());
            this.setDescription(circle.getDescription());
            this.setAvatar(circle.getAvatar());
            this.setOwnerId(circle.getOwnerId());
            this.setType(circle.getType());
            this.setStatus(circle.getStatus());
            this.setCreateTime(circle.getCreateTime());
            this.setUpdateTime(circle.getUpdateTime());
            
            // 设置扩展字段
            this.typeDescription = getTypeDescription(circle.getType());
            this.statusDescription = getStatusDescription(circle.getStatus());
        }
    }

    /**
     * 获取圈子类型描述
     */
    private String getTypeDescription(Integer type) {
        if (type == null) return "未知";
        switch (type) {
            case 0: return "私密圈子";
            case 1: return "公开圈子";
            case 2: return "个人空间";
            default: return "未知";
        }
    }

    /**
     * 获取状态描述
     */
    private String getStatusDescription(CommonStatus status) {
        if (status == null) return "未知";
        return status == CommonStatus.ENABLED ? "正常" : "解散";
    }
}
