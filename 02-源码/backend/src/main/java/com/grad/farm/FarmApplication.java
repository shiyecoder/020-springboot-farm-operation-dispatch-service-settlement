package com.grad.farm;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 农机作业派单与服务结算管理系统 启动类。
 * 端口通过 application.yml 的 server.port 指定（本通道 8081）。
 */
@SpringBootApplication
@MapperScan("com.grad.farm.mapper")
public class FarmApplication {
    public static void main(String[] args) {
        SpringApplication.run(FarmApplication.class, args);
    }
}
