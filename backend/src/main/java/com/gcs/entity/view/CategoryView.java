package com.gcs.entity.view;

import com.baomidou.mybatisplus.annotation.TableName;
import com.gcs.entity.Category;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 内容分类视图实体类
 * 后端返回视图实体辅助类（通常后端关联的表或者自定义的字段需要返回使用）
 * @author 
 * @email 
 * @date 2026-04-16 11:22:10
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("category")
public class CategoryView extends Category implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 扩展字段：文章数量统计
     */
    private Integer articleCount;

    /**
     * 扩展字段：父级分类名称
     */
    private String parentCategoryName;

    /**
     * 构造函数
     * @param category 原始分类对象
     */
    public CategoryView(Category category) {
        if (category != null) {
            // 手动复制属性，避免使用过时的BeanUtils
            this.setId(category.getId());
            this.setCategoryName(category.getCategoryName());
            this.setDescription(category.getDescription());
            this.setSort(category.getSort());
            this.setStatus(category.getStatus());
            this.setCreateTime(category.getCreateTime());
            this.setUpdateTime(category.getUpdateTime());
        }
    }
}
