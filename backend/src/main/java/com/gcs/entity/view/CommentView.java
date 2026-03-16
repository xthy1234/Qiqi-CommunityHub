package com.gcs.entity.view;

import com.baomidou.mybatisplus.annotation.TableName;
import com.gcs.entity.Comment;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * 评论视图实体类
 * 后端返回视图实体辅助类（通常后端关联的表或者自定义的字段需要返回使用）
 * @author 
 * @email 
 * @date 2026-04-16 11:22:10
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("comment")
public class CommentView extends Comment implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 扩展字段：子评论列表
     */
    private List<CommentView> children;

    /**
     * 扩展字段：关联内容标题
     */
    private String contentTitle;

    /**
     * 扩展字段：是否已点赞（针对当前用户）
     */
    private Boolean isLiked = false;

    /**
     * 扩展字段：评论层级（0 表示一级评论）
     */
    private Integer level;

    /**
     * 构造函数
     * @param comment 原始评论对象
     */
    public CommentView(Comment comment) {
        if (comment != null) {
            // 手动复制属性，避免使用过时的BeanUtils
            this.setId(comment.getId());
            this.setContentId(comment.getContentId());
            this.setUserId(comment.getUserId());
            this.setUserAvatar(comment.getUserAvatar());
            this.setUserNickname(comment.getUserNickname());
            this.setContent(comment.getContent());
            this.setReplyContent(comment.getReplyContent());
            this.setParentId(comment.getParentId());
            this.setLikeCount(comment.getLikeCount());
            this.setStatus(comment.getStatus());
            this.setCreateTime(comment.getCreateTime());
            this.setUpdateTime(comment.getUpdateTime());
        }
    }
}
