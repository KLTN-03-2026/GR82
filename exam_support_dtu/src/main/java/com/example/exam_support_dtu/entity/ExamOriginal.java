package com.example.exam_support_dtu.entity;


import com.example.exam_support_dtu.enums.FileType;
import jakarta.persistence.*;
import com.example.exam_support_dtu.enums.FileStatus;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "exam_original")

public class ExamOriginal {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String fileUrl;

    private String fileOriginalName;

    @Enumerated(EnumType.STRING)
    private FileType fileType;

    @Enumerated(EnumType.STRING)
    private FileStatus status = FileStatus.pending;

    private LocalDateTime foundAt = LocalDateTime.now();

    private LocalDateTime lastCheckedAt ;

    private Long savedFileId; // foreign key nhưng lưu dạng Long cho đơn giản

    private String note;

    private String downloadUrl;

}
