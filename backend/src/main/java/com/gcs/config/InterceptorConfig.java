package com.gcs.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.gcs.interceptor.AuthorizationInterceptor;

@Configuration
public class InterceptorConfig implements WebMvcConfigurer {
	
	@Bean
    public AuthorizationInterceptor getAuthorizationInterceptor() {
        return new AuthorizationInterceptor();
    }
	
	@Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(getAuthorizationInterceptor())
            .addPathPatterns("/**")

            .excludePathPatterns("/static/**", "/css/**", "/js/**", "/images/**")

            .excludePathPatterns("/swagger-ui/**", "/swagger-ui.html", "/swagger-ui/index.html")
            .excludePathPatterns("/swagger-resources/**", "/v2/api-docs", "/v3/api-docs/**")
            .excludePathPatterns("/webjars/**")
            .excludePathPatterns("/favicon.ico")

            .excludePathPatterns("/error");
	}
	
	/**
	 * 配置静态资源映射
	 */
	@Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
		registry.addResourceHandler("/**")
        .addResourceLocations("classpath:/resources/")
        .addResourceLocations("classpath:/static/")
        .addResourceLocations("classpath:/")
        .addResourceLocations("classpath:/public/");
    }
}
