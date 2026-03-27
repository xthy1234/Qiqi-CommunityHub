package com.gcs.controller;

import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.List;

import com.gcs.converter.UserConverter;
import com.gcs.converter.UserRegisterConverter;
import com.gcs.dto.*;
import com.gcs.entity.Role;
import com.gcs.enums.CommonStatus;
import com.gcs.service.RoleService;
import com.gcs.service.impl.UserServiceImpl;
import com.gcs.utils.RequestUtils;
import com.gcs.vo.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;

import com.gcs.annotation.IgnoreAuth;
import com.gcs.entity.User;
import com.gcs.service.UserService;
import com.gcs.service.TokenService;
import com.gcs.utils.PageUtils;
import com.gcs.utils.R;
import com.gcs.utils.MPUtil;
import com.gcs.utils.*;
import com.gcs.vo.UserPublicProfileVO;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

import lombok.extern.slf4j.Slf4j;

/**
 * 用户控制器
 * 提供用户相关的 RESTful API 接口
 * @author 
 * @email 
 * @date 2026-04-16 11:22:10
 */
@Slf4j
@Tag(name = "用户管理", description = "用户相关的 RESTful API 接口")
@RestController
@RequestMapping("/users")
public class UserController {
    
    private final UserService userService;
    
    private final TokenService tokenService;
    
    private final RoleService roleService;
    
    private final UserConverter userConverter;
    
    private final UserRegisterConverter userRegisterConverter;
    @Autowired
    private SessionUtils sessionUtils;

    @Autowired
    public UserController(UserService userService, TokenService tokenService, RoleService roleService, 
                         UserConverter userConverter, UserRegisterConverter userRegisterConverter) {
        this.userService = userService;
        this.tokenService = tokenService;
        this.roleService = roleService;
        this.userConverter = userConverter;
        this.userRegisterConverter = userRegisterConverter;
    }

    @Autowired
    private RequestUtils requestUtils;

    /**
     * 注册用户
     */
    @IgnoreAuth
    @PostMapping("/register")
    public R register(@Valid @RequestBody UserRegisterDTO registerDTO) {
        try {
            User user = userRegisterConverter.toEntity(registerDTO);
            
            if (userService.getUserByAccount(user.getAccount()) != null) {
                return R.error("账号已被注册");
            }
            
            if (StringUtils.hasText(user.getPhone()) && userService.getUserByPhone(user.getPhone()) != null) {
                return R.error("手机号已被使用");
            }
            
            if (StringUtils.hasText(user.getEmail()) && userService.getUserByEmail(user.getEmail()) != null) {
                return R.error("邮箱已被使用");
            }
            
            user.setPassword(user.getPassword());
            user.setRoleId(1L);
            user.setStatus(CommonStatus.ENABLED);
            
            userService.createUser(user);
            
            return R.ok("注册成功");
        } catch (Exception e) {
            log.error("用户注册失败", e);
            return R.error(e.getMessage());
        }
    }

    /**
     * 管理员注册用户
     */
    @PostMapping("/admin/register")
    public R adminRegister(@Valid @RequestBody AdminRegisterDTO adminRegisterDTO) {
        try {
            User user = new User();
            user.setAccount(adminRegisterDTO.getAccount());
            user.setPassword(adminRegisterDTO.getPassword());
            user.setNickname(adminRegisterDTO.getNickname());
            user.setAvatar(adminRegisterDTO.getAvatar());
            user.setPhone(adminRegisterDTO.getPhone());
            user.setEmail(adminRegisterDTO.getEmail());
            user.setStatus(CommonStatus.ENABLED);
            
            if (userService.getUserByAccount(user.getAccount()) != null) {
                return R.error("账号已被注册");
            }
            
            userService.createUser(user);
            
            return R.ok("用户创建成功");
        } catch (Exception e) {
            log.error("管理员创建用户失败", e);
            return R.error(e.getMessage());
        }
    }

    /**
     * 用户登录接口
     */
    @IgnoreAuth
    @PostMapping("/login")
    @Operation(summary = "用户登录", description = "用户账号密码登录")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "登录成功"),
        @ApiResponse(responseCode = "400", description = "账号或密码错误")
    })
    public R login(
        @Parameter(description = "登录信息", required = true) @Valid @RequestBody UserLoginDTO loginDTO, 
        @Parameter(hidden = true) HttpServletRequest request) {
        try {
            if (loginDTO.getAccount() == null || loginDTO.getAccount().isEmpty() || 
                loginDTO.getPassword() == null || loginDTO.getPassword().isEmpty()) {
                return R.error("账号或密码不能为空");
            }
            
            String loginIp = requestUtils.getClientIpAddress(request);
            User user = userService.loginUser(loginDTO.getAccount(), loginDTO.getPassword(), loginIp);
            
            String token = tokenService.generateToken(user.getId(), user.getAccount(), user.getRoleId(), loginIp, requestUtils.getUserAgent(request));
            
            // 构建登录响应
            UserLoginVO loginVO = new UserLoginVO();
            loginVO.setToken(token);
            loginVO.setUser(userConverter.toDetailVO(user));
            
            return R.ok("登录成功").put("data", loginVO);
        } catch (Exception e) {
            log.error("用户登录失败，账号：{}", loginDTO.getAccount(), e);
            return R.error(e.getMessage());
        }
    }

    /**
     * 管理员登录接口
     */
    @IgnoreAuth
    @PostMapping("/admin/login")
    @Operation(summary = "管理员登录", description = "管理员账号密码登录")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "登录成功"),
        @ApiResponse(responseCode = "400", description = "账号或密码错误")
    })
    public R adminLogin(
        @Parameter(description = "登录信息", required = true) @Valid @RequestBody UserLoginDTO loginDTO,
        @Parameter(hidden = true) HttpServletRequest request) {
        try {
            if (loginDTO.getAccount() == null || loginDTO.getAccount().isEmpty() || 
                loginDTO.getPassword() == null || loginDTO.getPassword().isEmpty()) {
                return R.error("账号或密码不能为空");
            }
            
            User user = userService.adminLogin(loginDTO.getAccount(), loginDTO.getPassword());
            String clientIp = requestUtils.getClientIpAddress(request);
            String userAgent = requestUtils.getUserAgent(request);
            
            String token = tokenService.generateToken(
                user.getId(), 
                user.getAccount(),
                user.getRoleId(),
                clientIp,
                userAgent
            );
            
            UserLoginVO loginVO = new UserLoginVO();
            loginVO.setToken(token);
            loginVO.setUser(userConverter.toDetailVO(user)); // 使用注入的 converter
            
            return R.ok().put("data", loginVO);
        } catch (Exception e) {
            log.error("管理员登录失败，账号：{}", loginDTO.getAccount(), e);
            return R.error(e.getMessage());
        }
    }

    /**
     * 退出登录
     */
    @PostMapping("/logout")
    @Operation(summary = "退出登录", description = "用户退出登录")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "退出成功"),
        @ApiResponse(responseCode = "500", description = "退出失败")
    })
    public R logout(@Parameter(hidden = true) HttpServletRequest request) {
        try {
            sessionUtils.invalidateSession(request);
            return R.ok("退出成功");
        } catch (Exception e) {
            log.error("用户退出失败", e);
            return R.error("退出失败");
        }
    }
    
    /**
     * 获取当前登录用户信息
     */
    @GetMapping("/me")
    @Operation(summary = "获取当前用户信息", description = "获取当前登录用户的详细信息")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "获取成功"),
        @ApiResponse(responseCode = "401", description = "用户未登录")
    })
    public R getCurrentUser(@Parameter(hidden = true) HttpServletRequest request) {
        try {
            Long userId = (Long) request.getSession().getAttribute("userId");
            if (userId == null) {
                return R.error("用户未登录");
            }
            
            User user = userService.getById(userId);
            UserDetailVO vo = userConverter.toDetailVO(user); // 使用注入的 converter
            return R.ok().put("data", vo);
        } catch (Exception e) {
            log.error("获取当前用户信息失败", e);
            return R.error("获取用户信息失败");
        }
    }
    
    /**
     * 获取用户分页列表
     */
    @GetMapping
    @Operation(summary = "获取用户分页列表", description = "分页查询用户列表，支持条件筛选")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "获取成功"),
        @ApiResponse(responseCode = "500", description = "获取失败")
    })
    public R getPage(
        @Parameter(description = "查询参数") @RequestParam Map<String, Object> params, 
        @Parameter(description = "用户查询条件") User user) {
        try {
            QueryWrapper<User> queryWrapper = new QueryWrapper<>();
            PageUtils page = userService.queryPage(params, 
                MPUtil.sort(MPUtil.between(MPUtil.likeOrEq(queryWrapper, user), params), params));
            
            List<User> userList = (List<User>) page.getList();
            List<UserVO> voList = userConverter.toVOList(userList);
            
            if (!voList.isEmpty()) {
                List<Long> roleIds = voList.stream()
                    .map(UserVO::getRoleId)
                    .filter(Objects::nonNull)
                    .distinct()
                    .collect(Collectors.toList());
                
                if (!roleIds.isEmpty()) {
                    List<Role> roles = roleService.listByIds(roleIds);
                    Map<Long, String> roleNameMap = roles.stream()
                        .collect(Collectors.toMap(Role::getId, Role::getRoleName, (v1, v2) -> v1));
                    
                    voList.forEach(vo -> {
                        if (vo.getRoleId() != null && roleNameMap.containsKey(vo.getRoleId())) {
                            vo.setRoleName(roleNameMap.get(vo.getRoleId()));
                        }
                    });
                }
            }
            
            page.setList(voList);
            
            return R.ok().put("data", page);
        } catch (Exception e) {
            log.error("获取用户分页列表失败", e);
            return R.error("获取数据失败");
        }
    }
    
    /**
     * 获取用户详情/公开主页信息
     */
    @GetMapping("/{id}")
    @IgnoreAuth
    @Operation(summary = "获取用户详情或公开主页", description = "访问别人主页返回公开信息，访问自己主页可通过 detail 参数选择返回公开信息或详细信息")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "获取成功"),
        @ApiResponse(responseCode = "404", description = "用户不存在"),
        @ApiResponse(responseCode = "500", description = "获取失败")
    })
    public R getUserProfile(
        @Parameter(description = "用户 ID", required = true) @PathVariable("id") Long id,
        @Parameter(description = "是否获取详细信息（仅自己访问时有效，默认 false）") 
        @RequestParam(defaultValue = "false") Boolean detail,
        HttpServletRequest request) {
        try {
            Long currentUserId = sessionUtils.getCurrentUserId(request);
            boolean isSelf = currentUserId != null && currentUserId.equals(id);
            
            if (isSelf && detail) {
                User user = userService.getById(id);
                if (user == null) {
                    return R.error("用户不存在");
                }
                
                UserDetailVO vo = userConverter.toDetailVO(user);
                return R.ok().put("data", vo);
            } else {
                // ✅ Service 返回 Entity
                User user = userService.getById(id);
                if (user == null) {
                    return R.error("用户不存在");
                }
                
                // ✅ Controller 调用 Service 方法构建公开主页 VO（包含统计数据）
                UserPublicProfileVO profileVO = ((UserServiceImpl) userService).buildPublicProfileVO(user, currentUserId);
                
                return R.ok().put("data", profileVO);
            }
        } catch (Exception e) {
            log.error("获取用户信息失败，ID: {}", id, e);
            return R.error("获取失败");
        }
    }

    /**
     * 创建用户（后台管理使用）
     */
    @PostMapping
    @Operation(summary = "创建用户", description = "管理员在后台创建用户或管理员")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "创建成功"),
        @ApiResponse(responseCode = "400", description = "创建失败")
    })
    public R createUser(
        @Parameter(description = "用户信息", required = true) @Valid @RequestBody UserRegisterDTO registerDTO,
        @Parameter(description = "角色 ID", required = true) @RequestParam Long roleId) {
        try {
            User user = userConverter.toEntity(registerDTO);
            user.setRoleId(roleId);
            boolean result = userService.createUser(user);
            if (result) {
                return R.ok("用户创建成功");
            } else {
                return R.error("创建失败");
            }
        } catch (Exception e) {
            log.error("创建用户失败，账号：{}", registerDTO.getAccount(), e);
            return R.error(e.getMessage());
        }
    }

    /**
     * 更新用户信息
     */
    @PutMapping("/{id}")
    @Transactional
    @Operation(summary = "更新用户信息", description = "根据用户 ID 更新用户信息")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "更新成功"),
        @ApiResponse(responseCode = "400", description = "更新失败")
    })
    public R updateUser(
        @Parameter(description = "用户 ID", required = true) @PathVariable("id") Long id, 
        @Parameter(description = "用户信息", required = true) @Valid @RequestBody UserUpdateDTO updateDTO) {
        try {
            User user = userService.getById(id);
            if (user == null) {
                return R.error("用户不存在");
            }
            
            if (updateDTO.getStatus() == null) {
                updateDTO.setStatus(user.getStatus());
            }
            
            userConverter.updateEntity(updateDTO, user);
            boolean result = userService.updateUser(user);
            if (result) {
                return R.ok("更新成功");
            } else {
                return R.error("更新失败");
            }
        } catch (Exception e) {
            log.error("修改用户失败，ID: {}", id, e);
            return R.error(e.getMessage());
        }
    }

    /**
     * 部分更新用户
     */
    @PatchMapping("/{id}")
    @Transactional
    @Operation(summary = "部分更新用户", description = "根据用户 ID 部分更新用户信息")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "更新成功"),
        @ApiResponse(responseCode = "400", description = "更新失败")
    })
    public R partialUpdateUser(
        @Parameter(description = "用户 ID", required = true) @PathVariable("id") Long id, 
        @Parameter(description = "用户信息") @RequestBody UserUpdateDTO updateDTO) {
        try {
            User user = userService.getById(id);
            if (user == null) {
                return R.error("用户不存在");
            }
            
            userConverter.updateEntity(updateDTO, user);
            boolean result = userService.updateUser(user);
            if (result) {
                return R.ok("更新成功");
            } else {
                return R.error("更新失败");
            }
        } catch (Exception e) {
            log.error("部分更新用户失败，ID: {}", id, e);
            return R.error(e.getMessage());
        }
    }

    /**
     * 更新用户密码
     */
    @PutMapping("/{id}/password")
    @Operation(summary = "更新用户密码", description = "用户修改自己的密码")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "更新成功"),
        @ApiResponse(responseCode = "401", description = "用户未登录"),
        @ApiResponse(responseCode = "403", description = "只能修改自己的密码"),
        @ApiResponse(responseCode = "400", description = "原密码错误")
    })
    public R updatePassword(
        @Parameter(description = "用户 ID", required = true) @PathVariable("id") Long userId,
        @Parameter(description = "原密码", required = true) @RequestParam String oldPassword,
        @Parameter(description = "新密码", required = true) @RequestParam String newPassword,
        @Parameter(hidden = true) HttpServletRequest request) {
        try {
            Long currentUserId = (Long) request.getSession().getAttribute("userId");
            if (currentUserId == null) {
                return R.error("用户未登录");
            }

            if (!Objects.equals(currentUserId, userId)) {
                return R.error("只能修改自己的密码");
            }

            User user = userService.getById(userId);
            if (!Objects.equals(oldPassword, user.getPassword())) {
                return R.error("原密码错误");
            }

            boolean result = userService.updatePassword(userId, newPassword);
            if (result) {
                return R.ok("密码更新成功");
            } else {
                return R.error("密码更新失败");
            }
        } catch (Exception e) {
            log.error("更新密码失败", e);
            return R.error("更新失败");
        }
    }

    /**
     * 修改密码（管理员接口）
     */
    @PutMapping("/{id}/change-password")
    @Operation(summary = "修改密码", description = "管理员修改用户密码")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "修改成功"),
        @ApiResponse(responseCode = "400", description = "修改失败")
    })
    public R changePassword(
        @Parameter(description = "用户 ID", required = true) @PathVariable("id") Long userId,
        @Parameter(description = "原密码", required = true) @RequestParam String oldPassword,
        @Parameter(description = "新密码", required = true) @RequestParam String newPassword) {
        try {
            boolean result = userService.changePassword(userId, oldPassword, newPassword);
            if (result) {
                return R.ok("密码修改成功");
            } else {
                return R.error("密码修改失败");
            }
        } catch (Exception e) {
            log.error("修改密码失败，ID: {}", userId, e);
            return R.error(e.getMessage());
        }
    }

    /**
     * 更新用户头像
     */
    @PatchMapping("/{id}/avatar")
    @Operation(summary = "更新用户头像", description = "用户更新自己的头像")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "更新成功"),
        @ApiResponse(responseCode = "401", description = "用户未登录"),
        @ApiResponse(responseCode = "403", description = "只能修改自己的头像")
    })
    public R updateAvatar(
        @Parameter(description = "用户 ID", required = true) @PathVariable("id") Long userId,
        @Parameter(description = "头像 URL", required = true) @RequestParam String avatarUrl,
        @Parameter(hidden = true) HttpServletRequest request) {
        try {
            Long currentUserId = (Long) request.getSession().getAttribute("userId");
            if (currentUserId == null) {
                return R.error("用户未登录");
            }

            if (!Objects.equals(currentUserId, userId)) {
                return R.error("只能修改自己的头像");
            }

            boolean result = userService.updateAvatar(userId, avatarUrl);
            if (result) {
                return R.ok("头像更新成功");
            } else {
                return R.error("头像更新失败");
            }
        } catch (Exception e) {
            log.error("更新头像失败", e);
            return R.error("更新失败");
        }
    }

    /**
     * 重置密码（管理员操作或忘记密码）
     */
    @PostMapping("/reset-password")
    @Operation(summary = "重置密码", description = "重置用户密码为默认密码")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "重置成功"),
        @ApiResponse(responseCode = "400", description = "重置失败")
    })
    public R resetPassword(
        @Parameter(description = "用户账号", required = true) @RequestParam String account) {
        try {
            boolean result = userService.resetPassword(account);
            if (result) {
                return R.ok("密码已重置为：123456");
            } else {
                return R.error("重置失败");
            }
        } catch (Exception e) {
            log.error("重置密码失败，账号：{}", account, e);
            return R.error(e.getMessage());
        }
    }

    /**
     * 启用/禁用用户
     */
    @PatchMapping("/{id}/status")
    @Operation(summary = "启用/禁用用户", description = "设置用户的启用/禁用状态")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "更新成功"),
        @ApiResponse(responseCode = "400", description = "更新失败")
    })
    public R updateStatus(
        @Parameter(description = "用户 ID", required = true) @PathVariable("id") Long userId,
        @Parameter(description = "状态 (0:启用，1:禁用)", required = true) @RequestParam Integer status) {
        try {
            boolean result = userService.updateStatus(userId, status);
            if (result) {
                return R.ok("状态更新成功");
            } else {
                return R.error("状态更新失败");
            }
        } catch (Exception e) {
            log.error("更新用户状态失败，ID: {}", userId, e);
            return R.error(e.getMessage());
        }
    }

    /**
     * 删除用户（单个）
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除用户", description = "根据用户 ID 删除用户")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "删除成功"),
        @ApiResponse(responseCode = "500", description = "删除失败")
    })
    public R deleteUser(
        @Parameter(description = "用户 ID", required = true) @PathVariable("id") Long id) {
        try {
            userService.removeById(id);
            return R.ok("删除成功");
        } catch (Exception e) {
            log.error("删除用户失败，ID: {}", id, e);
            return R.error("删除失败");
        }
    }

    /**
     * 批量删除用户
     */
    @PostMapping("/batch-delete")
    @Operation(summary = "批量删除用户", description = "批量删除多个用户")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "删除成功"),
        @ApiResponse(responseCode = "400", description = "请选择要删除的用户"),
        @ApiResponse(responseCode = "500", description = "删除失败")
    })
    public R batchDeleteUsers(
        @Parameter(description = "用户 ID 数组", required = true) @RequestBody Long[] ids) {
        try {
            if (ids == null || ids.length == 0) {
                return R.error("请选择要删除的用户");
            }
            
            boolean result = userService.removeByIds(Arrays.asList(ids));
            if (result) {
                return R.ok("删除成功");
            } else {
                return R.error("删除失败");
            }
        } catch (Exception e) {
            log.error("删除用户失败", e);
            return R.error("删除失败");
        }
    }
    
    /**
     * 统计用户总数
     */
    @GetMapping("/count")
    @Operation(summary = "统计用户总数", description = "统计系统中的用户总数")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "统计成功"),
        @ApiResponse(responseCode = "500", description = "统计失败")
    })
    public R getUserCount(
        @Parameter(description = "用户状态 (可选)") @RequestParam(required = false) Integer status) {
        try {
            Integer count = userService.countUsers(status);
            return R.ok().put("data", count);
        } catch (Exception e) {
            log.error("统计用户数量失败", e);
            return R.error("统计失败");
        }
    }

    /**
     * 获取用户公开列表（用户端使用）
     */
    @GetMapping("/public/list")
    @IgnoreAuth
    @Operation(summary = "获取用户公开列表", description = "分页查询用户公开信息列表，用于前端展示")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "获取成功"),
        @ApiResponse(responseCode = "500", description = "获取失败")
    })
    public R getUserPublicList(
        @Parameter(description = "查询参数（page, limit, keyword）") 
        @RequestParam Map<String, Object> params,
        @Parameter(hidden = true) HttpServletRequest request) {
        try {
            // 尝试获取当前登录用户 ID（可选）
            Long currentUserId = sessionUtils.getCurrentUserId(request);
            
            PageUtils page = userService.getUserPublicList(params, currentUserId);
            return R.ok().put("data", page);
        } catch (Exception e) {
            log.error("获取用户公开列表失败", e);
            return R.error("获取数据失败");
        }
    }

}
