package com.example.exam_support_dtu.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Ánh xạ URL "/imgava/**" tới thư mục thật trên ổ D:
        registry.addResourceHandler("/imgava/**")
                .addResourceLocations("file:D:/Tracuulich/imgava/");
    }
}