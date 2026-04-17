package com.example.exam_support_dtu.entity;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "exam_schedule")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ExamSchedule {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Link với bảng exam_original (file gốc)
    @ManyToOne
    @JoinColumn(name = "exam_original_id")
    private ExamOriginal examOriginal;

    // Link với bảng files (file vật lý - nếu có)
    @ManyToOne
    @JoinColumn(name = "file_id")
    private Files file;

    @Column(name = "course_code")
    private String courseCode; // IS-ENG 181

    @Column(name = "course_name")
    private String courseName; // IELTS - Level 2_SPEAKING...

    @Column(name = "credit")
    private Integer credit;    // Số TC (SQL: SMALLINT)

    @Column(name = "semester")
    private String semester;   // Học kỳ

    @Column(name = "attempt")
    private Integer attempt;   // Lần thi (SQL: SMALLINT)

    @Column(name = "parsed_at")
    private LocalDateTime parsedAt = LocalDateTime.now();

    @Column(name = "total_students")
    private Integer totalStudents;

    @Column(name = "notes")
    private String notes;

    // Quan hệ 1-Nhiều với ExamRoom
    @OneToMany(mappedBy = "examSchedule", cascade = CascadeType.ALL)
    private List<ExamRoom> rooms = new ArrayList<>();

}
