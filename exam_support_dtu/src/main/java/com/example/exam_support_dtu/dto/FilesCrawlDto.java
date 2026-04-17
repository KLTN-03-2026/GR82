package com.example.exam_support_dtu.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor

public class FilesCrawlDto {
    private String sourceName;
    private String fileOriginalName;
    private String downloadUrl;

    public FilesCrawlDto(String sourceName, String fileOriginalName, String downloadUrl) {
        if (downloadUrl == null || downloadUrl.trim().isEmpty()) {
            throw new IllegalArgumentException("fileUrl must not be null or empty");
        }
        this.sourceName = sourceName;
        this.fileOriginalName = fileOriginalName;
        this.downloadUrl = downloadUrl;
    }
}
