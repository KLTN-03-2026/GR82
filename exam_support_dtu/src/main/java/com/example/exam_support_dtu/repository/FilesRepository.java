package com.example.exam_support_dtu.repository;

import com.example.exam_support_dtu.entity.Files;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FilesRepository extends JpaRepository<Files, Long> {
    Files findByChecksum(String checksum);
    Optional<Files> findByStoredName(String storedName);

    // 1. CÂU LỆNH LỌC & TÌM KIẾM TỔNG HỢP (NATIVE QUERY CHO POSTGRESQL)
    @Query(value = "SELECT f.* FROM files f " +
            "LEFT JOIN exam_schedule es ON f.id = es.file_id " +
            "LEFT JOIN exam_room er ON es.id = er.exam_schedule_id " +
            "LEFT JOIN exam_interest ei ON er.id = ei.exam_room_id " +
            "WHERE (:search IS NULL OR :search = '' " +
            "       OR LOWER(f.original_name) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "       OR LOWER(f.source_name) LIKE LOWER(CONCAT('%', :search, '%'))) " +
            "AND (:type IS NULL OR :type = '' " +
            "     OR (:type = 'pdf' AND CAST(f.extension AS text) = 'pdf') " +
            "     OR (:type = 'excel' AND CAST(f.extension AS text) IN ('xls', 'xlsx'))) " +
            "GROUP BY f.id " +
            "HAVING (:minInterest IS NULL OR COUNT(DISTINCT ei.user_id) >= :minInterest)",
            countQuery = "SELECT COUNT(*) FROM (SELECT f.id FROM files f " +
                    "LEFT JOIN exam_schedule es ON f.id = es.file_id " +
                    "LEFT JOIN exam_room er ON es.id = er.exam_schedule_id " +
                    "LEFT JOIN exam_interest ei ON er.id = ei.exam_room_id " +
                    "WHERE (:search IS NULL OR :search = '' " +
                    "       OR LOWER(f.original_name) LIKE LOWER(CONCAT('%', :search, '%')) " +
                    "       OR LOWER(f.source_name) LIKE LOWER(CONCAT('%', :search, '%'))) " +
                    "AND (:type IS NULL OR :type = '' " +
                    "     OR (:type = 'pdf' AND CAST(f.extension AS text) = 'pdf') " +
                    "     OR (:type = 'excel' AND CAST(f.extension AS text) IN ('xls', 'xlsx'))) " +
                    "GROUP BY f.id " +
                    "HAVING (:minInterest IS NULL OR COUNT(DISTINCT ei.user_id) >= :minInterest)) AS count_table",
            nativeQuery = true)
    Page<Files> searchAndFilterFiles(@Param("search") String search,
                                     @Param("type") String type,
                                     @Param("minInterest") Integer minInterest,
                                     Pageable pageable);

    // 2. LỌC + SẮP XẾP THEO LƯỢT QUAN TÂM NHIỀU NHẤT
    @Query(value = "SELECT f.* FROM files f " +
            "LEFT JOIN exam_schedule es ON f.id = es.file_id " +
            "LEFT JOIN exam_room er ON es.id = er.exam_schedule_id " +
            "LEFT JOIN exam_interest ei ON er.id = ei.exam_room_id " +
            "WHERE (:search IS NULL OR :search = '' " +
            "       OR LOWER(f.original_name) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "       OR LOWER(f.source_name) LIKE LOWER(CONCAT('%', :search, '%'))) " +
            "AND (:type IS NULL OR :type = '' " +
            "     OR (:type = 'pdf' AND CAST(f.extension AS text) = 'pdf') " +
            "     OR (:type = 'excel' AND CAST(f.extension AS text) IN ('xls', 'xlsx'))) " +
            "GROUP BY f.id " +
            "HAVING (:minInterest IS NULL OR COUNT(DISTINCT ei.user_id) >= :minInterest) " +
            "ORDER BY COUNT(DISTINCT ei.user_id) DESC",
            countQuery = "SELECT COUNT(*) FROM (SELECT f.id FROM files f " +
                    "LEFT JOIN exam_schedule es ON f.id = es.file_id " +
                    "LEFT JOIN exam_room er ON es.id = er.exam_schedule_id " +
                    "LEFT JOIN exam_interest ei ON er.id = ei.exam_room_id " +
                    "WHERE (:search IS NULL OR :search = '' " +
                    "       OR LOWER(f.original_name) LIKE LOWER(CONCAT('%', :search, '%')) " +
                    "       OR LOWER(f.source_name) LIKE LOWER(CONCAT('%', :search, '%'))) " +
                    "AND (:type IS NULL OR :type = '' " +
                    "     OR (:type = 'pdf' AND CAST(f.extension AS text) = 'pdf') " +
                    "     OR (:type = 'excel' AND CAST(f.extension AS text) IN ('xls', 'xlsx'))) " +
                    "GROUP BY f.id " +
                    "HAVING (:minInterest IS NULL OR COUNT(DISTINCT ei.user_id) >= :minInterest)) AS count_table",
            nativeQuery = true)
    Page<Files> searchAndFilterFilesByInterest(@Param("search") String search,
                                               @Param("type") String type,
                                               @Param("minInterest") Integer minInterest,
                                               Pageable pageable);

}
