package com.example.exam_support_dtu.repository;

import com.example.exam_support_dtu.entity.VerificationToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

public interface VerificationTokenRepository extends JpaRepository<VerificationToken, Long> {
    Optional<VerificationToken> findByToken(String token);

    // Tìm token dựa vào MÃ SỐ và ID của User
    Optional<VerificationToken> findByUserIdAndToken(Long userId, String token);

    // Bắt buộc phải có 2 Annotation này khi viết hàm Delete hoặc Update tự tạo
    @Transactional
    @Modifying
    void deleteByUserId(Long userId);

}
