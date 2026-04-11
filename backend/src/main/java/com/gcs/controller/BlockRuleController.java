package com.gcs.controller;


import com.gcs.dto.BlockRuleCreateDTO;
import com.gcs.service.BlockRuleService;
import com.gcs.utils.R;
import com.gcs.vo.BlockRuleVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "屏蔽管理")
@RestController
@RequestMapping("/api/block-rules")
public class BlockRuleController {

    @Autowired
    private BlockRuleService blockRuleService;

    @PostMapping
    @Operation(summary = "添加屏蔽规则")
    public R addRule(@Valid @RequestBody BlockRuleCreateDTO dto, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId"); // 假设拦截器已设置
        if (userId == null) return R.error("请先登录");
        
        blockRuleService.addRule(userId, dto);
        return R.ok("添加成功");
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除屏蔽规则")
    public R deleteRule(@PathVariable Long id, HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) return R.error("请先登录");
        
        blockRuleService.deleteRule(userId, id);
        return R.ok("删除成功");
    }

    @GetMapping
    @Operation(summary = "查询我的屏蔽规则")
    public R getMyRules(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        if (userId == null) return R.error("请先登录");
        
        List<BlockRuleVO> rules = blockRuleService.getMyRules(userId);
        return R.ok().put("data", rules);
    }
}
