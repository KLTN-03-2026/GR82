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
@Table(name = "email_log")
public class EmailLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "to_email")
    private String toEmail;

    private String subject;

    @Column(name = "body_snippet", columnDefinition = "TEXT")
    private String bodySnippet;

    @Column(name = "sent_at")
    private OffsetDateTime sentAt;

    // Lưu trạng thái dạng chuỗi: "pending", "sent", "failed"
    private String status = "pending";

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    // Để đơn giản hóa, ta lưu ID của các bảng liên kết thay vì dùng @ManyToOne (dễ bị lỗi Lazy Load)
    @Column(name = "template_id")
    private Long templateId;

    @Column(name = "triggered_by")
    private Long triggeredBy;

}