package com.gcs.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.stereotype.Service;
import java.util.Map;
import java.util.List;
import java.util.Objects;
import java.time.LocalDateTime;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.gcs.utils.PageUtils;
import com.gcs.utils.Query;

import com.gcs.dao.UserDao;
import com.gcs.entity.User;
import com.gcs.entity.Article;
import com.gcs.service.UserService;
import com.gcs.service.FollowService;
import com.gcs.service.ArticleService;
import com.gcs.entity.view.UserView;
import com.gcs.vo.UserPublicProfileVO;
import java.util.stream.Collectors;

import com.gcs.enums.AuditStatus;

import org.springframework.util.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;

import lombok.extern.slf4j.Slf4j;

import com.gcs.enums.CommonStatus;

/**
 * 用户服务实现类
 * 提供用户相关的业务逻辑处理
 * @author 
 * @date 2026-04-16
 */
@Slf4j
@Service("userService")
public class UserServiceImpl extends ServiceImpl<UserDao, User> implements UserService {

    @Autowired(required = false)
    private FollowService followService;
    
    @Autowired(required = false)
    private ArticleService articleService;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        validateParams(params);
        
        IPage<User> userPage = new Query<User>(params).getPage();
        IPage<User> resultPage = this.page(userPage, new QueryWrapper<>());
        
        return new PageUtils(resultPage);
    }
    
    @Override
    public PageUtils queryPage(Map<String, Object> params, Wrapper<User> queryWrapper) {
        validateQueryParams(params, queryWrapper);
        
        IPage<UserView> userViewPage = new Query<UserView>(params).getPage();
        List<UserView> userViews = baseMapper.selectListView(userViewPage, queryWrapper);
        userViewPage.setRecords(userViews);
        
        return new PageUtils(userViewPage);
    }
    
    @Override
    public List<UserView> selectListView(Wrapper<User> queryWrapper) {
        validateWrapper(queryWrapper);
        return baseMapper.selectListView(queryWrapper);
    }

    @Override
    public UserView selectView(Wrapper<User> queryWrapper) {
        validateWrapper(queryWrapper);
        return baseMapper.selectView(queryWrapper);
    }

    @Override
    public boolean registerUser(User user) {
        validateUserForRegister(user);
        
        // 检查账号唯一性
        if (!isAccountUnique(user.getAccount(), null)) {
            throw new RuntimeException("账号已存在");
        }
        
        // 检查手机号唯一性
        if (StringUtils.hasText(user.getPhone()) && !isPhoneUnique(user.getPhone(), null)) {
            throw new RuntimeException("手机号已被使用");
        }
        
        // 检查邮箱唯一性
        if (StringUtils.hasText(user.getEmail()) && !isEmailUnique(user.getEmail(), null)) {
            throw new RuntimeException("邮箱已被使用");
        }

        // 设置默认值
        user.setCreateTime(LocalDateTime.now());
        user.setStatus(CommonStatus.ENABLED); // 0 表示启用
        if (user.getGender() == null) {
            user.setGender(2); // 默认保密
        }
        
        return this.save(user);
    }

    @Override
    public User loginUser(String account, String password, String loginIp) {
        if (!StringUtils.hasText(account) || !StringUtils.hasText(password)) {
            throw new IllegalArgumentException("账号和密码不能为空");
        }

        User user = baseMapper.selectByAccount(account);
        if (user == null) {
            throw new RuntimeException("账号不存在");
        }

        if (!password.equals(user.getPassword())) {
            throw new RuntimeException("密码错误");
        }

        if (user.getStatus() != CommonStatus.ENABLED) {
            throw new RuntimeException("账号已被禁用");
        }

        // 更新最后登录信息
        user.setLastLoginTime(LocalDateTime.now());
        user.setLastLoginIp(loginIp);
        baseMapper.updateLastLoginInfo(user.getId(), loginIp);
        
        return user;
    }

    @Override
    public User adminLogin(String account, String password) {
        if (!StringUtils.hasText(account) || !StringUtils.hasText(password)) {
            throw new IllegalArgumentException("账号和密码不能为空");
        }

        User user = getUserByAccount(account);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        if (!(user.getRoleId() == 2L)) {
            throw new RuntimeException("无管理员权限");
        }

        if (!password.equals(user.getPassword())) {
            throw new RuntimeException("密码错误");
        }

        if (user.getStatus() != null && user.getStatus() == CommonStatus.DISABLED) {
            throw new RuntimeException("账户已被禁用");
        }

        return user;
    }

    @Override
    public User getUserByAccount(String account) {
        if (!StringUtils.hasText(account)) {
            return null;
        }
        return baseMapper.selectByAccount(account);
    }

    @Override
    public User getUserByNickname(String nickname) {
        if (!StringUtils.hasText(nickname)) {
            return null;
        }
        
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getNickname, nickname);
        return this.getOne(queryWrapper);
    }

    @Override
    public User getUserByPhone(String phone) {
        if (!StringUtils.hasText(phone)) {
            return null;
        }
        return baseMapper.selectByPhone(phone);
    }

    @Override
    public User getUserByEmail(String email) {
        if (!StringUtils.hasText(email)) {
            return null;
        }
        return baseMapper.selectByEmail(email);
    }

    @Override
    public boolean updatePassword(Long userId, String newPassword) {
        if (userId == null || !StringUtils.hasText(newPassword)) {
            throw new IllegalArgumentException("参数不能为空");
        }

        User user = this.getById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        user.setPassword(newPassword);
        user.setUpdateTime(LocalDateTime.now());
        
        return this.updateById(user);
    }

    @Override
    public boolean resetPassword(String account) {
        if (!StringUtils.hasText(account)) {
            throw new IllegalArgumentException("账号不能为空");
        }

        User user = baseMapper.selectByAccount(account);
        if (user == null) {
            throw new RuntimeException("账号不存在");
        }

        user.setPassword("123456"); // 默认密码
        user.setUpdateTime(LocalDateTime.now());
        
        return this.updateById(user);
    }

    @Override
    public boolean changePassword(Long userId, String oldPassword, String newPassword) {
        if (userId == null || !StringUtils.hasText(oldPassword) || !StringUtils.hasText(newPassword)) {
            throw new IllegalArgumentException("参数不能为空");
        }

        User user = this.getById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        if (!user.getPassword().equals(oldPassword)) {
            throw new RuntimeException("原密码错误");
        }

        user.setPassword(newPassword);
        user.setUpdateTime(LocalDateTime.now());
        
        return this.updateById(user);
    }

    @Override
    public boolean updateAvatar(Long userId, String avatarUrl) {
        if (userId == null || !StringUtils.hasText(avatarUrl)) {
            throw new IllegalArgumentException("参数不能为空");
        }

        User user = new User();
        user.setId(userId);
        user.setAvatar(avatarUrl);
        user.setUpdateTime(LocalDateTime.now());
        
        return this.updateById(user);
    }

    @Override
    public Integer countUsers(Integer status) {
        return baseMapper.countUsers(status);
    }

    @Override
    public boolean isAccountUnique(String account, Long excludeUserId) {
        if (!StringUtils.hasText(account)) {
            return true;
        }
        
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getAccount, account);
        if (excludeUserId != null) {
            queryWrapper.ne(User::getId, excludeUserId);
        }
        
        return this.count(queryWrapper) == 0;
    }


    @Override
    public boolean isNicknameUnique(String nickname, Long excludeUserId) {
        if (!StringUtils.hasText(nickname)) {
            return true;
        }
        
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("nickname", nickname);
        if (excludeUserId != null) {
            queryWrapper.ne("id", excludeUserId);
        }
        
        return this.count(queryWrapper) == 0;
    }

    @Override
    public boolean isPhoneUnique(String phone, Long excludeUserId) {
        if (!StringUtils.hasText(phone)) {
            return true;
        }
        
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("phone", phone);
        if (excludeUserId != null) {
            queryWrapper.ne("id", excludeUserId);
        }
        
        return this.count(queryWrapper) == 0;
    }

    @Override
    public boolean isEmailUnique(String email, Long excludeUserId) {
        if (!StringUtils.hasText(email)) {
            return true;
        }
        
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("email", email);
        if (excludeUserId != null) {
            queryWrapper.ne("id", excludeUserId);
        }
        
        return this.count(queryWrapper) == 0;
    }

    @Override
    public boolean createUser(User user) {
        validateUserForCreate(user);
        
        // 检查账号唯一性
        if (!isAccountUnique(user.getAccount(), null)) {
            throw new RuntimeException("账号已存在");
        }
        
        // 检查手机号唯一性
        if (StringUtils.hasText(user.getPhone()) && !isPhoneUnique(user.getPhone(), null)) {
            throw new RuntimeException("手机号已被使用");
        }
        
        // 检查邮箱唯一性
        if (StringUtils.hasText(user.getEmail()) && !isEmailUnique(user.getEmail(), null)) {
            throw new RuntimeException("邮箱已被使用");
        }

        // 设置默认值
        user.setCreateTime(LocalDateTime.now());
        user.setStatus(CommonStatus.ENABLED); // 0 表示启用
        if (user.getGender() == null) {
            user.setGender(2); // 默认保密
        }
        
        return this.save(user);
    }

    @Override
    public boolean updateUser(User user) {
        validateUserForUpdate(user);
        
        // 检查昵称是否被其他用户占用
        if (StringUtils.hasText(user.getNickname())) {
            User existingUser = getUserByNickname(user.getNickname());
            if (existingUser != null && !existingUser.getId().equals(user.getId())) {
                throw new RuntimeException("昵称已存在");
            }
        }

        user.setUpdateTime(LocalDateTime.now());
        return this.updateById(user);
    }

    @Override
    public boolean updateStatus(Long userId, Integer status) {
        if (userId == null || status == null) {
            throw new IllegalArgumentException("参数不能为空");
        }

        User user = this.getById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        CommonStatus commonStatus = CommonStatus.valueOf(status);
        user.setStatus(commonStatus);
        user.setUpdateTime(LocalDateTime.now());
        
        return this.updateById(user);
    }

    @Override
    public void disableUser(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("用户 ID 不能为空");
        }

        User user = this.getById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        user.setStatus(CommonStatus.DISABLED);
        user.setUpdateTime(LocalDateTime.now());
        
        this.updateById(user);
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
    private void validateWrapper(Wrapper<User> queryWrapper) {
        if (Objects.isNull(queryWrapper)) {
            throw new IllegalArgumentException("查询条件不能为空");
        }
    }

    /**
     * 验证查询参数和条件
     */
    private void validateQueryParams(Map<String, Object> params, Wrapper<User> queryWrapper) {
        validateParams(params);
        validateWrapper(queryWrapper);
    }

    /**
     * 验证注册用户参数
     */
    private void validateUserForRegister(User user) {
        if (user == null) {
            throw new IllegalArgumentException("用户信息不能为空");
        }
        if (!StringUtils.hasText(user.getAccount())) {
            throw new IllegalArgumentException("账号不能为空");
        }
        if (!StringUtils.hasText(user.getPassword())) {
            throw new IllegalArgumentException("密码不能为空");
        }
        if (!StringUtils.hasText(user.getNickname())) {
            throw new IllegalArgumentException("用户名不能为空");
        }
    }

    /**
     * 验证创建用户参数
     */
    private void validateUserForCreate(User user) {
        if (user == null) {
            throw new IllegalArgumentException("用户信息不能为空");
        }
        if (!StringUtils.hasText(user.getNickname())) {
            throw new IllegalArgumentException("用户名不能为空");
        }
        if (!StringUtils.hasText(user.getPassword())) {
            throw new IllegalArgumentException("密码不能为空");
        }
    }

    /**
     * 验证更新用户参数
     */
    private void validateUserForUpdate(User user) {
        if (user == null || user.getId() == null) {
            throw new IllegalArgumentException("用户信息不完整");
        }
        if (!StringUtils.hasText(user.getNickname())) {
            throw new IllegalArgumentException("用户名不能为空");
        }
    }

    /**
     * 获取用户公开主页信息
     */
    @Override
    public UserPublicProfileVO getPublicProfile(Long userId, Long currentUserId) {
        // 查询用户基本信息
        User user = this.getById(userId);
        if (user == null) {
            return null;
        }

        // 构建公开信息 VO
        UserPublicProfileVO profileVO = new UserPublicProfileVO();
        profileVO.setId(user.getId());
        profileVO.setNickname(user.getNickname());
        profileVO.setAvatar(user.getAvatar());
        profileVO.setGender(user.getGender());
        profileVO.setSignature(user.getSignature());

        // 统计粉丝数量（查询该用户的 following_id，即谁关注了 TA）
        if (followService != null) {
            Integer followerCount = followService.countFollowers(userId);
            profileVO.setFollowerCount(followerCount != null ? followerCount : 0);

            // 统计关注数量（查询该用户的 follower_id，即 TA 关注了谁）
            Integer followingCount = followService.countFollowing(userId);
            profileVO.setFollowingCount(followingCount != null ? followingCount : 0);

            // 判断当前用户是否已关注该用户
            if (currentUserId != null && !currentUserId.equals(userId)) {
                Boolean isFollowed = followService.isFollowing(currentUserId, userId);
                profileVO.setIsFollowed(isFollowed);
            } else {
                profileVO.setIsFollowed(false);
            }
        } else {
            // 如果关注服务不可用，设置为默认值
            profileVO.setFollowerCount(0);
            profileVO.setFollowingCount(0);
            profileVO.setIsFollowed(false);
        }

        // 统计文章数量
        if (articleService != null) {
            try {
                // 查询该用户的已发布文章数量
                LambdaQueryWrapper<Article> queryWrapper = new LambdaQueryWrapper<>();
                queryWrapper.eq(Article::getAuthorId, userId);
                // 只统计已发布的文章（审核通过的）
                queryWrapper.eq(Article::getAuditStatus, AuditStatus.APPROVED);
                Long articleCount = articleService.count(queryWrapper);
                profileVO.setArticleCount(articleCount != null ? articleCount.intValue() : 0);
            } catch (Exception e) {
                log.warn("统计文章数量失败，userId: {}", userId, e);
                profileVO.setArticleCount(0);
            }
        } else {
            profileVO.setArticleCount(0);
        }

        return profileVO;
    }

    @Override
    public PageUtils getUserPublicList(Map<String, Object> params, Long currentUserId) {
        try {
            int page = Integer.parseInt(params.getOrDefault("page", "1").toString());
            int limit = Integer.parseInt(params.getOrDefault("limit", "20").toString());
            String keyword = (String) params.get("keyword");
            
            // 构建查询条件
            QueryWrapper<User> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("status", CommonStatus.ENABLED.getCode()); // 只查询正常状态的用户
            
            // 关键词搜索（昵称或签名）
            if (keyword != null && !keyword.trim().isEmpty()) {
                queryWrapper.and(wrapper -> 
                    wrapper.like("nickname", keyword)
                           .or()
                           .like("signature", keyword)
                );
            }
            
            // 排除自己（可选）
            if (currentUserId != null) {
                queryWrapper.ne("id", currentUserId);
            }
            
            // 按活跃度排序（可以改为最后登录时间）
            queryWrapper.orderByDesc("last_login_time");
            
            // 分页查询
            IPage<User> userPage = new Page<>(page, limit);
            IPage<User> resultPage = this.page(userPage, queryWrapper);
            
            // 转换为公开信息 VO
            List<UserPublicProfileVO> voList = resultPage.getRecords().stream()
                .map(this::convertToPublicProfileVO)
                .collect(Collectors.toList());
            
            PageUtils pageUtils = new PageUtils(voList, resultPage.getTotal(), limit, page);
            log.info("获取用户公开列表成功，keyword={}, total={}", keyword, resultPage.getTotal());
            
            return pageUtils;
        } catch (Exception e) {
            log.error("获取用户公开列表失败", e);
            throw new RuntimeException("获取用户列表失败：" + e.getMessage());
        }
    }

    /**
     * 将 User 转换为 UserPublicProfileVO
     */
    private UserPublicProfileVO convertToPublicProfileVO(User user) {
        if (user == null) {
            return null;
        }
        
        UserPublicProfileVO vo = new UserPublicProfileVO();
        vo.setId(user.getId());
        vo.setNickname(user.getNickname());
        vo.setAvatar(user.getAvatar());
        vo.setGender(user.getGender());
        vo.setSignature(user.getSignature());
        vo.setBirthday(user.getBirthday());
        
        // 不返回敏感信息
        // vo.setAccount(null);
        // vo.setPhone(null);
        // vo.setEmail(null);
        // vo.setRoleId(null);
        // vo.setStatus(null);
        
        return vo;
    }
}
