package com.example.exam_support_dtu.repository;

import com.example.exam_support_dtu.entity.ExamRoom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

public interface ExamRoomRepository extends JpaRepository<ExamRoom,Long> {

    @Transactional
    void deleteByExamScheduleId(Long examScheduleId);
}
