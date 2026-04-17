package com.example.exam_support_dtu.repository;

import com.example.exam_support_dtu.entity.ExamOriginal;
import com.example.exam_support_dtu.enums.FileStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ExamOriginalRepository extends JpaRepository<ExamOriginal, Long> {

    Optional<ExamOriginal> findByFileUrl(String fileUrl);

    List<ExamOriginal> findByFileOriginalName( String fileOriginalName);

    // Đếm số lượng file theo status
    long countByStatus(FileStatus status);

}
