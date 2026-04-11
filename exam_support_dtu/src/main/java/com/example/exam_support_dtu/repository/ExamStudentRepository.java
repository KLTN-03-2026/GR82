package com.example.exam_support_dtu.repository;

import com.example.exam_support_dtu.entity.ExamStudent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ExamStudentRepository extends JpaRepository<ExamStudent, Long> {
}
