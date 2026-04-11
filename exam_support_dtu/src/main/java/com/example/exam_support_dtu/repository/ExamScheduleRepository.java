package com.example.exam_support_dtu.repository;

import com.example.exam_support_dtu.entity.ExamSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Repository
public interface ExamScheduleRepository extends JpaRepository<ExamSchedule, Long> {
    // Tìm lịch thi theo ID của file gốc
    Optional<ExamSchedule> findByExamOriginalId(Long examOriginalId);
}
