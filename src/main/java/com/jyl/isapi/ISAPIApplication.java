package com.jyl.isapi;

import com.jyl.isapi.config.PtzProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * @Classname ISAPIApplication
 * @Description TODO
 * @Date 2025/9/2 11:26
 * @Created by startJYL
 */
@EnableConfigurationProperties(PtzProperties.class)
@SpringBootApplication
public class ISAPIApplication {

    public static void main(String[] args) {
        SpringApplication.run(ISAPIApplication.class);
    }

}
