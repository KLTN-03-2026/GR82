package com.example.exam_support_dtu.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "exam_interest")
public class ExamInterest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Giả sử bạn có class User, nếu chưa có thì tạm để Long userId
    @Column(name = "user_id", nullable = false)
    private Long userId;

    @ManyToOne
    @JoinColumn(name = "exam_room_id")
    private ExamRoom examRoom;

    @ManyToOne
    @JoinColumn(name = "exam_id")
    private ExamSchedule examSchedule;

    @ManyToOne
    @JoinColumn(name = "exam_student_id")
    private ExamStudent examStudent;

    @ManyToOne
    @JoinColumn(name = "exam_original_id")
    private ExamOriginal examOriginal;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @Column(name = "student_notify_days")
    private Integer studentNotifyDays;

    @Column(name = "notified")
    private Boolean notified = false;

    @Column(name = "notify_count")
    private Integer notifyCount = 0;

    @Column(name = "last_notified_at")
    private java.time.OffsetDateTime lastNotifiedAt;
}
