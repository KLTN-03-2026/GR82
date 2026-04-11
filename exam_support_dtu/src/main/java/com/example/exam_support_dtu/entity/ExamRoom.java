package com.example.exam_support_dtu.entity;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "exam_room")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ExamRoom {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Khóa ngoại trỏ về exam_schedule
    @ManyToOne
    @JoinColumn(name = "exam_schedule_id", nullable = false)
    private ExamSchedule examSchedule;

    @Column(name = "room_name")
    private String roomName;  // Phòng: 1251

    @Column(name = "building")
    private String building;  // Có thể null

    @Column(name = "location")
    private String location;  // cơ sở: 03 Quang Trung

    @Column(name = "exam_date")
    private LocalDate examDate; // Ngày 19/01/2026

    @Column(name = "exam_time")
    private String examTime;    // 13h30 (Lưu String cho dễ)

    @Column(name = "start_time")
    private LocalTime startTime; // Optional

    @Column(name = "end_time")
    private LocalTime endTime;   // Optional

    @Column(name = "capacity")
    private Integer capacity;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    // Quan hệ 1-Nhiều với ExamStudent
    @OneToMany(mappedBy = "examRoom", cascade = CascadeType.ALL)
    private List<ExamStudent> students = new ArrayList<>();

    // Helper method để thêm sinh viên và gán ngược lại Room
    public void addStudent(ExamStudent student) {
        students.add(student);
        student.setExamRoom(this);
    }





}
