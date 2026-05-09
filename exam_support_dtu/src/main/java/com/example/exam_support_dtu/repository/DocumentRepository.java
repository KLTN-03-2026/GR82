package com.example.exam_support_dtu.repository;

import com.example.exam_support_dtu.entity.Documents;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DocumentRepository extends JpaRepository<Documents, Long> {
    // Lấy danh sách tài liệu theo trạng thái, sắp xếp mới nhất lên đầu
    List<Documents> findByStatusOrderByCreatedAtDesc(String status);

    // Phân trang tài liệu theo trạng thái
    Page<Documents> findByStatus(String status, Pageable pageable);

    // Đếm số lượng tài liệu theo trạng thái
    long countByStatus(String status);

    // Lấy tài liệu của 1 user cụ thể
    List<Documents> findByUserIdOrderByCreatedAtDesc(Long userId);

    // Tính tổng lượt xem của tài liệu theo trạng thái
    @org.springframework.data.jpa.repository.Query("SELECT SUM(d.views) FROM Documents d WHERE d.status = :status")
    Long sumViewsByStatus(String status);

    // Tính tổng lượt tải của tài liệu theo trạng thái
    @org.springframework.data.jpa.repository.Query("SELECT SUM(d.downloads) FROM Documents d WHERE d.status = :status")
    Long sumDownloadsByStatus(String status);
}
