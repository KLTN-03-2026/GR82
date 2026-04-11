package com.example.exam_support_dtu.service;

import com.example.exam_support_dtu.entity.Files;
import com.example.exam_support_dtu.enums.FileType;
import com.example.exam_support_dtu.repository.FilesRepository;
import org.springframework.stereotype.Service;

@Service
public class FileStorageService {
    private final FilesRepository frepo;

    // Đường dẫn lưu file (Public để bên CrawlService lấy dùng cho đồng bộ)
    public static final String UPLOAD_DIR = "D:\\Tracuulich\\exam_support_dtu\\uploads\\exam_files\\";

    public FileStorageService(FilesRepository frepo) {
        this.frepo = frepo;
        // Tạo thư mục nếu chưa có
        java.io.File dir = new java.io.File(UPLOAD_DIR);
        if (!dir.exists()) dir.mkdirs();
    }

    public Files saveFile(String sourceName,
                          String originalName,
                          String storedName,
                          FileType ext,
                          String filePath,
                          Long fileSize,
                          String checksum) {

        // 1. Nếu đã có file cùng checksum (nội dung trùng) → Trả về file cũ trong DB
        Files existed = frepo.findByChecksum(checksum);
        if (existed != null) {
            return existed;
        }

        // 2. Nếu chưa có → Lưu file mới
        Files f = new Files();
        f.setSourceName(sourceName);
        f.setOriginalName(originalName);
        f.setStoredName(storedName);
        f.setExtension(ext);
        f.setFilePath(filePath);
        f.setFileSize(fileSize);
        f.setChecksum(checksum);

        // set thêm thời gian upload/người upload nếu cần
        f.setUploadedAt(java.time.LocalDateTime.now());

        return frepo.save(f);
    }

    // Getter để bên CrawlService lấy đường dẫn gốc
    public String getUploadDir() {
        return UPLOAD_DIR;
    }
}