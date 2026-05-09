package com.example.exam_support_dtu.repository;

import com.example.exam_support_dtu.entity.AiChatLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AiChatLogRepository extends JpaRepository<AiChatLog, Long> {
    List<AiChatLog> findByUserIdOrderByCreatedAtAsc(Long userId);
}
