package com.gcs.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.extension.service.IService;
import com.gcs.utils.PageUtils;
import com.gcs.entity.Category;
import com.gcs.entity.view.CategoryView;

import java.util.List;
import java.util.Map;

/**
 * 内容分类服务接口
 * 提供分类相关的业务操作
 * @author 
 * @date 2026-04-16
 */
public interface CategoryService extends IService<Category> {

    /**
     * 分页查询分类列表
     *
     * @param params 查询参数
     * @return 分页结果
     */
    PageUtils queryPage(Map<String, Object> params);

    /**
     * 查询分类列表视图
     *
     * @param queryWrapper 查询条件包装器
     * @return 分类视图列表
     */
    List<CategoryView> selectListView(Wrapper<Category> queryWrapper);

    /**
     * 查询单个分类视图
     *
     * @param queryWrapper 查询条件包装器
     * @return 分类视图
     */
    CategoryView selectView(Wrapper<Category> queryWrapper);

    /**
     * 带条件的分页查询分类列表
     *
     * @param params 查询参数
     * @param queryWrapper 查询条件包装器
     * @return 分页结果
     */
    PageUtils queryPage(Map<String, Object> params, Wrapper<Category> queryWrapper);

    /**
     * 根据分类名称查询
     *
     * @param categoryName 分类名称
     * @return 分类信息
     */
    Category selectByCategoryName(String categoryName);

    /**
     * 检查分类名称是否存在
     *
     * @param categoryName 分类名称
     * @return 是否存在
     */
    boolean existsByCategoryName(String categoryName);

    /**
     * 创建分类
     *
     * @param category 分类信息
     * @return 创建结果
     */
    boolean createCategory(Category category);

    /**
     * 更新分类
     *
     * @param category 分类信息
     * @return 更新结果
     */
    boolean updateCategory(Category category);

    /**
     * 启用/禁用分类
     *
     * @param categoryId 分类ID
     * @param status 状态（0:禁用 1:启用）
     * @return 操作结果
     */
    boolean updateStatus(Long categoryId, Integer status);

    /**
     * 获取所有启用的分类
     *
     * @return 分类列表
     */
    List<Category> getAllEnabledCategories();

    /**
     * 获取分类树结构
     *
     * @return 分类树
     */
    List<CategoryView> getCategoryTree();
}

