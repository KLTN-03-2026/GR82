package com.example.exam_support_dtu.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Duration;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "audit_log")
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private Long userId; // Có thể NULL nếu là hệ thống tự chạy

    @Column(name = "log_level", length = 20, nullable = false)
    private String logLevel = "INFO"; // SUCCESS, ERROR, WARNING, INFO

    @Column(name = "action")
    private String action;

    @Column(name = "target_type", length = 100)
    private String targetType; // CRAWLER, CRON_JOB, MAIL_SYSTEM, SYSTEM_CONFIG

    @Column(name = "target_id")
    private Long targetId;

    @Column(name = "details", columnDefinition = "TEXT")
    private String details;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // Tự động lấy giờ hiện tại khi tạo mới
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    // ==========================================
    // HÀM ẢO (TRANSIENT) ĐỂ RENDER RA VIEW THYMELEAF
    // ==========================================
    @Transient
    public String getTimeAgo() {
        if (createdAt == null) return "";
        Duration duration = Duration.between(createdAt, LocalDateTime.now());
        long seconds = duration.getSeconds();

        if (seconds < 60) return "VỪA XONG";
        if (seconds < 3600) return (seconds / 60) + " PHÚT TRƯỚC";
        if (seconds < 86400) return (seconds / 3600) + " GIỜ TRƯỚC";
        if (seconds < 172800) return "HÔM QUA"; // Nhỏ hơn 2 ngày

        return (seconds / 86400) + " NGÀY TRƯỚC";
    }
}