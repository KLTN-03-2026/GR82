package com.example.exam_support_dtu.repository;

import com.example.exam_support_dtu.entity.EmailLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EmailLogRepository extends JpaRepository<EmailLog,Long> {
    long countByStatus(String status);
}
