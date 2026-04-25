package com.example.exam_support_dtu.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Users {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "email", nullable = false, unique = true, length = 255)
    private String email;

    @Column(name = "full_name", length = 255)
    private String fullName;

    @Column(name = "student_code", length = 50)
    private String studentCode; // MSSV

    @Column(name = "phone_number", length = 20)
    private String phoneNumber; // Số điện thoại

    @Column(name = "faculty", length = 150)
    private String faculty; // Khoa (VD: Khoa Công nghệ Thông tin)

    @Column(name = "class_name", length = 50)
    private String class_name; // Khóa (VD: K28TPM28)

    // Default là 'google' theo DB
    @Column(name = "provider", nullable = false, length = 50)
    private String provider = "google";

    @Column(name = "password_hash", length = 255)
    private String passwordHash;

    // Default là 'student' theo DB
    @Column(name = "role", nullable = false, length = 50)
    private String role = "student";

    @Column(name = "avatar_url", columnDefinition = "TEXT")
    private String avatarUrl;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "last_login")
    private OffsetDateTime lastLogin;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    // ==========================================
    // Tự động gán thời gian lúc mới tạo User
    // ==========================================
    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = OffsetDateTime.now();
        }
    }
}