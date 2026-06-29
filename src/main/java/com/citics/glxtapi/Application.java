package com.citics.glxtapi;

import lombok.extern.slf4j.Slf4j;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Slf4j
@SpringBootApplication
//@ComponentScan(basePackages = "com.citics.glxtapi")
//@EnableDiscoveryClient
//@EnableFeignClients
@EnableTransactionManagement
@EnableAsync
//@Import({ApiInitializerImpl.class})
@MapperScan("com.citics.glxtapi.*.mapper")
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
