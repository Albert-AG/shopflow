package com.shopflow.orders;

import com.shopflow.orders.infrastructure.config.ShopflowProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(ShopflowProperties.class)
public class ShopflowApplication {

    public static void main(String[] args) {
        SpringApplication.run(ShopflowApplication.class, args);
    }
}
