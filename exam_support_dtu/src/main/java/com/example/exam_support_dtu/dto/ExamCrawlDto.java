package com.example.exam_support_dtu.dto;


import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor

public class ExamCrawlDto {
    private String sourceName;
    private String originalName;
    private String fileUrl;

    public ExamCrawlDto(String sourceName, String originalName, String fileUrl) {
        this.sourceName = sourceName;
        this.originalName = originalName;
        this.fileUrl = fileUrl;
    }
}
