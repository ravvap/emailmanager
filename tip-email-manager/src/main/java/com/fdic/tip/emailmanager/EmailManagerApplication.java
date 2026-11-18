package com.fdic.tip.emailmanager;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@EnableAsync
@SpringBootApplication
public class EmailManagerApplication {

    public static void main(String[] args) {
        SpringApplication.run(EmailManagerApplication.class, args);
    }
}
