package com.example.exam_support_dtu.service;

import com.example.exam_support_dtu.entity.Users;
import com.example.exam_support_dtu.repository.UserRepository;
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
        Users user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Không tìm thấy người dùng: " + email));

        // Không cần bóc tách hay build lại nữa, ném thẳng cục User ra cho Spring
        // Security xử lý
        // (Nó sẽ tự gọi các hàm getPassword(), getAuthorities(), isEnabled()... mà ta
        // vừa viết trong Entity)
        return user;
    }
}