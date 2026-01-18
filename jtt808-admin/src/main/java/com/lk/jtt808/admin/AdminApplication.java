package com.lk.jtt808.admin;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * JTT808 Admin API Application
 */
@SpringBootApplication(scanBasePackages = "com.lk.jtt808")
@EnableDiscoveryClient
@EnableFeignClients
public class AdminApplication {

    public static void main(String[] args) {
        SpringApplication.run(AdminApplication.class, args);
    }

}