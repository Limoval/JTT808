package com.lk.jtt808;


import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class Jtt808Application {

    public static void main(String[] args) {
        SpringApplication.run(Jtt808Application.class, args);
    }

}
