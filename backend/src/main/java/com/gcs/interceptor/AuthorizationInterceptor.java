package com.gcs.interceptor;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import com.alibaba.fastjson.JSONObject;
import com.gcs.annotation.IgnoreAuth;
import com.gcs.entity.Token;
import com.gcs.service.TokenService;
import com.gcs.utils.R;

import lombok.extern.slf4j.Slf4j;

/**
 * 权限(Token)验证拦截器
 * 负责用户身份验证和权限控制
 * @author 
 * @date 2026-04-16
 */
@Slf4j
@Component
public class AuthorizationInterceptor implements HandlerInterceptor {

    public static final String AUTHORIZATION_HEADER = "Authorization";
    public static final String BEARER_PREFIX = "Bearer ";
    public static final String LOGIN_TOKEN_KEY = "Token";

    @Autowired
    private TokenService tokenService;

    /**
     * 允许跨域的域名列表（从配置文件读取）
     */
    @Value("${cors.allowed.origins:*}")
    private String allowedOrigins;

    /**
     * 允许的HTTP方法
     */
    private static final List<String> ALLOWED_METHODS = Arrays.asList(
        "GET", "POST", "PUT", "DELETE", "OPTIONS", "HEAD", "PATCH"
    );

    /**
     * 允许的请求头
     */
    private static final List<String> ALLOWED_HEADERS = Arrays.asList(
        "Authorization", "Content-Type", "Accept", "X-Requested-With", 
        "Cache-Control", "Origin", "User-Agent", "Referer"
    );

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        try {
            // 处理跨域预检请求
            if (handleCorsPreflight(request, response)) {
                return false;
            }

            // 设置跨域响应头
            setCorsHeaders(request, response);

            // 检查是否需要跳过权限验证
            if (shouldSkipAuthorization(handler)) {
                return true;
            }

            // 验证并处理Token
            return validateAndProcessToken(request, response);

        } catch (Exception e) {
            log.error("权限拦截器处理异常: {}", e.getMessage(), e);
            handleUnauthorized(response, "服务器内部错误");
            return false;
        }
    }

    /**
     * 处理跨域预检请求(OPTIONS)
     */
    private boolean handleCorsPreflight(HttpServletRequest request, HttpServletResponse response) {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            response.setStatus(HttpStatus.OK.value());
            return true;
        }
        return false;
    }

    /**
     * 设置跨域响应头
     */
    private void setCorsHeaders(HttpServletRequest request, HttpServletResponse response) {
        String origin = request.getHeader(HttpHeaders.ORIGIN);
        
        // 验证来源域名
        if (isAllowedOrigin(origin)) {
            response.setHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, origin);
        }
        
        response.setHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, String.join(",", ALLOWED_METHODS));
        response.setHeader(HttpHeaders.ACCESS_CONTROL_MAX_AGE, "3600");
        response.setHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true");
        response.setHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS, String.join(",", ALLOWED_HEADERS));
    }

    /**
     * 验证来源域名是否允许
     */
    private boolean isAllowedOrigin(String origin) {
        if (StringUtils.isBlank(origin)) {
            return false;
        }
        
        // 如果配置为*，允许所有域名
        if ("*".equals(allowedOrigins)) {
            return true;
        }
        
        // 检查是否在允许列表中
        return Arrays.asList(allowedOrigins.split(",")).contains(origin);
    }

    /**
     * 判断是否应该跳过权限验证
     */
    private boolean shouldSkipAuthorization(Object handler) {
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }

        HandlerMethod handlerMethod = (HandlerMethod) handler;
        IgnoreAuth ignoreAuth = handlerMethod.getMethodAnnotation(IgnoreAuth.class);
        
        return ignoreAuth != null;
    }

    /**
     * 验证并处理Token
     */
    private boolean validateAndProcessToken(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String token = extractToken(request);
        
        if (StringUtils.isBlank(token)) {
            log.warn("请求缺少认证令牌: {}", request.getRequestURI());
            handleUnauthorized(response, "请提供有效的认证令牌");
            return false;
        }

        Token tokenEntity = tokenService.validateAndGetToken(token);
        if (tokenEntity == null) {
            log.warn("无效的认证令牌: {}", token);
            handleUnauthorized(response, "认证令牌无效或已过期");
            return false;
        }

        // 将用户信息存储到 Session 中
        storeUserInfoInSession(request, tokenEntity);
        log.debug("用户认证成功：userId={}, account={}", tokenEntity.getUserId(), tokenEntity.getAccount());
        
        return true;
    }

    /**
     * 从请求中提取Token
     */
    private String extractToken(HttpServletRequest request) {
        // 优先从Authorization header获取
        String authHeader = request.getHeader(AUTHORIZATION_HEADER);
        if (StringUtils.isNotBlank(authHeader) && authHeader.startsWith(BEARER_PREFIX)) {
            return authHeader.substring(BEARER_PREFIX.length());
        }
        
        // 兼容旧的Token header
        String token = request.getHeader(LOGIN_TOKEN_KEY);
        if (StringUtils.isNotBlank(token)) {
            return token;
        }
        
        // 从请求参数获取（适用于某些特殊场景）
        return request.getParameter("token");
    }

    /**
     * 将用户信息存储到 Session 中
     * 注意：session 中的"username"实际存储的是用户的 account（用于登录标识）
     * 展示时应使用 nickname（昵称）
     */
    private void storeUserInfoInSession(HttpServletRequest request, Token tokenEntity) {
        request.getSession().setAttribute("userId", tokenEntity.getUserId());
        request.getSession().setAttribute("roleId", tokenEntity.getRoleId());
        request.getSession().setAttribute("account", tokenEntity.getAccount()); // 这里存储的是 account
    }

    /**
     * 处理未授权响应
     */
    private void handleUnauthorized(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json;charset=UTF-8");
        
        try (PrintWriter writer = response.getWriter()) {
            writer.print(JSONObject.toJSONString(R.error(401, message)));
            writer.flush();
        }
    }
}
