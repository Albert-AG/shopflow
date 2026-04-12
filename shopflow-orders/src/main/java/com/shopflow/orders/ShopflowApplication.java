package com.shopflow.orders;

import com.shopflow.orders.infrastructure.config.ShopflowProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableConfigurationProperties(ShopflowProperties.class)
@EnableScheduling
public class ShopflowApplication {

    public static void main(String[] args) {
        SpringApplication.run(ShopflowApplication.class, args);
    }
}
