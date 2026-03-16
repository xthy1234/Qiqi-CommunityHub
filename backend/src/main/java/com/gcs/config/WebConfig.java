package com.gcs.config;

import com.gcs.converter.StringToAuditStatusConverter;
import org.springframework.context.annotation.Configuration;
import org.springframework.format.FormatterRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC 配置类
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final StringToAuditStatusConverter stringToAuditStatusConverter;

    public WebConfig(StringToAuditStatusConverter stringToAuditStatusConverter) {
        this.stringToAuditStatusConverter = stringToAuditStatusConverter;
    }

    @Override
    public void addFormatters(FormatterRegistry registry) {
        registry.addConverter(stringToAuditStatusConverter);
    }
}
