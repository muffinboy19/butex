package com.example.butex;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableCaching
public class ButexApplication {

    public static void main(String[] args) {
        SpringApplication.run(ButexApplication.class, args);
    }
}
