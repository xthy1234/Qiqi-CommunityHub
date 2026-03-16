package com.gcs.entity.view;

import com.baomidou.mybatisplus.annotation.TableName;
import com.gcs.entity.Interaction;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 收藏视图实体类
 * 后端返回视图实体辅助类（通常后端关联的表或者自定义的字段需要返回使用）
 * @author 
 * @email 
 * @date 2026-04-16 11:22:09
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("favorite")
public class InteractionView extends Interaction implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 扩展字段：用户昵称
     */
    private String userNickname;

    /**
     * 扩展字段：用户头像
     */
    private String userAvatar;

    /**
     * 扩展字段：关联内容详情
     */
    private String contentDetail;

    /**
     * 构造函数
     * @param interaction 原始收藏对象
     */
    public InteractionView(Interaction interaction) {
        if (interaction != null) {
            // 手动复制属性，避免使用过时的BeanUtils
            this.setId(interaction.getId());
            this.setContentId(interaction.getContentId());
            this.setTableName(interaction.getTableName());
            this.setActionType(interaction.getActionType());
            this.setRecommendType(interaction.getRecommendType());
            this.setRemark(interaction.getRemark());
            this.setUserId(interaction.getUserId());
            this.setStatus(interaction.getStatus());
            this.setCreateTime(interaction.getCreateTime());
            this.setUpdateTime(interaction.getUpdateTime());
        }
    }
}
