package com.example.exam_support_dtu.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "documents")
public class Documents {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Liên kết với người đăng tài liệu
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private Users user;

    @Column(name = "course_code", length = 50)
    private String courseCode; // VD: CS 311

    @Column(name = "title", nullable = false)
    private String title; // VD: Đề thi giữa kỳ 2024

    @Column(name = "file_path", nullable = false, length = 500)
    private String filePath; // Đường dẫn file lưu trên server

    @Column(name = "file_type", length = 20)
    private String fileType; // VD: PDF, DOCX, ZIP

    // Trạng thái: PENDING, APPROVED, REJECTED
    @Column(name = "status", length = 20, nullable = false)
    private String status = "PENDING";

    @Column(name = "views")
    private Integer views = 0;

    @Column(name = "downloads")
    private Integer downloads = 0;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

}