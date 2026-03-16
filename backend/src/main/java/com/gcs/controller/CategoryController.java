package com.gcs.controller;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.stream.Collectors;

import jakarta.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.gcs.annotation.IgnoreAuth;
import com.gcs.dto.CategoryCreateDTO;
import com.gcs.dto.CategoryUpdateDTO;
import com.gcs.entity.Category;
import com.gcs.entity.view.CategoryView;
import com.gcs.service.CategoryService;
import com.gcs.utils.PageUtils;
import com.gcs.utils.R;
import com.gcs.utils.MPUtil;
import com.gcs.vo.CategoryVO;
import com.gcs.vo.CategoryDetailVO;
import com.gcs.vo.CategoryTreeVO;

import lombok.extern.slf4j.Slf4j;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

/**
 * 内容分类控制器
 * 提供分类相关的 RESTful API 接口
 * @author 
 * @email 
 * @date 2026-04-16 11:22:10
 */
@Slf4j
@Tag(name = "分类管理", description = "内容分类相关的 RESTful API 接口")
@RestController
@RequestMapping("/categories")
public class CategoryController {
    
    @Autowired
    private CategoryService categoryService;

    /**
     * 获取分类分页列表
     */
    @Operation(summary = "获取分类分页列表", description = "分页查询分类列表，支持条件筛选")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "获取成功"),
        @ApiResponse(responseCode = "500", description = "获取失败")
    })
    @GetMapping
    public R getPage(
        @Parameter(description = "查询参数") @RequestParam(required = false) Map<String, Object> params, 
        @Parameter(description = "分类查询条件") Category category) {
        try {
            // 如果 params 为 null 或为空，初始化为包含默认分页参数的 Map
            if (params == null || params.isEmpty()) {
                params = new HashMap<>();
                params.put("page", 1);
                params.put("limit", 10);
            }
            
            QueryWrapper<Category> queryWrapper = new QueryWrapper<>();
            PageUtils page = categoryService.queryPage(params, 
                MPUtil.sort(MPUtil.between(MPUtil.likeOrEq(queryWrapper, category), params), params));
            
            // 将 Category 转换为 CategoryVO
            List<CategoryVO> voList = ((List<Category>) page.getList())
                .stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
            page.setList(voList);
            
            return R.ok().put("data", page);
        } catch (Exception e) {
            log.error("获取分类分页列表失败", e);
            return R.error("获取数据失败");
        }
    }
    
    /**
     * 获取分类详情
     */
    @Operation(summary = "获取分类详情", description = "根据分类 ID 获取详细信息")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "获取成功"),
        @ApiResponse(responseCode = "404", description = "分类不存在"),
        @ApiResponse(responseCode = "500", description = "获取失败")
    })
    @GetMapping("/{id}")
    public R getCategoryInfo(
        @Parameter(description = "分类 ID", required = true) @PathVariable("id") Long id) {
        try {
            Category category = categoryService.getById(id);
            if (category == null) {
                return R.error("分类不存在");
            }
            
            // 将 Category 转换为 CategoryDetailVO
            CategoryDetailVO vo = convertToDetailVO(category);
            return R.ok().put("data", vo);
        } catch (Exception e) {
            log.error("获取分类详情失败，ID: {}", id, e);
            return R.error("获取详情失败");
        }
    }
    
    /**
     * 获取所有启用的分类
     */
    @Operation(summary = "获取所有启用的分类", description = "获取所有启用状态的分类列表")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "获取成功"),
        @ApiResponse(responseCode = "500", description = "获取失败")
    })
    @IgnoreAuth
    @GetMapping("/enabled")
    public R getEnabledCategories() {
        try {
            List<Category> categories = categoryService.getAllEnabledCategories();
            
            // 将 Category 转换为 CategoryVO
            List<CategoryVO> voList = categories.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
            
            return R.ok().put("data", voList);
        } catch (Exception e) {
            log.error("获取启用分类失败", e);
            return R.error("获取数据失败");
        }
    }

    /**
     * 获取分类树结构
     */
    @Operation(summary = "获取分类树结构", description = "获取分类的树形层级结构")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "获取成功"),
        @ApiResponse(responseCode = "500", description = "获取失败")
    })
    @GetMapping("/tree")
    public R getCategoryTree() {
        try {
            List<CategoryView> categoryTree = categoryService.getCategoryTree();
            
            // 将 CategoryView 转换为 CategoryTreeVO
            List<CategoryTreeVO> treeVOList = categoryTree.stream()
                .map(this::convertToTreeVO)
                .collect(Collectors.toList());
            
            return R.ok().put("data", treeVOList);
        } catch (Exception e) {
            log.error("获取分类树失败", e);
            return R.error("获取数据失败");
        }
    }

    /**
     * 创建分类
     */
    @Operation(summary = "创建分类", description = "新增分类信息")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "创建成功"),
        @ApiResponse(responseCode = "400", description = "创建失败")
    })
    @PostMapping
    public R createCategory(
        @Parameter(description = "分类信息", required = true) @Valid @RequestBody CategoryCreateDTO createDTO) {
        try {
            Category category = convertToEntity(createDTO);
            boolean result = categoryService.createCategory(category);
            if (result) {
                return R.ok("分类创建成功");
            } else {
                return R.error("创建失败");
            }
        } catch (Exception e) {
            log.error("保存分类失败，名称：{}", createDTO.getCategoryName(), e);
            return R.error(e.getMessage());
        }
    }

    /**
     * 更新分类信息
     */
    @Operation(summary = "更新分类信息", description = "根据分类 ID 更新分类信息")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "更新成功"),
        @ApiResponse(responseCode = "400", description = "更新失败")
    })
    @PutMapping("/{id}")
    @Transactional
    public R updateCategory(
        @Parameter(description = "分类 ID", required = true) @PathVariable("id") Long id, 
        @Parameter(description = "分类信息", required = true) @Valid @RequestBody CategoryUpdateDTO updateDTO) {
        try {
            Category category = categoryService.getById(id);
            if (category == null) {
                return R.error("分类不存在");
            }
            
            // 使用转换器更新实体
            convertToUpdateEntity(updateDTO, category);
            boolean result = categoryService.updateCategory(category);
            if (result) {
                return R.ok("更新成功");
            } else {
                return R.error("更新失败");
            }
        } catch (Exception e) {
            log.error("修改分类失败，ID: {}", id, e);
            return R.error(e.getMessage());
        }
    }

    /**
     * 部分更新分类
     */
    @Operation(summary = "部分更新分类", description = "根据分类 ID 部分更新分类信息")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "更新成功"),
        @ApiResponse(responseCode = "400", description = "更新失败")
    })
    @PatchMapping("/{id}")
    @Transactional
    public R partialUpdateCategory(
        @Parameter(description = "分类 ID", required = true) @PathVariable("id") Long id, 
        @Parameter(description = "分类信息") @RequestBody CategoryUpdateDTO updateDTO) {
        try {
            Category category = categoryService.getById(id);
            if (category == null) {
                return R.error("分类不存在");
            }
            
            // 使用转换器更新实体
            convertToUpdateEntity(updateDTO, category);
            boolean result = categoryService.updateCategory(category);
            if (result) {
                return R.ok("更新成功");
            } else {
                return R.error("更新失败");
            }
        } catch (Exception e) {
            log.error("部分更新分类失败，ID: {}", id, e);
            return R.error(e.getMessage());
        }
    }

    /**
     * 启用/禁用分类
     */
    @Operation(summary = "启用/禁用分类", description = "设置分类的启用/禁用状态")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "更新成功"),
        @ApiResponse(responseCode = "400", description = "更新失败")
    })
    @PatchMapping("/{id}/status")
    public R updateStatus(
        @Parameter(description = "分类 ID", required = true) @PathVariable("id") Long categoryId,
        @Parameter(description = "状态 (0:启用，1:禁用)", required = true) @RequestParam Integer status) {
        try {
            boolean result = categoryService.updateStatus(categoryId, status);
            if (result) {
                return R.ok("状态更新成功");
            } else {
                return R.error("状态更新失败");
            }
        } catch (Exception e) {
            log.error("更新分类状态失败，ID: {}", categoryId, e);
            return R.error(e.getMessage());
        }
    }

    /**
     * 删除分类（单个）
     */
    @Operation(summary = "删除分类", description = "根据分类 ID 删除分类")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "删除成功"),
        @ApiResponse(responseCode = "500", description = "删除失败")
    })
    @DeleteMapping("/{id}")
    public R deleteCategory(
        @Parameter(description = "分类 ID", required = true) @PathVariable("id") Long id) {
        try {
            categoryService.removeById(id);
            return R.ok("删除成功");
        } catch (Exception e) {
            log.error("删除分类失败，ID: {}", id, e);
            return R.error("删除失败");
        }
    }

    /**
     * 批量删除分类
     */
    @Operation(summary = "批量删除分类", description = "批量删除多个分类")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "删除成功"),
        @ApiResponse(responseCode = "400", description = "请选择要删除的分类"),
        @ApiResponse(responseCode = "500", description = "删除失败")
    })
    @PostMapping("/batch-delete")
    public R batchDeleteCategories(
        @Parameter(description = "分类 ID 数组", required = true) @RequestBody Long[] ids) {
        try {
            if (ids == null || ids.length == 0) {
                return R.error("请选择要删除的分类");
            }
            
            boolean result = categoryService.removeByIds(Arrays.asList(ids));
            if (result) {
                return R.ok("删除成功");
            } else {
                return R.error("删除失败");
            }
        } catch (Exception e) {
            log.error("批量删除分类失败", e);
            return R.error("删除失败");
        }
    }
    
    // ==================== 私有转换方法 ====================
    
    /**
     * 将 Category 转换为 CategoryVO
     */
    private CategoryVO convertToVO(Category category) {
        CategoryVO vo = new CategoryVO();
        vo.setId(category.getId());
        vo.setCategoryName(category.getCategoryName());
        vo.setDescription(category.getDescription());
        vo.setSort(category.getSort());
        vo.setStatus(category.getStatus());
        vo.setCreateTime(category.getCreateTime());
        return vo;
    }
    
    /**
     * 将 Category 转换为 CategoryDetailVO
     */
    private CategoryDetailVO convertToDetailVO(Category category) {
        CategoryDetailVO vo = new CategoryDetailVO();
        vo.setId(category.getId());
        vo.setCategoryName(category.getCategoryName());
        vo.setDescription(category.getDescription());
        vo.setSort(category.getSort());
        vo.setStatus(category.getStatus());
        vo.setCreateTime(category.getCreateTime());
        vo.setUpdateTime(category.getUpdateTime());
        return vo;
    }
    
    /**
     * 将 CategoryCreateDTO 转换为 Category
     */
    private Category convertToEntity(CategoryCreateDTO dto) {
        Category category = new Category();
        category.setCategoryName(dto.getCategoryName());
        category.setDescription(dto.getDescription());
        category.setSort(dto.getSort());
        return category;
    }
    
    /**
     * 将 CategoryUpdateDTO 转换为 Category（更新）
     */
    private void convertToUpdateEntity(CategoryUpdateDTO dto, Category category) {
        category.setCategoryName(dto.getCategoryName());
        category.setDescription(dto.getDescription());
        category.setSort(dto.getSort());
        if (dto.getStatus() != null) {
            category.setStatus(dto.getStatus());
        }
    }
    
    /**
     * 将 CategoryView 转换为 CategoryTreeVO
     */
    private CategoryTreeVO convertToTreeVO(CategoryView view) {
        CategoryTreeVO vo = new CategoryTreeVO();
        vo.setId(view.getId());
        vo.setCategoryName(view.getCategoryName());
        vo.setDescription(view.getDescription());
        vo.setSort(view.getSort());
        vo.setStatus(view.getStatus());
        // 如果有子分类，递归转换
//        if (view.getChildren() != null) {
//            vo.setChildren(view.getChildren().stream()
//                .map(this::convertToTreeVO)
//                .collect(Collectors.toList()));
//        }
        return vo;
    }
}
