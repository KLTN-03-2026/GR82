package com.example.exam_support_dtu.dto;


import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor

public class DownloadFileDto {
    private String storedName;
    private String extension;
    private String filePath;
    private Long fileSize;

    public DownloadFileDto(
            String storedName,
            String extension,
            String filePath,
            Long fileSize
    ) {
        this.storedName = storedName;
        this.extension = extension;
        this.filePath = filePath;
        this.fileSize = fileSize;
    }
}

