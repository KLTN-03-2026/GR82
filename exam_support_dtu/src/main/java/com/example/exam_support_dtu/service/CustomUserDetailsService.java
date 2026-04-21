package com.example.exam_support_dtu.service;

import com.example.exam_support_dtu.entity.User;
import com.example.exam_support_dtu.repository.UserRepository;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        // 1. Lấy user từ DB theo email
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Không tìm thấy người dùng: " + email));

        // 2. Kiểm tra tài khoản có đang bị khóa (is_active = false) không
        if (!user.isActive()) {
            throw new DisabledException("Tài khoản này đã bị vô hiệu hóa.");
        }

        // 3. Trả về đối tượng User của Security để nó tự check password và role
        return org.springframework.security.core.userdetails.User
                .withUsername(user.getEmail())
                .password(user.getPasswordHash())
                // THAY ĐỔI Ở ĐÂY: Ép chuỗi "admin" thành "ADMIN"
                .roles(user.getRole().toUpperCase())
                .build();
    }
}