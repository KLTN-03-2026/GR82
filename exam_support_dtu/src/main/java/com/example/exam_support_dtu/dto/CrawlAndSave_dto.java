package com.example.exam_support_dtu.dto;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class CrawlAndSave_dto {


    //lay tu examcrawl dto
    private String originalName;
    private String fileUrl;

    //file crawl dot
    private String sourceName;
    private String fileOriginalName;
    private String dowloadUrl;

    //download dto
    private String storedName;
    private String extension;
    private String filePath;
    private Long fileSize;

    public CrawlAndSave_dto(String originalName,
                            String fileUrl,
                            String sourceName,
                            String fileOriginalName,
                            String dowloadUrl,
                            String storedName,
                            String extension,
                            String filePath,
                            Long fileSize) {
        this.originalName = originalName;
        this.fileUrl = fileUrl;
        this.sourceName = sourceName;
        this.fileOriginalName = fileOriginalName;
        this.dowloadUrl = dowloadUrl;
        this.storedName = storedName;
        this.extension = extension;
        this.filePath = filePath;
        this.fileSize = fileSize;
    }
}
