package com.lk.jtt808.device;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * JTT808 Device Service Application
 */
@SpringBootApplication(scanBasePackages = {"com.lk.jtt808.device", "com.lk.jtt808.common"})
@EnableDiscoveryClient
@EnableFeignClients
@EnableAsync
@MapperScan("com.lk.jtt808.device.mapper")
public class DeviceApplication {

    public static void main(String[] args) {
        SpringApplication.run(DeviceApplication.class, args);
    }

}
