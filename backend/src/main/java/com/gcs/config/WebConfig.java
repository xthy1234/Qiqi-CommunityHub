package com.gcs.config;

import com.gcs.converter.StringToAuditStatusConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.format.FormatterRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC 配置类
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final StringToAuditStatusConverter stringToAuditStatusConverter;
    
    @Value("${file.upload.path:#{systemProperties['user.dir']}/uploads}")
    private String uploadPath;

    public WebConfig(StringToAuditStatusConverter stringToAuditStatusConverter) {
        this.stringToAuditStatusConverter = stringToAuditStatusConverter;
    }

    @Override
    public void addFormatters(FormatterRegistry registry) {
        registry.addConverter(stringToAuditStatusConverter);
    }
    
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 映射上传文件目录到 /upload/** 路径
        registry.addResourceHandler("/upload/**")
                .addResourceLocations("file:" + uploadPath + "/");
    }
}
