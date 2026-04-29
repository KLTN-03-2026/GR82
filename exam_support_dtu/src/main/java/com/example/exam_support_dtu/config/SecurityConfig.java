package com.example.exam_support_dtu.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.server.SecurityWebFilterChain;

@Configuration
public class SecurityConfig {

        // 1. Khai báo Bean PasswordEncoder để Spring Security dùng mã hóa/kiểm tra mật
        // khẩu
        @Bean
        public org.springframework.security.crypto.password.PasswordEncoder passwordEncoder() {
                return new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder();
        }

        @Bean
        public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
                http
                                // Tạm tắt CSRF để dễ test form login/register (bạn có thể bật lại sau)
                                .csrf(csrf -> csrf.disable())

                                // Cấu hình phân quyền đường dẫn
                                .authorizeHttpRequests(auth -> auth
                                                // Các trang ai cũng vào được (kể cả chưa đăng nhập)
                                                .requestMatchers("/", "/search", "/login", "/register" , "/search", "/login", "/register","/verify-otp" )
                                                .permitAll()

                                                // CHÍNH THỨC PHÂN QUYỀN: Chỉ tài khoản có Role là ADMIN mới vào được
                                                // khu vực /admin/...
                                                .requestMatchers("/admin/**", "/api/admin/**").hasRole("ADMIN")

                                                // Các trang còn lại: Chỉ cần Đăng nhập là vào được (bất kể là Admin hay
                                                // Student)
                                                .anyRequest().authenticated())

                                // CẤU HÌNH FORM LOGIN
                                .formLogin(form -> form
                                                .loginPage("/login")
                                                .loginProcessingUrl("/perform_login")
                                                .usernameParameter("email")
                                                .passwordParameter("password")
                                                // Thay vì fix cứng 1 trang, ta dùng Handler để kiểm tra Role và điều hướng
                                                .successHandler(new CustomAuthenticationSuccessHandler())
                                                .failureUrl("/login?error=true")
                                                .permitAll())

                                // Cấu hình LOGOUT
                                .logout(logout -> logout
                                                .logoutUrl("/logout")
                                                .logoutSuccessUrl("/login?logout=true")
                                                .permitAll())

                                // CHO PHÉP XEM PDF TRONG IFRAME
                                .headers(headers -> headers.frameOptions(frameOptions -> frameOptions.sameOrigin()));

                return http.build();
        }
}
