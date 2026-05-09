package com.example.exam_support_dtu.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Value("${document.upload-dir}")
    private String documentUploadDir;

    private final com.example.exam_support_dtu.interceptor.PageVisitInterceptor pageVisitInterceptor;

    public WebConfig(com.example.exam_support_dtu.interceptor.PageVisitInterceptor pageVisitInterceptor) {
        this.pageVisitInterceptor = pageVisitInterceptor;
    }

    @Override
    public void addInterceptors(org.springframework.web.servlet.config.annotation.InterceptorRegistry registry) {
        registry.addInterceptor(pageVisitInterceptor).addPathPatterns("/**")
                .excludePathPatterns("/css/**", "/js/**", "/img/**", "/favicon.ico");
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Ánh xạ URL "/imgava/**" tới thư mục thật trên ổ D:
        registry.addResourceHandler("/imgava/**")
                .addResourceLocations("file:D:/Tracuulich/imgava/");

        // Ánh xạ URL "/files/documents/**" trỏ tới thư mục ổ D:
        // Lưu ý: Phải thêm "file:" ở phía trước đường dẫn Windows
        registry.addResourceHandler("/files/documents/**")
                .addResourceLocations("file:" + documentUploadDir);
    }


}