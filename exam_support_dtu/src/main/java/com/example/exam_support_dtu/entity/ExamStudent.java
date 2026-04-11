package com.example.exam_support_dtu.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "exam_student")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ExamStudent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Khóa ngoại trỏ về exam_room
    @ManyToOne
    @JoinColumn(name = "exam_room_id", nullable = false)
    private ExamRoom examRoom;

    @Column(name = "student_code")
    private String studentCode; // MSV

    @Column(name = "last_name")
    private String lastName;    // Họ

    @Column(name = "first_name")
    private String firstName;   // Tên

    @Column(name = "class_code")
    private String classCode;   // Lớp MH

    @Column(name = "student_class")
    private String studentClass; // Lớp SH

    @Column(name = "seat_number")
    private String seatNumber;   // STT

    @Column(name = "signature")
    private String signature;    // (Để null khi import danh sách thi)

    @Column(name = "grade_number")
    private String gradeNumber;  // Điểm số

    @Column(name = "grade_text")
    private String gradeText;    // Điểm chữ

    @Column(name = "note")
    private String note;         // Ghi chú

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();


}
