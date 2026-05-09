package com.example.exam_support_dtu.repository;

import com.example.exam_support_dtu.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    // Lấy 5 logs mới nhất, sắp xếp giảm dần theo thời gian
    List<AuditLog> findTop5ByOrderByCreatedAtDesc();

    List<AuditLog> findTop10ByOrderByCreatedAtDesc();
}
