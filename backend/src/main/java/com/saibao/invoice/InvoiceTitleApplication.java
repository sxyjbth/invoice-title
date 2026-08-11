package com.saibao.invoice;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

/** 发票抬头服务启动入口。 */
@MapperScan("com.saibao.invoice.mapper")
@SpringBootApplication
@ConfigurationPropertiesScan
@EnableScheduling
public class InvoiceTitleApplication {

    public static void main(String[] args) {
        SpringApplication.run(InvoiceTitleApplication.class, args);
    }
}
