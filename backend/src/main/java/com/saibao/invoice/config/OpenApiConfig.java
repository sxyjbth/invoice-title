package com.saibao.invoice.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Swagger/OpenAPI 文档基础信息。 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI invoiceTitleOpenApi() {
        return new OpenAPI().info(new Info()
                .title("发票抬头服务 API")
                .description("钉钉员工端与财务管理端的发票抬头维护、权限、版本和二维码接口")
                .version("v1")
                .contact(new Contact().name("赛宝财务数字化团队")));
    }
}
