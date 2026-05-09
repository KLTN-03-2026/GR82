package com.example.exam_support_dtu.repository;

import com.example.exam_support_dtu.entity.Users;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.OffsetDateTime;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<Users, Long> {
    // Đếm số user đang hoạt động
    long countByIsActiveTrue();

    // Đếm số user đã bị khóa
    long countByIsActiveFalse();

    // Đếm số user đăng ký mới (sau một mốc thời gian nào đó)
    long countByCreatedAtAfter(OffsetDateTime date);
    long countByCreatedAtBetween(OffsetDateTime start, OffsetDateTime end);

    // kiểm tra xem Email
    boolean existsByEmail(String email);

    Optional<Users> findByEmail(String email);
}
