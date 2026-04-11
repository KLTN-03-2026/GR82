package com.example.exam_support_dtu.entity;

import com.example.exam_support_dtu.enums.FileType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "files")
public class Files {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "source_name")
    private String sourceName; // Đổi từ source_name -> sourceName

    @Column(name = "original_name")
    private String originalName; // Đổi từ original_name -> originalName

    @Column(name = "stored_name")
    private String storedName; // Đổi từ stored_name -> storedName (Khắc phục lỗi Repository)

    @Enumerated(EnumType.STRING)
    @Column(name = "extension")
    private FileType extension;

    @Column(name = "file_path", nullable = false)
    private String filePath; // Đổi từ file_path -> filePath

    @Column(name = "file_size")
    private Long fileSize; // Đổi từ file_size -> fileSize

    @Column(name = "uploaded_by")
    private Long uploadedBy; // Đổi từ uploaded_by -> uploadedBy

    @Column(name = "uploaded_at")
    private LocalDateTime uploadedAt = LocalDateTime.now(); // Đổi từ uploaded_at -> uploadedAt

    @Column(name = "checksum")
    private String checksum;

}
