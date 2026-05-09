package com.example.exam_support_dtu.config;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import com.example.exam_support_dtu.repository.UserRepository;
import com.example.exam_support_dtu.entity.Users;
import java.time.OffsetDateTime;

import java.io.IOException;

@Component
public class CustomAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private final UserRepository userRepository;

    public CustomAuthenticationSuccessHandler(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
            Authentication authentication) throws IOException, ServletException {

        // Cập nhật lần đăng nhập cuối
        if (authentication.getPrincipal() instanceof Users) {
            Users user = (Users) authentication.getPrincipal();
            user.setLastLogin(OffsetDateTime.now());
            userRepository.save(user);
        }

        // Kiểm tra xem User đăng nhập có quyền ADMIN hay không
        boolean isAdmin = false;
        for (GrantedAuthority auth : authentication.getAuthorities()) {
            if ("ROLE_ADMIN".equals(auth.getAuthority())) {
                isAdmin = true;
                break;
            }
        }

        // Nếu là Admin, chuyển hướng vào Dashboard
        if (isAdmin) {
            response.sendRedirect("/admin/dashboard");
        }
        // Nếu là Sinh viên (hoặc các role khác), chuyển về trang chủ user
        else {
            response.sendRedirect("/user/home");
        }
    }
}
