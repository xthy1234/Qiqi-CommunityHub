package com.gcs.entity.view;

import com.baomidou.mybatisplus.annotation.TableName;
import com.gcs.entity.Article;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 帖子视图实体类
 * 后端返回视图实体辅助类（通常后端关联的表或者自定义的字段需要返回使用）
 * @author 
 * @email 
 * @date 2026-04-16 11:22:09
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("article")
public class ArticleView extends Article implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 扩展字段：分类名称
     */
  private String categoryName;

    /**
     * 扩展字段：作者头像
     */
  private String authorAvatar;

    /**
     * 扩展字段：作者昵称
     */
  private String authorNickname;

    /**
     * 构造函数
     * @param article 原始文章对象
     */
  public ArticleView(Article article) {
       if (article != null) {
           // 使用现代的属性复制方式替代过时的 BeanUtils
           // 这里可以根据需要选择合适的映射工具
           this.setId(article.getId());
           this.setTitle(article.getTitle());
           this.setCoverUrl(article.getCoverUrl());
           this.setAttachment(article.getAttachment());
           this.setCategoryId(article.getCategoryId());
           this.setContent(article.getContent());
           this.setPublishTime(article.getPublishTime());
           this.setAuthorId(article.getAuthorId());
           this.setLikeCount(article.getLikeCount());
           this.setDislikeCount(article.getDislikeCount());
           this.setFavoriteCount(article.getFavoriteCount());
           this.setShareCount(article.getShareCount());
           this.setAuditStatus(article.getAuditStatus());
           this.setAuditReply(article.getAuditReply());
           this.setViewCount(article.getViewCount());
           this.setCreateTime(article.getCreateTime());
           this.setUpdateTime(article.getUpdateTime());
       }
   }
}
