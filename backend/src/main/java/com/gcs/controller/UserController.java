package com.gcs.controller;

import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.List;

import com.gcs.dto.*;
import com.gcs.enums.CommonStatus;
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
    
    @Autowired
    private UserService userService;

    @Autowired
    private TokenService tokenService;
    
    /**
     * 注册用户
     */
    @IgnoreAuth
    @PostMapping("/register")
    public R register(@Valid @RequestBody UserRegisterDTO registerDTO) {
        try {
            User user = convertToRegisterUser(registerDTO);
            
            // 检查账号是否存在
            if (userService.getUserByAccount(user.getAccount()) != null) {
                return R.error("账号已被注册");
            }
            
            // 检查手机号是否被使用
            if (StringUtils.hasText(user.getPhone()) && userService.getUserByPhone(user.getPhone()) != null) {
                return R.error("手机号已被使用");
            }
            
            // 检查邮箱是否被使用
            if (StringUtils.hasText(user.getEmail()) && userService.getUserByEmail(user.getEmail()) != null) {
                return R.error("邮箱已被使用");
            }
            
            // 加密密码
            user.setPassword(user.getPassword());
            
            // 设置默认值
            user.setRoleId(1L); // 普通用户角色
            user.setStatus(CommonStatus.ENABLED); // 默认启用
            
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
            //测试的时候密码先不加密
            user.setPassword(adminRegisterDTO.getPassword());
            user.setNickname(adminRegisterDTO.getNickname());
            user.setAvatar(adminRegisterDTO.getAvatar());
            user.setPhone(adminRegisterDTO.getPhone());
            user.setEmail(adminRegisterDTO.getEmail());
            user.setStatus(CommonStatus.ENABLED); // 默认启用
            
            // 检查账号是否存在
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
            
            String loginIp = getClientIpAddress(request);
            User user = userService.loginUser(loginDTO.getAccount(), loginDTO.getPassword(), loginIp);
            
            String token = tokenService.generateToken(user.getId(), user.getAccount(), user.getRoleId(), loginIp, getUserAgent(request));
            
            // 构建登录响应
            UserLoginVO loginVO = new UserLoginVO();
            loginVO.setToken(token);
            loginVO.setUser(convertToDetailVO(user));
            
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
            String clientIp = getClientIpAddress(request);
            String userAgent = getUserAgent(request);
            
            String token = tokenService.generateToken(
                user.getId(), 
                user.getAccount(),
                user.getRoleId(),
                clientIp,
                userAgent
            );
            
            // 构建登录响应
            UserLoginVO loginVO = new UserLoginVO();
            loginVO.setToken(token);
            loginVO.setUser(convertToDetailVO(user));
            
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
            request.getSession().invalidate();
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
            UserDetailVO vo = convertToDetailVO(user);
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
            
            // 将 User 转换为 UserVO
            List<UserVO> voList = ((List<User>) page.getList())
                .stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
            page.setList(voList);
            
            return R.ok().put("data", page);
        } catch (Exception e) {
            log.error("获取用户分页列表失败", e);
            return R.error("获取数据失败");
        }
    }
    
    /**
     * 获取用户详情/公开主页信息
     * 
     * @param id 用户ID
     * @param detail 是否获取详细信息（仅自己访问时有效）
     * @param request HTTP 请求
     * @return 用户信息（公开信息或详细信息）
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
        @Parameter(description = "用户ID", required = true) @PathVariable("id") Long id,
        @Parameter(description = "是否获取详细信息（仅自己访问时有效，默认 false）") 
        @RequestParam(defaultValue = "false") Boolean detail,
        HttpServletRequest request) {
        try {
            // 获取当前登录用户ID
            Long currentUserId = null;
            String userIdStr = getSessionAttribute(request, "userId");
            if (userIdStr != null) {
                currentUserId = Long.parseLong(userIdStr);
            }
            
            // 判断是否是访问自己的主页
            boolean isSelf = currentUserId != null && currentUserId.equals(id);
            
            if (isSelf && detail) {
                // 访问自己的主页且要求详细信息
                User user = userService.getById(id);
                if (user == null) {
                    return R.error("用户不存在");
                }
                
                UserDetailVO vo = convertToDetailVO(user);
                return R.ok().put("data", vo);
            } else {
                // 访问别人的主页，或访问自己但只需要公开信息
                UserPublicProfileVO profileVO = userService.getPublicProfile(id, currentUserId);
                if (profileVO == null) {
                    return R.error("用户不存在");
                }
                
                return R.ok().put("data", profileVO);
            }
        } catch (Exception e) {
            log.error("获取用户信息失败，ID: {}", id, e);
            return R.error("获取失败");
        }
    }


    private Long getCurrentUserId(HttpServletRequest request) {
        String userIdStr = getSessionAttribute(request, "userId");
        return userIdStr != null ? Long.parseLong(userIdStr) : null;
    }
    private String getSessionAttribute(HttpServletRequest request, String attributeName) {
        Object attribute = request.getSession().getAttribute(attributeName);
        return attribute != null ? attribute.toString() : null;
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
            User user = convertToRegisterUser(registerDTO);
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
            
            // 如果前端没有传 status，保持原有的 status
            if (updateDTO.getStatus() == null) {
                updateDTO.setStatus(user.getStatus());
            }
            
            // 使用转换器更新实体
            convertToUpdateEntity(updateDTO, user);
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
            
            // 使用转换器更新实体
            convertToUpdateEntity(updateDTO, user);
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

    // ==================== 私有转换方法 ====================
    
    /**
     * 将 User 转换为 UserVO
     */
    private UserVO convertToVO(User user) {
        UserVO vo = new UserVO();
        vo.setId(user.getId());
        vo.setAccount(user.getAccount());
        vo.setNickname(user.getNickname());
        vo.setAvatar(user.getAvatar());
        vo.setGender(user.getGender());
        vo.setPhone(user.getPhone());
        vo.setEmail(user.getEmail());
        vo.setRoleId(user.getRoleId());
        vo.setBirthday(user.getBirthday());
        vo.setSignature(user.getSignature());
        vo.setStatus(user.getStatus());
        vo.setCreateTime(user.getCreateTime());
        return vo;
    }
    
    /**
     * 将 User 转换为 UserDetailVO
     */
    private UserDetailVO convertToDetailVO(User user) {
        UserDetailVO vo = new UserDetailVO();
        vo.setId(user.getId());
        vo.setAccount(user.getAccount());
        vo.setNickname(user.getNickname());
        vo.setAvatar(user.getAvatar());
        vo.setGender(user.getGender());
        vo.setPhone(user.getPhone());
        vo.setEmail(user.getEmail());
        vo.setRoleId(user.getRoleId());
        vo.setBirthday(user.getBirthday());
        vo.setSignature(user.getSignature());
        vo.setStatus(user.getStatus());
        vo.setCreateTime(user.getCreateTime());
        vo.setUpdateTime(user.getUpdateTime());
        vo.setLastLoginTime(user.getLastLoginTime());
        vo.setLastLoginIp(user.getLastLoginIp());
        return vo;
    }
    
    /**
     * 将 User 转换为 UserProfileVO
     */
    private UserProfileVO convertToProfileVO(User user) {
        UserProfileVO vo = new UserProfileVO();
        vo.setId(user.getId());
        vo.setAccount(user.getAccount());
        vo.setNickname(user.getNickname());
        vo.setAvatar(user.getAvatar());
        vo.setGender(user.getGender());
        vo.setSignature(user.getSignature());
        vo.setRoleId(user.getRoleId());
        return vo;
    }
    
    /**
     * 将 UserRegisterDTO 转换为 User
     */
  private User convertToRegisterUser(UserRegisterDTO dto) {
       User user = new User();
       user.setAccount(dto.getAccount());
       user.setPassword(dto.getPassword()); // 实际应用中需要加密
       
       // 处理昵称：如果为空则尝试与账号名一致
       if (dto.getNickname() == null || dto.getNickname().trim().isEmpty()) {
           // 从邮箱中提取用户名部分作为昵称
           String accountPrefix = dto.getAccount().split("@")[0];
           user.setNickname(accountPrefix);
       } else {
           user.setNickname(dto.getNickname());
       }
       
       // 处理头像：如果为空则使用默认头像
       if (dto.getAvatar() == null || dto.getAvatar().trim().isEmpty()) {
           user.setAvatar("http://localhost:8080/server/upload/default_avatar.jpg");
       } else {
           user.setAvatar(dto.getAvatar());
       }
       
       // 处理邮箱：不填的话设置为 null
       if (dto.getEmail() == null || dto.getEmail().trim().isEmpty()) {
           user.setEmail(null);
       } else {
           user.setEmail(dto.getEmail());
       }
       
       user.setPhone(dto.getPhone());
       user.setStatus(CommonStatus.ENABLED); // 默认启用
       user.setRoleId(1L); // 默认为普通用户角色
       return user;
   }

    /**
     * 将 AdminRegisterDTO 转换为 User
     */
  private User convertToAdminRegisterUser(AdminRegisterDTO dto) {
       User user = new User();
       user.setAccount(dto.getAccount());
       user.setPassword(dto.getPassword()); // 实际应用中需要加密
       
       // 处理昵称：如果为空则尝试与账号名一致
       if (dto.getNickname() == null || dto.getNickname().trim().isEmpty()) {
           // 从邮箱中提取用户名部分作为昵称
           String accountPrefix = dto.getAccount().split("@")[0];
           user.setNickname(accountPrefix);
       } else {
           user.setNickname(dto.getNickname());
       }
       
       // 处理头像：如果为空则使用默认头像
       if (dto.getAvatar() == null || dto.getAvatar().trim().isEmpty()) {
           user.setAvatar("http://localhost:8080/server/upload/default_avatar.jpg");
       } else {
           user.setAvatar(dto.getAvatar());
       }
       
       // 处理邮箱：不填的话设置为 null
       if (dto.getEmail() == null || dto.getEmail().trim().isEmpty()) {
           user.setEmail(null);
       } else {
           user.setEmail(dto.getEmail());
       }
       
       user.setPhone(dto.getPhone());
       user.setStatus(CommonStatus.ENABLED); // 默认启用
       user.setRoleId(2L); // 默认为管理员角色
       return user;
   }

    /**
     * 将 UserUpdateDTO 转换为 User（更新）
     */
    private void convertToUpdateEntity(UserUpdateDTO dto, User user) {
        user.setNickname(dto.getNickname());
        user.setAvatar(dto.getAvatar());
        user.setGender(dto.getGender());
        user.setPhone(dto.getPhone());
        user.setEmail(dto.getEmail());
        user.setRoleId(dto.getRoleId());
        user.setBirthday(dto.getBirthday());
        user.setSignature(dto.getSignature());
        if (dto.getStatus() != null) {
            user.setStatus(dto.getStatus());
        }
    }

    // ==================== 原有辅助方法 ====================

    /**
     * 获取客户端 IP 地址
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty() && !"unknown".equalsIgnoreCase(xForwardedFor)) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty() && !"unknown".equalsIgnoreCase(xRealIp)) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }

    /**
     * 获取用户代理信息
     */
    private String getUserAgent(HttpServletRequest request) {
        return request.getHeader("User-Agent");
    }
}
