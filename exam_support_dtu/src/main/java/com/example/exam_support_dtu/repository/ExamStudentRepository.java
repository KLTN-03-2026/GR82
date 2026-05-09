package com.example.exam_support_dtu.repository;

import com.example.exam_support_dtu.entity.ExamStudent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExamStudentRepository extends JpaRepository<ExamStudent, Long> {
    // Tìm gần đúng theo mã sinh viên, không phân biệt hoa/thường
    List<ExamStudent> findByStudentCodeContainingIgnoreCase(String studentCode);

    // Tìm chính xác theo mã sinh viên
    List<ExamStudent> findByStudentCode(String studentCode);

    @org.springframework.data.jpa.repository.Query("SELECT es FROM ExamStudent es WHERE es.studentCode = :studentCode AND es.examRoom.examSchedule.id = :scheduleId")
    java.util.Optional<ExamStudent> findByStudentCodeAndScheduleId(@org.springframework.data.repository.query.Param("studentCode") String studentCode, @org.springframework.data.repository.query.Param("scheduleId") Long scheduleId);
}
