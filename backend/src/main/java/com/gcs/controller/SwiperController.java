package com.gcs.controller;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.gcs.enums.CommonStatus;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.gcs.annotation.IgnoreAuth;
import com.gcs.dto.SwiperCreateDTO;
import com.gcs.dto.SwiperUpdateDTO;
import com.gcs.entity.Swiper;
import com.gcs.service.SwiperService;
import com.gcs.utils.MPUtil;
import com.gcs.utils.PageUtils;
import com.gcs.utils.R;
import com.gcs.vo.SwiperVO;
import com.gcs.vo.SwiperDetailVO;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;

/**
 * 轮播图控制器
 * 提供轮播图相关的 RESTful API 接口
 * @author 
 * @email 
 * @date 2026-04-16
 */
@Slf4j
@Tag(name = "轮播图管理", description = "轮播图 CRUD 操作接口")
@RestController
@RequestMapping("/swipers")
public class SwiperController {
    
    @Autowired
    private SwiperService swiperService;

    /**
     * 获取轮播图分页列表
     */
    @Operation(summary = "分页查询轮播图列表", description = "根据条件分页查询轮播图数据")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "获取成功"),
        @ApiResponse(responseCode = "500", description = "获取失败")
    })
    @GetMapping
    public R getPage(
        @Parameter(description = "查询参数") @RequestParam Map<String, Object> params, 
        @Parameter(description = "轮播图查询对象") Swiper swiper) {
        try {
            QueryWrapper<Swiper> queryWrapper = new QueryWrapper<>();
            PageUtils page = swiperService.queryPage(params, 
                MPUtil.sort(MPUtil.between(MPUtil.likeOrEq(queryWrapper, swiper), params), params));
            
            // 将 Swiper 转换为 SwiperVO
            List<SwiperVO> voList = ((List<Swiper>) page.getList())
                .stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
            page.setList(voList);
            
            return R.ok().put("data", page);
        } catch (Exception e) {
            log.error("获取轮播图分页列表失败", e);
            return R.error("获取数据失败");
        }
    }
    
    /**
     * 获取轮播图详情
     */
    @Operation(summary = "获取轮播图详情", description = "根据 ID 获取轮播图详细信息")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "获取成功"),
        @ApiResponse(responseCode = "404", description = "轮播图不存在"),
        @ApiResponse(responseCode = "500", description = "获取失败")
    })
    @GetMapping("/{id}")
    public R getSwiperInfo(
        @Parameter(description = "轮播图 ID", required = true) @PathVariable("id") Long id) {
        try {
            Swiper swiper = swiperService.getById(id);
            if (swiper == null) {
                return R.error("轮播图不存在");
            }
            
            // 将 Swiper 转换为 SwiperDetailVO
            SwiperDetailVO vo = convertToDetailVO(swiper);
            return R.ok().put("data", vo);
        } catch (Exception e) {
            log.error("获取轮播图详情失败，ID: {}", id, e);
            return R.error("获取详情失败");
        }
    }

    /**
     * 获取所有启用的轮播图（无需认证）
     */
    @Operation(summary = "获取所有启用的轮播图", description = "查询所有状态为显示的轮播图，按排序字段升序排列")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "获取成功"),
        @ApiResponse(responseCode = "500", description = "获取失败")
    })
    @IgnoreAuth
    @GetMapping("/enabled")
    public R getEnabledSwipers() {
        try {
            List<Swiper> swipers = swiperService.getAllEnabledSwipers();
            
            // 将 Swiper 转换为 SwiperVO
            List<SwiperVO> voList = swipers.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
            
            return R.ok().put("data", voList);
        } catch (Exception e) {
            log.error("获取启用的轮播图失败", e);
            return R.error("获取数据失败");
        }
    }

    /**
     * 创建轮播图
     */
    @Operation(summary = "创建轮播图", description = "新增轮播图信息")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "保存成功"),
        @ApiResponse(responseCode = "400", description = "保存失败")
    })
    @PostMapping
    public R saveSwiper(
        @Parameter(description = "轮播图信息", required = true) @Valid @RequestBody SwiperCreateDTO createDTO) {
        try {
            Swiper swiper = convertToEntity(createDTO);
            
            // 默认状态为显示
            if (swiper.getStatus() == null) {
                swiper.setStatus(CommonStatus.ENABLED);
            }
            
            boolean result = swiperService.save(swiper);
            if (result) {
                return R.ok("轮播图保存成功");
            } else {
                return R.error("保存失败");
            }
        } catch (Exception e) {
            log.error("保存轮播图失败", e);
            return R.error(e.getMessage());
        }
    }

    /**
     * 更新轮播图
     */
    @Operation(summary = "更新轮播图", description = "根据 ID 更新轮播图信息")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "更新成功"),
        @ApiResponse(responseCode = "400", description = "更新失败")
    })
    @PutMapping("/{id}")
    public R updateSwiper(
        @Parameter(description = "轮播图 ID", required = true) @PathVariable("id") Long id, 
        @Parameter(description = "轮播图信息", required = true) @Valid @RequestBody SwiperUpdateDTO updateDTO) {
        try {
            Swiper swiper = swiperService.getById(id);
            if (swiper == null) {
                return R.error("轮播图不存在");
            }
            
            // 使用转换器更新实体
            convertToUpdateEntity(updateDTO, swiper);
            boolean result = swiperService.updateById(swiper);
            if (result) {
                return R.ok("轮播图更新成功");
            } else {
                return R.error("更新失败");
            }
        } catch (Exception e) {
            log.error("修改轮播图失败，ID: {}", id, e);
            return R.error(e.getMessage());
        }
    }

    /**
     * 部分更新轮播图
     */
    @Operation(summary = "部分更新轮播图", description = "根据 ID 部分更新轮播图信息")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "更新成功"),
        @ApiResponse(responseCode = "400", description = "更新失败")
    })
    @PatchMapping("/{id}")
    public R partialUpdateSwiper(
        @Parameter(description = "轮播图 ID", required = true) @PathVariable("id") Long id, 
        @Parameter(description = "轮播图信息", required = true) @Valid @RequestBody SwiperUpdateDTO updateDTO) {
        try {
            Swiper swiper = swiperService.getById(id);
            if (swiper == null) {
                return R.error("轮播图不存在");
            }
            
            // 使用转换器更新实体
            convertToUpdateEntity(updateDTO, swiper);
            boolean result = swiperService.updateById(swiper);
            if (result) {
                return R.ok("轮播图更新成功");
            } else {
                return R.error("更新失败");
            }
        } catch (Exception e) {
            log.error("部分更新轮播图失败，ID: {}", id, e);
            return R.error(e.getMessage());
        }
    }

    /**
     * 启用/禁用轮播图
     */
    @Operation(summary = "更新轮播图状态", description = "启用或禁用指定 ID 的轮播图")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "状态更新成功"),
        @ApiResponse(responseCode = "400", description = "状态更新失败")
    })
    @PatchMapping("/{id}/status")
    public R updateStatus(
        @Parameter(description = "轮播图 ID", required = true) @PathVariable("id") Long swiperId,
        @Parameter(description = "状态 (0:隐藏 1:显示)", required = true) @RequestParam Integer status) {
        try {
            boolean result = swiperService.updateStatus(swiperId, status);
            if (result) {
                return R.ok("状态更新成功");
            } else {
                return R.error("状态更新失败");
            }
        } catch (Exception e) {
            log.error("更新轮播图状态失败，ID: {}", swiperId, e);
            return R.error(e.getMessage());
        }
    }

    /**
     * 删除轮播图（单个）
     */
    @Operation(summary = "删除轮播图", description = "根据 ID 删除单个轮播图")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "删除成功"),
        @ApiResponse(responseCode = "500", description = "删除失败")
    })
    @DeleteMapping("/{id}")
    public R deleteSwiper(
        @Parameter(description = "轮播图 ID", required = true) @PathVariable("id") Long id) {
        try {
            swiperService.removeById(id);
            return R.ok("删除成功");
        } catch (Exception e) {
            log.error("删除轮播图失败，ID: {}", id, e);
            return R.error("删除失败");
        }
    }

    /**
     * 批量删除轮播图
     */
    @Operation(summary = "批量删除轮播图", description = "批量删除指定 ID 的轮播图")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "删除成功"),
        @ApiResponse(responseCode = "400", description = "请选择要删除的轮播图"),
        @ApiResponse(responseCode = "500", description = "删除失败")
    })
    @PostMapping("/batch-delete")
    public R batchDeleteSwipers(
        @Parameter(description = "轮播图 ID 数组", required = true) @RequestBody Long[] ids) {
        try {
            if (ids == null || ids.length == 0) {
                return R.error("请选择要删除的轮播图");
            }
            
            List<Long> swiperIds = Arrays.stream(ids).collect(Collectors.toList());
            boolean result = swiperService.deleteSwipers(swiperIds);
            if (result) {
                return R.ok("删除成功");
            } else {
                return R.error("删除失败");
            }
        } catch (Exception e) {
            log.error("删除轮播图失败", e);
            return R.error("删除失败");
        }
    }

    /**
     * 获取轮播图总数
     */
    @Operation(summary = "获取轮播图总数", description = "根据条件查询轮播图总数量")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "查询成功"),
        @ApiResponse(responseCode = "500", description = "查询失败")
    })
    @GetMapping("/count")
    public R getCount(
        @Parameter(description = "查询参数") @RequestParam Map<String, Object> params, 
        @Parameter(description = "轮播图查询对象") Swiper swiper) {
        try {
            QueryWrapper<Swiper> queryWrapper = new QueryWrapper<>();
            Long count = swiperService.count(MPUtil.sort(
                MPUtil.between(MPUtil.likeOrEq(queryWrapper, swiper), params), params));
                
            return R.ok().put("data", count);
        } catch (Exception e) {
            log.error("获取轮播图总数失败", e);
            return R.error("查询失败");
        }
    }

    // ==================== 私有转换方法 ====================
    
    /**
     * 将 Swiper 转换为 SwiperVO
     */
    private SwiperVO convertToVO(Swiper swiper) {
        SwiperVO vo = new SwiperVO();
        vo.setId(swiper.getId());
        vo.setTitle(swiper.getTitle());
        vo.setImageUrl(swiper.getImageUrl());
        vo.setLinkUrl(swiper.getLinkUrl());
        vo.setSort(swiper.getSort());
        vo.setStatus(swiper.getStatus());
        vo.setDescription(swiper.getDescription());
        vo.setCreateTime(swiper.getCreateTime());
        return vo;
    }
    
    /**
     * 将 Swiper 转换为 SwiperDetailVO
     */
    private SwiperDetailVO convertToDetailVO(Swiper swiper) {
        SwiperDetailVO vo = new SwiperDetailVO();
        vo.setId(swiper.getId());
        vo.setTitle(swiper.getTitle());
        vo.setImageUrl(swiper.getImageUrl());
        vo.setLinkUrl(swiper.getLinkUrl());
        vo.setSort(swiper.getSort());
        vo.setStatus(swiper.getStatus());
        vo.setDescription(swiper.getDescription());
        vo.setCreateTime(swiper.getCreateTime());
        vo.setUpdateTime(swiper.getUpdateTime());
        return vo;
    }
    
    /**
     * 将 SwiperCreateDTO 转换为 Swiper
     */
    private Swiper convertToEntity(SwiperCreateDTO dto) {
        Swiper swiper = new Swiper();
        swiper.setTitle(dto.getTitle());
        swiper.setImageUrl(dto.getImageUrl());
        swiper.setLinkUrl(dto.getLinkUrl());
        swiper.setSort(dto.getSort());
        swiper.setStatus(dto.getStatus() != null ? dto.getStatus() : CommonStatus.ENABLED);
        swiper.setDescription(dto.getDescription());
        return swiper;
    }
    
    /**
     * 将 SwiperUpdateDTO 转换为 Swiper（更新）
     */
    private void convertToUpdateEntity(SwiperUpdateDTO dto, Swiper swiper) {
        swiper.setTitle(dto.getTitle());
        swiper.setImageUrl(dto.getImageUrl());
        swiper.setLinkUrl(dto.getLinkUrl());
        swiper.setSort(dto.getSort());
        swiper.setStatus(dto.getStatus());
        swiper.setDescription(dto.getDescription());
    }
}
