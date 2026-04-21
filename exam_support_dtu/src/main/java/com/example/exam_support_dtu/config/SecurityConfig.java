package com.example.exam_support_dtu.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

@Configuration
@EnableWebSecurity // Kích hoạt tính năng bảo mật
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        // Bắt buộc phải có để Spring Security kiểm tra password_hash trong DB
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                //                .csrf(csrf -> csrf.disable())
//                .authorizeHttpRequests(auth -> auth
//                        .anyRequest().permitAll() // cho phép tất cả request
//                )
//                .formLogin(form -> form.disable())  // tắt form login mặc định
//                .httpBasic(basic -> basic.disable()); // tắt HTTP basic
                .csrf(csrf -> csrf.disable()) // Tắt CSRF để làm việc với form POST dễ dàng hơn
                .authorizeHttpRequests(auth -> auth
                        // Cho phép truy cập công khai trang login và các tài nguyên giao diện
                        .requestMatchers("/login", "/register", "/css/**", "/js/**", "/images/**").permitAll()

                        // Phân quyền dựa trên Role (Lưu ý: Spring tự hiểu ROLE_ADMIN)
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        .requestMatchers("/student/**").hasRole("STUDENT")

                        // Tất cả các request khác đều phải đăng nhập
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/login")                // Trang giao diện đỏ trắng của bạn
                        .loginProcessingUrl("/perform_login") // URL gửi form từ login.html
                        .usernameParameter("email")          // Tên ô input email
                        .passwordParameter("password")       // Tên ô input password
                        .successHandler(myAuthenticationSuccessHandler()) // Bộ điều hướng thông minh
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout")
                        .permitAll()
                );

        return http.build();
    }

    @Bean
    public AuthenticationSuccessHandler myAuthenticationSuccessHandler() {
        return (request, response, authentication) -> {
            var authorities = authentication.getAuthorities();

            for (var authority : authorities) {
                // Kiểm tra chính xác chuỗi Role mà Spring Security đang nắm giữ
                if (authority.getAuthority().equals("ROLE_ADMIN")) {
                    response.sendRedirect("/admin/dashboard");
                    return;
                } else if (authority.getAuthority().equals("ROLE_STUDENT")) {
                    response.sendRedirect("/student/calendar");
                    return;
                }
            }
            response.sendRedirect("/login?error=true");
        };
    }
}


