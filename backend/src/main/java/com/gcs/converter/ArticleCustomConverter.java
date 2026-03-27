package com.gcs.converter;

import com.gcs.entity.Article;
import com.gcs.entity.view.ArticleView;
import com.gcs.vo.ArticleDetailVO;
import com.gcs.vo.ArticleVO;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public abstract class ArticleCustomConverter {
    
    /**
     * 处理 publishTime 的默认值逻辑
     */
    @AfterMapping
    protected void afterMapping(Article article, @MappingTarget ArticleVO vo) {
        if (article.getPublishTime() == null && article.getCreateTime() != null) {
            vo.setPublishTime(java.util.Date.from(
                article.getCreateTime().atZone(java.time.ZoneId.systemDefault()).toInstant()
            ));
        }
    }
    
    /**
     * 处理 updateTime 的格式转换
     */
    @AfterMapping
    protected void afterMapping(Article article, @MappingTarget ArticleDetailVO vo) {
        if (article.getPublishTime() == null && article.getCreateTime() != null) {
            vo.setPublishTime(java.util.Date.from(
                article.getCreateTime().atZone(java.time.ZoneId.systemDefault()).toInstant()
            ));
        }
        
        if (article.getUpdateTime() != null) {
            vo.setUpdateTime(article.getUpdateTime().toString());
        }
    }
}
