package com.gcs.controller;

import java.util.Arrays;
import java.util.Map;
import java.util.List;
import java.util.stream.Collectors;

import com.gcs.enums.FeedbackStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.gcs.dto.FeedbackCreateDTO;
import com.gcs.dto.FeedbackUpdateDTO;
import com.gcs.dto.FeedbackReplyDTO;
import com.gcs.dto.FeedbackStatusUpdateDTO;
import com.gcs.dto.FeedbackBatchStatusUpdateDTO;
import com.gcs.entity.Feedback;
import com.gcs.service.FeedbackService;
import com.gcs.utils.PageUtils;
import com.gcs.utils.R;
import com.gcs.utils.MPUtil;
import com.gcs.vo.FeedbackVO;
import com.gcs.vo.FeedbackDetailVO;
import com.gcs.vo.FeedbackWithUserVO;
import com.gcs.vo.FeedbackStatisticsVO;
import com.gcs.enums.FeedbackStatus;

import lombok.extern.slf4j.Slf4j;

/**
 * 用户反馈控制器
 * 提供反馈相关的 RESTful API 接口
 * @author 
 * @email 
 * @date 2026-04-16 11:22:10
 */
@Slf4j
@Tag(name = "反馈信息管理", description = "反馈信息相关的 RESTful API 接口")
@RestController
@RequestMapping("/feedbacks")
public class FeedbackController {
    
    @Autowired
    private FeedbackService feedbackService;

    /**
     * 获取反馈分页列表
     */
    @Operation(summary = "获取反馈分页列表", description = "分页查询反馈列表，支持条件筛选")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "获取成功"),
        @ApiResponse(responseCode = "500", description = "获取失败")
    })
    @GetMapping
    public R getPage(
        @Parameter(description = "查询参数") @RequestParam Map<String, Object> params, 
        @Parameter(description = "反馈查询条件") Feedback feedback,
        @Parameter(hidden = true) HttpServletRequest request) {
        try {
            String tableName = getSessionAttribute(request, "tableName");
            if ("user".equals(tableName)) {
                feedback.setUserId((Long) request.getSession().getAttribute("userId"));
            }
            
            QueryWrapper<Feedback> queryWrapper = new QueryWrapper<>();
            PageUtils page = feedbackService.queryPage(params, 
                MPUtil.sort(MPUtil.between(MPUtil.likeOrEq(queryWrapper, feedback), params), params));
            
            // 将 Feedback 转换为 FeedbackVO
            List<FeedbackVO> voList = ((List<Feedback>) page.getList())
                .stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
            page.setList(voList);
            
            return R.ok().put("data", page);
        } catch (Exception e) {
            log.error("获取反馈分页列表失败", e);
            return R.error("获取数据失败");
        }
    }
    
    /**
     * 获取反馈详情
     */
    @Operation(summary = "获取反馈详情", description = "根据反馈 ID 获取详细信息")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "获取成功"),
        @ApiResponse(responseCode = "404", description = "反馈不存在"),
        @ApiResponse(responseCode = "500", description = "获取失败")
    })
    @GetMapping("/{id}")
    public R getFeedbackInfo(
        @Parameter(description = "反馈 ID", required = true) @PathVariable("id") Long id) {
        try {
            Feedback feedback = feedbackService.getById(id);
            if (feedback == null) {
                return R.error("反馈不存在");
            }
            
            // 将 Feedback 转换为 FeedbackDetailVO
            FeedbackDetailVO vo = convertToDetailVO(feedback);
            return R.ok().put("data", vo);
        } catch (Exception e) {
            log.error("获取反馈详情失败，ID: {}", id, e);
            return R.error("获取详情失败");
        }
    }

    /**
     * 获取用户反馈列表
     */
    @Operation(summary = "获取用户反馈列表", description = "根据用户账号获取该用户的所有反馈列表")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "获取成功"),
        @ApiResponse(responseCode = "500", description = "获取失败")
    })
    @GetMapping("/user/{userAccount}")
    public R getUserFeedbacks(
        @Parameter(description = "用户账号", required = true) @PathVariable("userAccount") String userAccount,
        @Parameter(description = "查询参数") @RequestParam Map<String, Object> params) {
        try {
            PageUtils page = feedbackService.getUserFeedbacks(userAccount, params);
            
            // 将 Feedback 转换为 FeedbackVO
            List<FeedbackVO> voList = ((List<Feedback>) page.getList())
                .stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
            page.setList(voList);
            
            return R.ok().put("data", page);
        } catch (Exception e) {
            log.error("获取用户反馈失败，userAccount: {}", userAccount, e);
            return R.error("获取数据失败");
        }
    }

    /**
     * 统计不同状态的反馈数量
     */
    @Operation(summary = "统计反馈状态数量", description = "根据反馈状态统计该状态的反馈总数")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "统计成功"),
        @ApiResponse(responseCode = "500", description = "统计失败")
    })
    @GetMapping("/stats/status/{status}")
    public R countByStatus(
        @Parameter(description = "反馈状态 (0:待处理，1:已回复，2:已关闭)", required = true) @PathVariable("status") Integer status) {
        try {
            FeedbackStatus feedbackStatus = FeedbackStatus.valueOf(status);
            Integer count = feedbackService.countByStatus(feedbackStatus.getCode());
            return R.ok().put("data", count);
        } catch (Exception e) {
            log.error("统计反馈状态失败，status: {}", status, e);
            return R.error("统计失败");
        }
    }

    /**
     * 创建反馈
     */
    @Operation(summary = "创建反馈", description = "用户提交新的反馈信息")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "提交成功"),
        @ApiResponse(responseCode = "400", description = "提交失败")
    })
    @PostMapping
    public R createFeedback(
        @Parameter(description = "反馈信息", required = true) @Valid @RequestBody FeedbackCreateDTO createDTO, 
        @Parameter(hidden = true) HttpServletRequest request) {
        try {
            Feedback feedback = convertToEntity(createDTO);
            // 设置当前用户信息
            Long userId = (Long) request.getSession().getAttribute("userId");
            if (userId != null) {
                feedback.setUserId(userId);
            }
            
            boolean result = feedbackService.createFeedback(feedback);
            if (result) {
                return R.ok("反馈提交成功");
            } else {
                return R.error("提交失败");
            }
        } catch (Exception e) {
            log.error("添加反馈失败", e);
            return R.error(e.getMessage());
        }
    }

    /**
     * 回复反馈
     */
    @Operation(summary = "回复反馈", description = "管理员对用户的反馈进行回复")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "回复成功"),
        @ApiResponse(responseCode = "400", description = "回复失败")
    })
    @PostMapping("/{id}/reply")
    public R replyFeedback(
        @Parameter(description = "反馈 ID", required = true) @PathVariable("id") Long feedbackId,
        @Parameter(description = "回复内容", required = true) @RequestParam String replyContent,
        @Parameter(hidden = true) HttpServletRequest request) {
        try {
            String handlerAccount = getSessionAttribute(request, "account");
            boolean result = feedbackService.replyFeedback(feedbackId, replyContent, handlerAccount);
            if (result) {
                return R.ok("回复成功");
            } else {
                return R.error("回复失败");
            }
        } catch (Exception e) {
            log.error("回复反馈失败，ID: {}", feedbackId, e);
            return R.error(e.getMessage());
        }
    }

    /**
     * 更新反馈
     */
    @Operation(summary = "更新反馈", description = "根据反馈 ID 更新反馈信息")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "更新成功"),
        @ApiResponse(responseCode = "400", description = "更新失败")
    })
    @PutMapping("/{id}")
    @Transactional
    public R updateFeedback(
        @Parameter(description = "反馈 ID", required = true) @PathVariable("id") Long id, 
        @Parameter(description = "反馈信息", required = true) @Valid @RequestBody FeedbackUpdateDTO updateDTO) {
        try {
            Feedback feedback = feedbackService.getById(id);
            if (feedback == null) {
                return R.error("反馈不存在");
            }
            
            // 使用转换器更新实体
            convertToUpdateEntity(updateDTO, feedback);
            boolean result = feedbackService.updateById(feedback);
            if (result) {
                return R.ok("更新成功");
            } else {
                return R.error("更新失败");
            }
        } catch (Exception e) {
            log.error("修改反馈失败，ID: {}", id, e);
            return R.error(e.getMessage());
        }
    }

    /**
     * 部分更新反馈
     */
    @Operation(summary = "部分更新反馈", description = "根据反馈 ID 部分更新反馈信息")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "更新成功"),
        @ApiResponse(responseCode = "400", description = "更新失败")
    })
    @PatchMapping("/{id}")
    @Transactional
    public R partialUpdateFeedback(
        @Parameter(description = "反馈 ID", required = true) @PathVariable("id") Long id, 
        @Parameter(description = "反馈信息") @RequestBody FeedbackUpdateDTO updateDTO) {
        try {
            Feedback feedback = feedbackService.getById(id);
            if (feedback == null) {
                return R.error("反馈不存在");
            }
            
            // 使用转换器更新实体
            convertToUpdateEntity(updateDTO, feedback);
            boolean result = feedbackService.updateById(feedback);
            if (result) {
                return R.ok("更新成功");
            } else {
                return R.error("更新失败");
            }
        } catch (Exception e) {
            log.error("部分更新反馈失败，ID: {}", id, e);
            return R.error(e.getMessage());
        }
    }

    /**
     * 更新反馈状态
     */
    @Operation(summary = "更新反馈状态", description = "根据反馈 ID 更新反馈的处理状态")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "状态更新成功"),
        @ApiResponse(responseCode = "400", description = "状态更新失败")
    })
    @PatchMapping("/{id}/status")
    public R updateStatus(
        @Parameter(description = "反馈 ID", required = true) @PathVariable("id") Long feedbackId,
        @Parameter(description = "状态 (0:待处理，1:已回复，2:已关闭)", required = true) @RequestParam Integer status) {
        try {
            // 验证状态值的有效性
            FeedbackStatus.valueOf(status);
            boolean result = feedbackService.updateStatus(feedbackId, status);
            if (result) {
                return R.ok("状态更新成功");
            } else {
                return R.error("状态更新失败");
            }
        } catch (Exception e) {
            log.error("更新反馈状态失败，ID: {}", feedbackId, e);
            return R.error(e.getMessage());
        }
    }

    /**
     * 批量更新反馈状态
     */
    @Operation(summary = "批量更新反馈状态", description = "批量更新多个反馈的处理状态")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "批量更新成功"),
        @ApiResponse(responseCode = "400", description = "请选择要更新的反馈"),
        @ApiResponse(responseCode = "500", description = "批量更新失败")
    })
    @PostMapping("/batch-update-status")
    public R batchUpdateStatus(
        @Parameter(description = "反馈 ID 数组", required = true) @RequestBody Long[] ids,
        @Parameter(description = "状态 (0:待处理，1:已回复，2:已关闭)", required = true) @RequestParam Integer status) {
        try {
            if (ids == null || ids.length == 0) {
                return R.error("请选择要更新的反馈");
            }
            
            // 验证状态值的有效性
            FeedbackStatus.valueOf(status);
            
            List<Long> feedbackIds = Arrays.stream(ids).collect(Collectors.toList());
            boolean result = feedbackService.batchUpdateStatus(feedbackIds, status);
            if (result) {
                return R.ok("批量更新成功");
            } else {
                return R.error("批量更新失败");
            }
        } catch (Exception e) {
            log.error("批量更新反馈状态失败", e);
            return R.error("更新失败");
        }
    }

    /**
     * 删除反馈（单个）
     */
    @Operation(summary = "删除反馈", description = "根据反馈 ID 删除单个反馈")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "删除成功"),
        @ApiResponse(responseCode = "500", description = "删除失败")
    })
    @DeleteMapping("/{id}")
    public R deleteFeedback(
        @Parameter(description = "反馈 ID", required = true) @PathVariable("id") Long id) {
        try {
            feedbackService.removeById(id);
            return R.ok("删除成功");
        } catch (Exception e) {
            log.error("删除反馈失败，ID: {}", id, e);
            return R.error("删除失败");
        }
    }

    /**
     * 批量删除反馈
     */
    @Operation(summary = "批量删除反馈", description = "批量删除多个反馈记录")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "删除成功"),
        @ApiResponse(responseCode = "400", description = "请选择要删除的反馈"),
        @ApiResponse(responseCode = "500", description = "删除失败")
    })
    @PostMapping("/batch-delete")
    public R batchDeleteFeedbacks(
        @Parameter(description = "反馈 ID 数组", required = true) @RequestBody Long[] ids) {
        try {
            if (ids == null || ids.length == 0) {
                return R.error("请选择要删除的反馈");
            }
            
            boolean result = feedbackService.removeByIds(Arrays.asList(ids));
            if (result) {
                return R.ok("删除成功");
            } else {
                return R.error("删除失败");
            }
        } catch (Exception e) {
            log.error("删除反馈失败", e);
            return R.error("删除失败");
        }
    }

    /**
     * 获取反馈总数
     */
    @Operation(summary = "获取反馈总数", description = "统计系统中的反馈总数量")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "查询成功"),
        @ApiResponse(responseCode = "500", description = "查询失败")
    })
    @GetMapping("/count")
    public R getCount(
        @Parameter(description = "查询参数") @RequestParam Map<String, Object> params, 
        @Parameter(description = "反馈查询条件") Feedback feedback, 
        @Parameter(hidden = true) HttpServletRequest request) {
        try {
            String tableName = getSessionAttribute(request, "tableName");
            if ("user".equals(tableName)) {
                Long userId = (Long) request.getSession().getAttribute("userId");
                feedback.setUserId(userId);
            }
            
            QueryWrapper<Feedback> queryWrapper = new QueryWrapper<>();
            Long count = feedbackService.count(MPUtil.sort(
                MPUtil.between(MPUtil.likeOrEq(queryWrapper, feedback), params), params));
                
            return R.ok().put("data", count);
        } catch (Exception e) {
            log.error("获取反馈总数失败", e);
            return R.error("查询失败");
        }
    }

    // ==================== 私有转换方法 ====================
    
    /**
     * 将 Feedback 转换为 FeedbackVO
     */
    private FeedbackVO convertToVO(Feedback feedback) {
        FeedbackVO vo = new FeedbackVO();
        vo.setId(feedback.getId());
        vo.setUserId(feedback.getUserId());
        vo.setUserNickname(feedback.getUserNickname());
        vo.setContent(feedback.getContent());
        vo.setStatus(feedback.getStatus());
        vo.setCreateTime(feedback.getCreateTime());
        return vo;
    }
    
    /**
     * 将 Feedback 转换为 FeedbackDetailVO
     */
    private FeedbackDetailVO convertToDetailVO(Feedback feedback) {
        FeedbackDetailVO vo = new FeedbackDetailVO();
        vo.setId(feedback.getId());
        vo.setUserId(feedback.getUserId());
        vo.setUserNickname(feedback.getUserNickname());
        vo.setContent(feedback.getContent());
        vo.setReplyContent(feedback.getReplyContent());
        vo.setReplyTime(feedback.getReplyTime());
        vo.setStatus(feedback.getStatus());
        vo.setCreateTime(feedback.getCreateTime());
        vo.setUpdateTime(feedback.getUpdateTime());
        return vo;
    }
    
    /**
     * 将 FeedbackCreateDTO 转换为 Feedback
     */
    private Feedback convertToEntity(FeedbackCreateDTO dto) {
        Feedback feedback = new Feedback();
        feedback.setContent(dto.getContent());
        feedback.setStatus(FeedbackStatus.PENDING); // 默认为待处理状态
        return feedback;
    }
    
    /**
     * 将 FeedbackUpdateDTO 转换为 Feedback（更新）
     */
    private void convertToUpdateEntity(FeedbackUpdateDTO dto, Feedback feedback) {
        feedback.setContent(dto.getContent());
        if (dto.getStatus() != null) {
            feedback.setStatus(dto.getStatus());
        }
    }

    // ==================== 原有辅助方法 ====================

    /**
     * 获取会话属性
     */
    private String getSessionAttribute(HttpServletRequest request, String attributeName) {
        Object attribute = request.getSession().getAttribute(attributeName);
        return attribute != null ? attribute.toString() : null;
    }
}
