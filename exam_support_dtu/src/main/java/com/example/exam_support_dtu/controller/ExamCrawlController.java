package com.example.exam_support_dtu.controller;

import com.example.exam_support_dtu.entity.*;
import com.example.exam_support_dtu.enums.FileStatus;
import com.example.exam_support_dtu.enums.FileType;
import com.example.exam_support_dtu.repository.ExamOriginalRepository;
import com.example.exam_support_dtu.repository.FilesRepository;
import com.example.exam_support_dtu.service.ExamCrawlService;
import com.example.exam_support_dtu.service.ExcelParserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.sql.DataSource;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.sql.Connection;
import java.util.List;

@RestController
public class ExamCrawlController {
    private final ExamCrawlService examCrawlService;
    private final ExcelParserService excelParserService;
    private final FilesRepository filesRepository;
    private final ExamOriginalRepository examOriginalRepository; // Inject thêm cái này
    public ExamCrawlController(ExamCrawlService examCrawlService, ExcelParserService excelParserService, FilesRepository filesRepository, ExamOriginalRepository examOriginalRepository) {
        this.examCrawlService = examCrawlService;
        this.excelParserService =  excelParserService;
        this.filesRepository = filesRepository;
        this.examOriginalRepository = examOriginalRepository;
    }



    @GetMapping("/read-local")
    public ResponseEntity<String> readLocalFile() {
        String path = "D:\\Tracuulich\\exam_support_dtu\\uploads\\exam_files\\1769527618896_1ce9380f_DS_CHI_168_AS-AU-AW-E-G-I-K-Q-S.xls";
        File file = new File(path);

        if (!file.exists()) return ResponseEntity.badRequest().body("❌ File không tồn tại!");

        try (FileInputStream fis = new FileInputStream(file)) {
            String fileName = file.getName();

            // 1. XỬ LÝ FILES (Metadata)
            // Tìm trong DB xem file này đã lưu chưa dựa vào tên
            Files mockFile = filesRepository.findByStoredName(fileName)
                    .orElseGet(() -> {
                        Files newFile = new Files();
                        newFile.setOriginalName(fileName);
                        newFile.setStoredName(fileName); // Dùng tên file làm stored_name
                        newFile.setFilePath(path);
                        // --- SỬA ĐOẠN NÀY: TỰ ĐỘNG CHECK ĐUÔI FILE ---
                        if (fileName.toLowerCase().endsWith(".xls")) {
                            newFile.setExtension(com.example.exam_support_dtu.enums.FileType.xls);
                        } else {
                            // Mặc định là XLSX cho các trường hợp còn lại (.xlsx, .xlsm...)
                            newFile.setExtension(com.example.exam_support_dtu.enums.FileType.xlsx);
                        }
                        newFile.setFileSize(file.length());
                        return filesRepository.save(newFile);
                    });

            // 2. XỬ LÝ EXAM ORIGINAL (CỐT LÕI CỦA VIỆC KHÔNG BỊ MẤT DỮ LIỆU)
            // Tìm xem ExamOriginal của file này đã có chưa?
            // Nếu có rồi -> Lấy ID cũ (Service sẽ update đè lên cái cũ này)
            // Nếu chưa -> Tạo mới (Service sẽ tạo bản ghi mới, KHÔNG đụng đến file khác)
            ExamOriginal mockExamOriginal = examOriginalRepository.findAll().stream()
                    .filter(e -> e.getFileOriginalName() != null && e.getFileOriginalName().equals(fileName))
                    .findFirst()
                    .orElseGet(() -> {
                        ExamOriginal newExam = new ExamOriginal();
                        newExam.setFileOriginalName(fileName);

                        // --- SỬA LỖI TẠI ĐÂY: Thêm dữ liệu giả cho các trường NOT NULL ---
                        // Vì đang test local, ta lấy luôn đường dẫn file làm URL giả
                        newExam.setFileUrl(path);      // <--- QUAN TRỌNG: Cần dòng này để fix lỗi
                        newExam.setDownloadUrl(path);  // Set luôn cái này cho chắc nếu DB yêu cầu
                        newExam.setFileType(FileType.xlsx);    // Set loại file
                        newExam.setStatus(FileStatus.pending);   // Trạng thái mặc định
                        // -----------------------------------------------------------------

                        return examOriginalRepository.save(newExam);
                    });

            // Gọi Service
            String result = excelParserService.parseAndSave(fis, mockExamOriginal, mockFile);

            return ResponseEntity.ok("File: " + fileName + "\n" + result);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("❌ Lỗi: " + e.getMessage());
        }
    }

    // API Cào X trang
    // URL ví dụ: /api/crawl/pages?total=5 (Cào 5 trang đầu tiên)
    @GetMapping("/api/crawl/pages")
    public String crawlPages(@RequestParam(defaultValue = "1") int total) {
        // Gọi hàm loop mới
        return examCrawlService.CrawlMultiplePages(5);
    }
}
