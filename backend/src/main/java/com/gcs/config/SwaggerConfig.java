package com.gcs.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.util.List;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("游戏社区平台 API")
                .version("1.0.0")
                .description("游戏社区平台后端接口文档")
                .contact(new Contact()
                    .name("Support")
                    .email("support@example.com")))
            .servers(List.of(
                new Server()
                    .url("/")
                    .description("本地服务器")
            ));
    }
}
