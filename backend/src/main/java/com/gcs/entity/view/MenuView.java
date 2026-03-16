package com.gcs.entity.view;

import com.baomidou.mybatisplus.annotation.TableName;
import com.gcs.entity.Menu;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * 菜单视图实体类
 * 后端返回视图实体辅助类（通常后端关联的表或者自定义的字段需要返回使用）
 * @author 
 * @email 
 * @date 2026-04-16 11:22:10
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TableName("menu")
public class MenuView extends Menu implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 扩展字段：子菜单列表
     */
    private List<MenuView> children;

    /**
     * 构造函数
     * @param menu 原始菜单对象
     */
    public MenuView(Menu menu) {
        if (menu != null) {
            // 手动复制属性，避免使用过时的 BeanUtils
            this.setId(menu.getId());
            this.setName(menu.getName());
            this.setComponent(menu.getComponent());
            this.setParentId(menu.getParentId());
            this.setIcon(menu.getIcon());
            this.setPath(menu.getPath());
            this.setSort(menu.getSort());
            this.setStatus(menu.getStatus());
            this.setCreateTime(menu.getCreateTime());
            this.setUpdateTime(menu.getUpdateTime());
            this.setRemark(menu.getRemark());
        }
    }
}
