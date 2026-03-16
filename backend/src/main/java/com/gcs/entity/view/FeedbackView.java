package com.gcs.entity.view;

import com.baomidou.mybatisplus.annotation.TableName;
import com.gcs.entity.Feedback;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 用户反馈视图实体类
 * 后端返回视图实体辅助类（通常后端关联的表或者自定义的字段需要返回使用）
 * @author 
 * @email 
 * @date 2026-04-16 11:22:10
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("feedback")
public class FeedbackView extends Feedback implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 扩展字段：处理人姓名
     */
    private String handlerName;

    /**
     * 扩展字段：处理人账号
     */
    private String handlerAccount;

    /**
     * 扩展字段：状态描述
     */
    private String statusDescription;

    /**
     * 构造函数
     * @param feedback 原始反馈对象
     */
    public FeedbackView(Feedback feedback) {
        if (feedback != null) {
            // 手动复制属性，避免使用过时的BeanUtils
            this.setId(feedback.getId());
            this.setUserId(feedback.getUserId());
            this.setUserNickname(feedback.getUserNickname());
            this.setContent(feedback.getContent());
            this.setReplyContent(feedback.getReplyContent());
            this.setReplyTime(feedback.getReplyTime());
            this.setStatus(feedback.getStatus());
            this.setCreateTime(feedback.getCreateTime());
            this.setUpdateTime(feedback.getUpdateTime());
        }
    }
}
