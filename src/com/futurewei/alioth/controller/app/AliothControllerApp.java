package com.futurewei.alioth.controller.app;

import java.util.Arrays;
import java.util.HashMap;

import com.futurewei.alioth.controller.cache.config.RedisConfiguration;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication(scanBasePackages = "com.futurewei.alioth.controller")
public class AliothControllerApp {
    public static void main(String[] args) {
        System.out.println("Hello Alioth Controller!");
        //Class<?>[] sources = {AliothControllerApp.class, RedisConfiguration.class};
        SpringApplication.run(AliothControllerApp.class, args);
        System.out.println("Bye from Alioth Controller!");
    }

    @Bean
    public CommandLineRunner commandLineRunner(ApplicationContext ctx) {
        return args -> {

            System.out.println("Let's inspect the beans provided by Spring Boot:");

            String[] beanNames = ctx.getBeanDefinitionNames();
            Arrays.sort(beanNames);
            for (String beanName : beanNames) {
                System.out.println(beanName);
            }

        };
    }
}
