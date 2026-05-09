package com.example.exam_support_dtu.repository;

import com.example.exam_support_dtu.entity.ExamInterest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ExamInterestRepository extends JpaRepository<ExamInterest, Long> {

    long countByCreatedAtAfter(OffsetDateTime date);

    long countByCreatedAtBetween(OffsetDateTime start, OffsetDateTime end);

    List<ExamInterest> findAllByUserId(Long userId);

    // 1. Đếm số lượt quan tâm theo file gốc
    @Query("SELECT COUNT(ei) FROM ExamInterest ei WHERE ei.examSchedule.examOriginal.id = :fileId")
    long countByFileId(@Param("fileId") Long fileId);

    // 2. Gom nhóm theo fileId
    @Query("SELECT ei.examSchedule.examOriginal.id, COUNT(ei) FROM ExamInterest ei GROUP BY ei.examSchedule.examOriginal.id")
    List<Object[]> countAllInterestsGroupedByFileId();

    // 3. Gom nhóm theo scheduleId
    @Query("SELECT ei.examSchedule.id, COUNT(ei) FROM ExamInterest ei WHERE ei.examSchedule.id IS NOT NULL GROUP BY ei.examSchedule.id")
    List<Object[]> countAllInterestsGroupedByScheduleId();

    // 4. Lấy tất cả exam_interest của 1 user (Lịch thi quan tâm - thủ công,
    // exam_room_id IS NULL)
    @Query("SELECT ei FROM ExamInterest ei WHERE ei.userId = :userId AND ei.examRoom IS NULL")
    List<ExamInterest> findFollowedByUserId(@Param("userId") Long userId);

    // 5. Lấy tất cả exam_interest của 1 user có room (Lịch thi của tôi - tự động)
    @Query("SELECT ei FROM ExamInterest ei WHERE ei.userId = :userId AND ei.examRoom IS NOT NULL")
    List<ExamInterest> findMyExamsByUserId(@Param("userId") Long userId);

    // 6. Kiểm tra đã follow schedule chưa (tránh trùng lặp khi upsert Lịch thi của
    // tôi)
    @Query("SELECT ei FROM ExamInterest ei WHERE ei.userId = :userId AND ei.examRoom.id = :roomId")
    Optional<ExamInterest> findByUserIdAndRoomId(@Param("userId") Long userId, @Param("roomId") Long roomId);

    @Query("SELECT ei FROM ExamInterest ei WHERE ei.userId = :userId AND ei.examRoom.id = :roomId")
    List<ExamInterest> findAllByUserIdAndRoomId(@Param("userId") Long userId, @Param("roomId") Long roomId);

    @Query("SELECT ei FROM ExamInterest ei WHERE ei.userId = :userId AND ei.examStudent.id = :studentId")
    Optional<ExamInterest> findByUserIdAndStudentId(@Param("userId") Long userId, @Param("studentId") Long studentId);

    // 7. Kiểm tra đã quan tâm schedule chưa (tránh trùng lặp khi follow thủ công)
    @Query("SELECT ei FROM ExamInterest ei WHERE ei.userId = :userId AND ei.examSchedule.id = :scheduleId AND ei.examRoom IS NULL")
    Optional<ExamInterest> findFollowedByUserIdAndScheduleId(@Param("userId") Long userId,
            @Param("scheduleId") Long scheduleId);

    @Query("SELECT ei FROM ExamInterest ei WHERE ei.userId = :userId AND ei.examSchedule.id = :scheduleId")
    List<ExamInterest> findAllByUserIdAndExamScheduleId(@Param("userId") Long userId,
            @Param("scheduleId") Long scheduleId);

    @Query("SELECT ei FROM ExamInterest ei WHERE ei.userId = :userId AND ei.examOriginal.id = :originalId")
    List<ExamInterest> findAllByUserIdAndExamOriginalId(@Param("userId") Long userId,
            @Param("originalId") Long originalId);

    @Query("SELECT ei FROM ExamInterest ei WHERE ei.userId = :userId AND ei.examOriginal.id = :originalId AND ei.examSchedule IS NULL")
    Optional<ExamInterest> findFollowedByUserIdAndOriginalId(@Param("userId") Long userId,
            @Param("originalId") Long originalId);

    List<ExamInterest> findByNotifiedFalse();
}
