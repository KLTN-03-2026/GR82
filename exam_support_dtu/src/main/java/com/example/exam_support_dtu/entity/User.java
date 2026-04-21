package com.example.exam_support_dtu.entity;

import jakarta.persistence.*;
import lombok.Data;


@Entity
@Table(name = "users", schema = "exam_sp")
@Data
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "password_hash")
    private String passwordHash;

    @Column(nullable = false)
    private String role; // Vì role_type trong DB, Spring sẽ đọc ra String (ADMIN/STUDENT)

    @Column(name = "is_active")
    private boolean isActive = true;

    @Column(name = "full_name")
    private String fullName;
}