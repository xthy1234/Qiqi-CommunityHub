package com.gcs.controller;

import java.util.Arrays;
import java.util.Map;
import java.util.List;
import java.util.stream.Collectors;

import com.gcs.entity.Interaction;
import com.gcs.enums.ContentType;
import com.gcs.enums.InteractionActionType;
import com.gcs.enums.InteractionStatus;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.gcs.dto.InteractionCreateDTO;
import com.gcs.dto.InteractionUpdateDTO;
import com.gcs.dto.InteractionLikeDTO;
import com.gcs.dto.InteractionCancelDTO;
import com.gcs.dto.InteractionCheckDTO;
import com.gcs.service.InteractionService;
import com.gcs.utils.PageUtils;
import com.gcs.utils.R;
import com.gcs.utils.MPUtil;
import com.gcs.vo.InteractionVO;
import com.gcs.vo.InteractionDetailVO;
import com.gcs.vo.InteractionStatisticsVO;
import com.gcs.vo.InteractionUserActionVO;

import lombok.extern.slf4j.Slf4j;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

/**
 * 互动记录控制器
 * 提供互动相关的 RESTful API 接口
 * @author 
 * @email 
 * @date 2026-04-16 11:22:09
 */
@Slf4j
@Tag(name = "互动记录管理", description = "互动记录（点赞、收藏、关注等）相关的 RESTful API 接口")
@RestController
@RequestMapping("/interactions")
public class InteractionController {
    
    @Autowired
    private InteractionService interactionService;

    /**
     * 获取互动分页列表
     */
    @Operation(summary = "获取互动分页列表", description = "分页查询互动记录列表，支持条件筛选")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "获取成功"),
        @ApiResponse(responseCode = "500", description = "获取失败")
    })
    @GetMapping
    public R getPage(
        @Parameter(description = "查询参数") @RequestParam Map<String, Object> params, 
        @Parameter(description = "互动查询条件") Interaction interaction,
        @Parameter(hidden = true) HttpServletRequest request) {
        try {
            if (!"管理员".equals(request.getSession().getAttribute("role"))) {
                interaction.setUserId((Long) request.getSession().getAttribute("userId"));
            }
            
            QueryWrapper<Interaction> queryWrapper = new QueryWrapper<>();
            PageUtils page = interactionService.queryPage(params,
                MPUtil.sort(MPUtil.between(MPUtil.likeOrEq(queryWrapper, interaction), params), params));
            
            // 将 Interaction 转换为 InteractionVO
            List<InteractionVO> voList = ((List<Interaction>) page.getList())
                .stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
            page.setList(voList);
            
            return R.ok().put("data", page);
        } catch (Exception e) {
            log.error("获取互动分页列表失败", e);
            return R.error("获取数据失败");
        }
    }
    
    /**
     * 获取互动详情
     */
    @Operation(summary = "获取互动详情", description = "根据互动 ID 获取详细信息")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "获取成功"),
        @ApiResponse(responseCode = "404", description = "互动记录不存在"),
        @ApiResponse(responseCode = "500", description = "获取失败")
    })
    @GetMapping("/{id}")
    public R getInteractionInfo(
        @Parameter(description = "互动 ID", required = true) @PathVariable("id") Long id) {
        try {
            Interaction interaction = interactionService.getById(id);
            if (interaction == null) {
                return R.error("互动记录不存在");
            }
            
            // 将 Interaction 转换为 InteractionDetailVO
            InteractionDetailVO vo = convertToDetailVO(interaction);
            return R.ok().put("data", vo);
        } catch (Exception e) {
            log.error("获取互动详情失败，ID: {}", id, e);
            return R.error("获取详情失败");
        }
    }

    /**
     * 获取用户互动列表
     */
    @Operation(summary = "获取用户互动列表", description = "根据用户 ID 获取该用户的所有互动记录列表")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "获取成功"),
        @ApiResponse(responseCode = "500", description = "获取失败")
    })
    @GetMapping("/user/{userId}")
    public R getUserInteractions(
        @Parameter(description = "用户 ID", required = true) @PathVariable("userId") Long userId,
        @Parameter(description = "操作类型 (可选，1:收藏，2:点赞，3:点踩，4：分享)") @RequestParam(required = false) Integer actionType,
        @Parameter(description = "查询参数") @RequestParam Map<String, Object> params) {
        try {
            InteractionActionType type=InteractionActionType.valueOf(actionType);
            PageUtils page = interactionService.getUserInteractions(userId, type, params);
            
            // 将 Interaction 转换为 InteractionVO
            List<InteractionVO> voList = ((List<Interaction>) page.getList())
                .stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
            page.setList(voList);
            
            return R.ok().put("data", page);
        } catch (Exception e) {
            log.error("获取用户互动列表失败，userId: {}", userId, e);
            return R.error("获取数据失败");
        }
    }

    /**
     * 统计用户操作数量
     */
    @Operation(summary = "统计用户操作数量", description = "根据用户 ID 和操作类型统计互动次数")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "统计成功"),
        @ApiResponse(responseCode = "500", description = "统计失败")
    })
    @GetMapping("/user/{userId}/count")
    public R countUserActions(
        @Parameter(description = "用户 ID", required = true) @PathVariable("userId") Long userId,
        @Parameter(description = "操作类型 (可选，1:收藏，2:点赞，3:点踩，4：分享)") @RequestParam(required = false) Integer actionType) {
        try {
            InteractionActionType type=InteractionActionType.valueOf(actionType);
            Integer count = interactionService.countUserInteractions(userId, type);
            return R.ok().put("data", count);
        } catch (Exception e) {
            log.error("统计用户操作失败，userId: {}", userId, e);
            return R.error("统计失败");
        }
    }

    /**
     * 检查操作状态
     */
    @Operation(summary = "检查操作状态", description = "检查当前用户是否已对指定内容执行过某项操作")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "检查成功"),
        @ApiResponse(responseCode = "500", description = "检查失败")
    })
    @GetMapping("/check")
    public R checkAction(
        @Parameter(description = "内容 ID", required = true) @RequestParam Long contentId,
        @Parameter(description = "操作类型 (1:收藏，2:点赞，3:点踩，4：分享)", required = true) @RequestParam Integer actionType,
        @Parameter(description = "内容类型 (article:文章，comment:评论)") @RequestParam(defaultValue = "article") String tableName,
        @Parameter(hidden = true) HttpServletRequest request) {
        try {
            Long userId = (Long) request.getSession().getAttribute("userId");
            InteractionActionType type = InteractionActionType.valueOf(actionType);
            
            // ✅ 修复：使用 ContentType.fromName() 方法支持大小写不敏感
            ContentType contentType = ContentType.fromName(tableName);
            if (contentType == null) {
                return R.error("无效的内容类型：" + tableName);
            }
            
            boolean exists = interactionService.hasValidInteraction(userId, contentId, type, contentType);
            
            // 构建响应对象
            InteractionUserActionVO vo = new InteractionUserActionVO();
            vo.setContentId(contentId);
            vo.setActionType(type);
            vo.setHasAction(exists);
            
            return R.ok().put("data", vo);
        } catch (Exception e) {
            log.error("检查操作状态失败，contentId: {}, actionType: {}", contentId, actionType, e);
            return R.error("检查失败");
        }
    }

    /**
     * 添加互动记录
     */
    @Operation(summary = "添加互动记录", description = "创建新的互动记录（收藏、点赞、点踩等）")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "操作成功"),
        @ApiResponse(responseCode = "400", description = "操作失败")
    })
    @PostMapping
    public R addInteraction(
        @Parameter(description = "互动信息", required = true) @Valid @RequestBody InteractionCreateDTO createDTO,
        @Parameter(hidden = true) HttpServletRequest request) {
        try {
            Interaction interaction = convertToEntity(createDTO);
            
            // 设置当前用户 ID
            Long userId = (Long) request.getSession().getAttribute("userId");
            if (userId != null) {
                interaction.setUserId(userId);
            }
            
            boolean result = interactionService.addInteraction(interaction);
            if (result) {
                return R.ok("操作成功");
            } else {
                return R.error("操作失败");
            }
        } catch (Exception e) {
            log.error("添加操作失败", e);
            return R.error(e.getMessage());
        }
    }

    /**
     * 取消互动操作
     */
    @Operation(summary = "取消互动操作", description = "取消用户对内容的互动操作")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "取消成功"),
        @ApiResponse(responseCode = "400", description = "取消失败")
    })
    @DeleteMapping("/action")
    public R removeAction(
        @Parameter(description = "取消互动请求", required = true) @Valid @RequestBody InteractionCancelDTO cancelDTO,
        @Parameter(hidden = true) HttpServletRequest request) {
        try {
            Long userId = (Long) request.getSession().getAttribute("userId");
            
            // ✅ 确保 tableName 有值（默认为 article）
            if (cancelDTO.getTableName() == null) {
                cancelDTO.setTableName(ContentType.ARTICLE);
            }
            
            boolean result = interactionService.removeInteraction(
                userId, cancelDTO.getContentId(), cancelDTO.getActionType(), cancelDTO.getTableName());
            if (result) {
                return R.ok("取消操作成功");
            } else {
                return R.error("取消操作失败");
            }
        } catch (Exception e) {
            log.error("取消操作失败，contentId: {}, actionType: {}", cancelDTO.getContentId(), cancelDTO.getActionType(), e);
            return R.error(e.getMessage());
        }
    }

    /**
     * 更新互动记录
     */
    @Operation(summary = "更新互动记录", description = "根据互动 ID 更新互动信息")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "更新成功"),
        @ApiResponse(responseCode = "400", description = "更新失败")
    })
    @PutMapping("/{id}")
    @Transactional
    public R updateInteraction(
        @Parameter(description = "互动 ID", required = true) @PathVariable("id") Long id, 
        @Parameter(description = "互动信息", required = true) @Valid @RequestBody InteractionUpdateDTO updateDTO) {
        try {
            Interaction interaction = interactionService.getById(id);
            if (interaction == null) {
                return R.error("互动记录不存在");
            }
            
            // 使用转换器更新实体
            convertToUpdateEntity(updateDTO, interaction);
            boolean result = interactionService.updateById(interaction);
            if (result) {
                return R.ok("更新成功");
            } else {
                return R.error("更新失败");
            }
        } catch (Exception e) {
            log.error("修改互动记录失败，ID: {}", id, e);
            return R.error(e.getMessage());
        }
    }

    /**
     * 部分更新互动记录
     */
    @Operation(summary = "部分更新互动记录", description = "根据互动 ID 部分更新互动信息")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "更新成功"),
        @ApiResponse(responseCode = "400", description = "更新失败")
    })
    @PatchMapping("/{id}")
    @Transactional
    public R partialUpdateInteraction(
        @Parameter(description = "互动 ID", required = true) @PathVariable("id") Long id, 
        @Parameter(description = "互动信息") @RequestBody InteractionUpdateDTO updateDTO) {
        try {
            Interaction interaction = interactionService.getById(id);
            if (interaction == null) {
                return R.error("互动记录不存在");
            }
            
            // 使用转换器更新实体
            convertToUpdateEntity(updateDTO, interaction);
            boolean result = interactionService.updateById(interaction);
            if (result) {
                return R.ok("更新成功");
            } else {
                return R.error("更新失败");
            }
        } catch (Exception e) {
            log.error("部分更新互动记录失败，ID: {}", id, e);
            return R.error(e.getMessage());
        }
    }

    /**
     * 删除互动记录（单个）
     */
    @Operation(summary = "删除互动记录", description = "根据互动 ID 删除单个互动记录")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "删除成功"),
        @ApiResponse(responseCode = "500", description = "删除失败")
    })
    @DeleteMapping("/{id}")
    public R deleteInteraction(
        @Parameter(description = "互动 ID", required = true) @PathVariable("id") Long id) {
        try {
            interactionService.removeById(id);
            return R.ok("删除成功");
        } catch (Exception e) {
            log.error("删除互动记录失败，ID: {}", id, e);
            return R.error("删除失败");
        }
    }

    /**
     * 批量删除互动记录
     */
    @Operation(summary = "批量删除互动记录", description = "批量删除用户的互动记录")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "删除成功"),
        @ApiResponse(responseCode = "400", description = "请选择要删除的记录"),
        @ApiResponse(responseCode = "500", description = "删除失败")
    })
    @PostMapping("/batch-delete")
    public R batchDeleteInteractions(
        @Parameter(description = "互动 ID 数组", required = true) @RequestBody Long[] ids,
        @Parameter(hidden = true) HttpServletRequest request) {
        try {
            if (ids == null || ids.length == 0) {
                return R.error("请选择要删除的记录");
            }
            
            List<Long> interactionIds = Arrays.stream(ids).collect(Collectors.toList());
            Long userId = (Long) request.getSession().getAttribute("userId");
            boolean result = interactionService.batchRemoveInteractions(userId, interactionIds);
            
            if (result) {
                return R.ok("删除成功");
            } else {
                return R.error("删除失败");
            }
        } catch (Exception e) {
            log.error("删除互动记录失败", e);
            return R.error("删除失败");
        }
    }

    /**
     * 点赞或点踩接口
     */
    @Operation(summary = "点赞或点踩", description = "用户对内容进行点赞或点踩操作")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "操作成功"),
        @ApiResponse(responseCode = "400", description = "用户未登录或无效的操作类型"),
        @ApiResponse(responseCode = "500", description = "操作失败")
    })
    @PostMapping("/like")
    public R likeContent(
        @Parameter(description = "点赞/点踩请求", required = true) @Valid @RequestBody InteractionLikeDTO likeDTO,
        @Parameter(hidden = true) HttpServletRequest request) {
        try {
            Long userId = (Long) request.getSession().getAttribute("userId");
            if (userId == null) {
                return R.error("用户未登录");
            }
            
            // 验证操作类型（必须是 2:点赞 或 3:点踩）
            if (likeDTO.getActionType() == null || 
                (likeDTO.getActionType() != InteractionActionType.LIKE && 
                 likeDTO.getActionType() != InteractionActionType.DISLIKE)) {
                return R.error("无效的操作类型");
            }
            
            // ✅ 确保 tableName 有值（默认为 article）
            if (likeDTO.getTableName() == null) {
                likeDTO.setTableName(ContentType.ARTICLE);
            }
            
            // 转换为 Interaction 实体
            Interaction interaction = convertLikeDTOToEntity(likeDTO);
            
            // 设置当前用户 ID
            interaction.setUserId(userId);
            
            // 调用 Service 层添加互动记录
            boolean result = interactionService.addInteraction(interaction);
            
            if (result) {
                return R.ok("操作成功");
            } else {
                return R.error("操作失败");
            }

        } catch (Exception e) {
            log.error("点赞操作失败，contentId: {}, actionType: {}", likeDTO.getContentId(), likeDTO.getActionType(), e);
            if (e.getMessage() != null && e.getMessage().contains("已执行过此操作")) {
                return R.error("您已经操作过了");
            }
            return R.error("操作失败");
        }
    }

    /**
     * 取消点赞或点踩
     */
    @Operation(summary = "取消点赞或点踩", description = "用户取消对内容的点赞或点踩操作")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "取消成功"),
        @ApiResponse(responseCode = "400", description = "用户未登录或无效的操作类型"),
        @ApiResponse(responseCode = "500", description = "取消失败")
    })
    @DeleteMapping("/like")
    public R unlikeContent(
        @Parameter(description = "取消互动请求", required = true) @Valid @RequestBody InteractionCancelDTO cancelDTO,
        @Parameter(hidden = true) HttpServletRequest request) {
        try {
            Long userId = (Long) request.getSession().getAttribute("userId");
            if (userId == null) {
                return R.error("用户未登录");
            }
            
            // 验证操作类型（必须是 2:点赞 或 3:点踩）
            if (cancelDTO.getActionType() == null || 
                (cancelDTO.getActionType() != InteractionActionType.LIKE && 
                 cancelDTO.getActionType() != InteractionActionType.DISLIKE)) {
                return R.error("无效的操作类型");
            }

            // ✅ 确保 tableName 有值（默认为 article）
            if (cancelDTO.getTableName() == null) {
                cancelDTO.setTableName(ContentType.ARTICLE);
            }

            boolean result = interactionService.removeInteraction(
                userId, cancelDTO.getContentId(), cancelDTO.getActionType(), cancelDTO.getTableName());

            if (result) {
                return R.ok("取消操作成功");
            } else {
                return R.error("取消操作失败");
            }
        } catch (Exception e) {
            log.error("取消点赞操作失败，contentId: {}, actionType: {}", cancelDTO.getContentId(), cancelDTO.getActionType(), e);
            return R.error("操作失败");
        }
    }

    // ==================== 私有转换方法 ====================
    
    /**
     * 将 Interaction 转换为 InteractionVO
     */
    private InteractionVO convertToVO(Interaction interaction) {
        InteractionVO vo = new InteractionVO();
        vo.setId(interaction.getId());
        vo.setContentId(interaction.getContentId());
        vo.setTableName(interaction.getTableName());
        vo.setActionType(interaction.getActionType());
        vo.setUserId(interaction.getUserId());
        vo.setStatus(interaction.getStatus());
        vo.setCreateTime(interaction.getCreateTime());
        return vo;
    }
    
    /**
     * 将 Interaction 转换为 InteractionDetailVO
     */
    private InteractionDetailVO convertToDetailVO(Interaction interaction) {
        InteractionDetailVO vo = new InteractionDetailVO();
        vo.setId(interaction.getId());
        vo.setContentId(interaction.getContentId());
        vo.setTableName(interaction.getTableName());
        vo.setActionType(interaction.getActionType());
        vo.setUserId(interaction.getUserId());
        vo.setRecommendType(interaction.getRecommendType());
        vo.setRemark(interaction.getRemark());
        vo.setStatus(interaction.getStatus());
        vo.setCreateTime(interaction.getCreateTime());
        vo.setUpdateTime(interaction.getUpdateTime());
        return vo;
    }
    
    /**
     * 将 InteractionCreateDTO 转换为 Interaction
     */
    private Interaction convertToEntity(InteractionCreateDTO dto) {
        Interaction interaction = new Interaction();
        interaction.setContentId(dto.getContentId());
        
        // 转换 tableName 为 ContentType 枚举
        if (dto.getTableName() != null) {
            interaction.setTableName(dto.getTableName());
        }
        
        // 将 Integer 转换为 InteractionActionType
        if (dto.getActionType() != null) {
            interaction.setActionType(dto.getActionType());
        }
        
        interaction.setRecommendType(dto.getRecommendType());
        interaction.setRemark(dto.getRemark());
        interaction.setStatus(InteractionStatus.VALID); // 默认为有效状态
        return interaction;
    }
    
    /**
     * 将 InteractionLikeDTO 转换为 Interaction
     */
    private Interaction convertLikeDTOToEntity(InteractionLikeDTO dto) {
        Interaction interaction = new Interaction();
        interaction.setContentId(dto.getContentId());
        
        // 转换 tableName 为 ContentType 枚举（默认为 article）
        interaction.setTableName(dto.getTableName());
        
        // 将 Integer 转换为 InteractionActionType
        if (dto.getActionType() != null) {
            interaction.setActionType(dto.getActionType());
        }
        
        interaction.setStatus(InteractionStatus.VALID);
        return interaction;
    }
    
    /**
     * 将 InteractionUpdateDTO 转换为 Interaction（更新）
     */
    private void convertToUpdateEntity(InteractionUpdateDTO dto, Interaction interaction) {
        if (dto.getRecommendType() != null) {
            interaction.setRecommendType(dto.getRecommendType());
        }
        if (dto.getRemark() != null) {
            interaction.setRemark(dto.getRemark());
        }
        if (dto.getStatus() != null) {
            interaction.setStatus(dto.getStatus());
        }
    }
}
