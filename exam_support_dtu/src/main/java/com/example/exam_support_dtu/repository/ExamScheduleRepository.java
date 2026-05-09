package com.example.exam_support_dtu.repository;

import com.example.exam_support_dtu.entity.ExamSchedule;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ExamScheduleRepository extends JpaRepository<ExamSchedule, Long> {
        // Tìm lịch thi theo ID của file gốc
        Optional<ExamSchedule> findByExamOriginalId(Long examOriginalId);

        List<ExamSchedule> findByFileId(Long fileId);

        // tổng sinh viên dự thi
        @Query("SELECT COALESCE(SUM(e.totalStudents), 0) FROM ExamSchedule e")
        Long sumTotalStudents();

        @Query("SELECT e FROM ExamSchedule e WHERE " +
                        "(:semester IS NULL OR :semester = '' OR LOWER(e.semester) LIKE LOWER(CONCAT('%', :semester, '%'))) AND "
                        +
                        "(:courseFilter IS NULL OR :courseFilter = '' OR LOWER(e.courseCode) LIKE LOWER(CONCAT('%', :courseFilter, '%')) "
                        +
                        "OR LOWER(e.courseName) LIKE LOWER(CONCAT('%', :courseFilter, '%')))")
        org.springframework.data.domain.Page<ExamSchedule> findByFilter(
                        @Param("semester") String semester,
                        @Param("courseFilter") String courseFilter,
                        org.springframework.data.domain.Pageable pageable);

        @Query("SELECT COUNT(e) FROM ExamSchedule e WHERE " +
                        "e.id < :id AND " +
                        "(:semester IS NULL OR :semester = '' OR LOWER(e.semester) LIKE LOWER(CONCAT('%', :semester, '%'))) AND "
                        +
                        "(:courseFilter IS NULL OR :courseFilter = '' OR LOWER(e.courseCode) LIKE LOWER(CONCAT('%', :courseFilter, '%')) "
                        +
                        "OR LOWER(e.courseName) LIKE LOWER(CONCAT('%', :courseFilter, '%')))")
        long countBeforeIdWithFilter(@Param("id") Long id, @Param("semester") String semester,
                        @Param("courseFilter") String courseFilter);

        long countByIdLessThan(Long id);
}
