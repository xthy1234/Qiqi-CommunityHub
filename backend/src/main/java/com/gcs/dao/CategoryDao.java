package com.gcs.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.gcs.entity.Category;

import java.util.List;

import com.baomidou.mybatisplus.core.conditions.Wrapper;

import org.apache.ibatis.annotations.Param;
import com.gcs.entity.view.CategoryView;


/**
 * 内容分类
 * 
 * @author 
 * @email 
 * @date 2026-04-16 11:22:10
 */
public interface CategoryDao extends BaseMapper<Category> {
	
	List<CategoryView> selectListView(@Param("ew") Wrapper<Category> wrapper);

	List<CategoryView> selectListView(IPage<CategoryView> page, @Param("ew") Wrapper<Category> wrapper);
	
	CategoryView selectView(@Param("ew") Wrapper<Category> wrapper);
	

}
