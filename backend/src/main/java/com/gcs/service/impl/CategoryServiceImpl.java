package com.gcs.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.stereotype.Service;
import java.util.Map;
import java.util.List;
import java.util.Objects;
import java.time.LocalDateTime;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.gcs.utils.PageUtils;
import com.gcs.utils.Query;

import com.gcs.dao.CategoryDao;
import com.gcs.entity.Category;
import com.gcs.service.CategoryService;
import com.gcs.entity.view.CategoryView;

import org.springframework.util.StringUtils;

import lombok.extern.slf4j.Slf4j;

import com.gcs.enums.CategoryStatus;

/**
 * 内容分类服务实现类
 * 提供分类相关的业务逻辑处理
 * @author 
 * @date 2026-04-16
 */
@Slf4j
@Service("categoryService")
public class CategoryServiceImpl extends ServiceImpl<CategoryDao, Category> implements CategoryService {

    /**
     * 分页查询分类列表
     *
     * @param params 查询参数
     * @return 分页结果
     */
    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        validateParams(params);
        
        IPage<Category> categoryPage = new Query<Category>(params).getPage();
        IPage<Category> resultPage = this.page(categoryPage, new QueryWrapper<>());
        
        return new PageUtils(resultPage);
    }

    /**
     * 带条件的分页查询分类列表
     *
     * @param params 查询参数
     * @param queryWrapper 查询条件包装器
     * @return 分页结果
     */
    @Override
    public PageUtils queryPage(Map<String, Object> params, Wrapper<Category> queryWrapper) {
        validateQueryParams(params, queryWrapper);
        
        IPage<CategoryView> categoryViewPage =(Page<CategoryView>) new Query<CategoryView>(params).getPage();
        List<CategoryView> categoryViews = baseMapper.selectListView(categoryViewPage, queryWrapper);
        categoryViewPage.setRecords(categoryViews);
        
        return new PageUtils(categoryViewPage);
    }

    /**
     * 查询分类列表视图
     *
     * @param queryWrapper 查询条件包装器
     * @return 分类视图列表
     */
    @Override
    public List<CategoryView> selectListView(Wrapper<Category> queryWrapper) {
        validateWrapper(queryWrapper);
        return baseMapper.selectListView(queryWrapper);
    }

    /**
     * 查询单个分类视图
     *
     * @param queryWrapper 查询条件包装器
     * @return 分类视图
     */
    @Override
    public CategoryView selectView(Wrapper<Category> queryWrapper) {
        validateWrapper(queryWrapper);
        return baseMapper.selectView(queryWrapper);
    }

    /**
     * 根据分类名称查询
     *
     * @param categoryName 分类名称
     * @return 分类信息
     */
    @Override
    public Category selectByCategoryName(String categoryName) {
        if (!StringUtils.hasText(categoryName)) {
            throw new IllegalArgumentException("分类名称不能为空");
        }
        return this.getOne(new QueryWrapper<Category>().eq("category_name", categoryName));
    }

    /**
     * 检查分类名称是否存在
     *
     * @param categoryName 分类名称
     * @return 是否存在
     */
    @Override
    public boolean existsByCategoryName(String categoryName) {
        if (!StringUtils.hasText(categoryName)) {
            return false;
        }
        return this.count(new QueryWrapper<Category>().eq("category_name", categoryName)) > 0;
    }

    /**
     * 创建分类
     *
     * @param category 分类信息
     * @return 创建结果
     */
    @Override
    public boolean createCategory(Category category) {
        validateCategoryForCreate(category);
        
        if (existsByCategoryName(category.getCategoryName())) {
            throw new RuntimeException("分类名称已存在");
        }

        // 设置默认值
        category.setCreateTime(LocalDateTime.now());
        category.setStatus(CategoryStatus.ENABLED); // 默认启用
        if (category.getSort() == null) {
            category.setSort(0);
        }
        
        return this.save(category);
    }

    /**
     * 更新分类
     *
     * @param category 分类信息
     * @return 更新结果
     */
    @Override
    public boolean updateCategory(Category category) {
        validateCategoryForUpdate(category);
        
        // 检查分类名称是否被其他分类占用
        Category existingCategory = selectByCategoryName(category.getCategoryName());
        if (existingCategory != null && !existingCategory.getId().equals(category.getId())) {
            throw new RuntimeException("分类名称已存在");
        }

        category.setUpdateTime(LocalDateTime.now());
        return this.updateById(category);
    }

    /**
     * 启用/禁用分类
     *
     * @param categoryId 分类ID
     * @param status 状态（0:禁用 1:启用）
     * @return 操作结果
     */
    @Override
    public boolean updateStatus(Long categoryId, Integer status) {
        if (categoryId == null || status == null) {
            throw new IllegalArgumentException("参数不能为空");
        }

        Category category = this.getById(categoryId);
        if (category == null) {
            throw new RuntimeException("分类不存在");
        }
        CategoryStatus categoryStatus = CategoryStatus.valueOf(status);
        category.setStatus(categoryStatus);
        category.setUpdateTime(LocalDateTime.now());
        
        return this.updateById(category);
    }

    /**
     * 获取所有启用的分类
     *
     * @return 分类列表
     */
    @Override
    public List<Category> getAllEnabledCategories() {
        return this.list(new QueryWrapper<Category>()
            .eq("status", CategoryStatus.ENABLED.getCode()) // 查询启用状态的分类
            .orderByAsc("sort"));
    }

    /**
     * 获取分类树结构
     *
     * @return 分类树
     */
    @Override
    public List<CategoryView> getCategoryTree() {
        // 这里可以实现树形结构的构建逻辑
        // 目前简单返回扁平化的列表
        List<Category> categories = getAllEnabledCategories();
        return categories.stream()
            .map(CategoryView::new)
            .collect(Collectors.toList());
    }

    // ==================== 私有验证方法 ====================

    /**
     * 验证查询参数
     */
    private void validateParams(Map<String, Object> params) {
		if (params == null || params.isEmpty()) {
            throw new IllegalArgumentException("查询参数不能为空");
        }
    }

    /**
     * 验证查询条件包装器
     */
    private void validateWrapper(Wrapper<Category> queryWrapper) {
        if (Objects.isNull(queryWrapper)) {
            throw new IllegalArgumentException("查询条件不能为空");
        }
    }

    /**
     * 验证查询参数和条件
     */
    private void validateQueryParams(Map<String, Object> params, Wrapper<Category> queryWrapper) {
        validateParams(params);
        validateWrapper(queryWrapper);
    }

    /**
     * 验证创建分类参数
     */
    private void validateCategoryForCreate(Category category) {
        if (category == null) {
            throw new IllegalArgumentException("分类信息不能为空");
        }
        if (!StringUtils.hasText(category.getCategoryName())) {
            throw new IllegalArgumentException("分类名称不能为空");
        }
    }

    /**
     * 验证更新分类参数
     */
    private void validateCategoryForUpdate(Category category) {
        if (category == null || category.getId() == null) {
            throw new IllegalArgumentException("分类信息不完整");
        }
        if (!StringUtils.hasText(category.getCategoryName())) {
            throw new IllegalArgumentException("分类名称不能为空");
        }
    }
}
