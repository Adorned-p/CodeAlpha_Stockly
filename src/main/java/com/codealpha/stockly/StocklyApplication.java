package com.codealpha.stockly;

import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@EnableCaching
@EnableScheduling
@SpringBootApplication
public class StocklyApplication {

    public static void main(String[] args) {
        SpringApplication.run(StocklyApplication.class, args);
    }

}
