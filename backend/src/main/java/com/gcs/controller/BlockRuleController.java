package com.gcs.controller;


import com.gcs.dto.BlockRuleCreateDTO;
import com.gcs.service.BlockRuleService;
import com.gcs.utils.R;
import com.gcs.utils.SessionUtils;
import com.gcs.vo.BlockRuleVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "屏蔽管理")
@RestController
@RequestMapping("/api/block-rules")
public class BlockRuleController {

    @Autowired
    private BlockRuleService blockRuleService;

    @Autowired
    private SessionUtils sessionUtils;

    @PostMapping
    @Operation(summary = "添加屏蔽规则")
    public R addRule(@Valid @RequestBody BlockRuleCreateDTO dto, HttpServletRequest request) {
        Long userId = sessionUtils.getCurrentUserId(request);
        if (userId == null) return R.error("请先登录");
        
        String result = blockRuleService.addRule(userId, dto);
        
        switch (result) {
            case "added":
                return R.ok("添加成功");
            case "enabled":
                return R.ok("规则已存在，已为您启用");
            case "exists":
                return R.ok("该规则已存在且已启用");
            default:
                return R.ok("操作完成");
        }
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除屏蔽规则")
    public R deleteRule(@PathVariable Long id, HttpServletRequest request) {
        Long userId = sessionUtils.getCurrentUserId(request);
        if (userId == null) return R.error("请先登录");
        
        blockRuleService.deleteRule(userId, id);
        return R.ok("删除成功");
    }

    @PutMapping("/{id}/enable")
    @Operation(summary = "启用/禁用屏蔽规则")
    public R toggleRule(@PathVariable Long id, 
                        @RequestBody Map<String, Boolean> body,
                        HttpServletRequest request) {
        Long userId = sessionUtils.getCurrentUserId(request);
        if (userId == null) return R.error("请先登录");
        
        Boolean enabled = body.get("enabled");
        if (enabled == null) return R.error("请指定启用状态");
        
        blockRuleService.toggleRule(userId, id, enabled);
        return R.ok(enabled ? "已启用" : "已禁用");
    }

    @GetMapping
    @Operation(summary = "查询我的屏蔽规则")
    public R getMyRules(HttpServletRequest request) {
        Long userId = sessionUtils.getCurrentUserId(request);
        if (userId == null) return R.error("请先登录");
        
        List<BlockRuleVO> rules = blockRuleService.getMyRules(userId);
        return R.ok().put("data", rules);
    }
}
