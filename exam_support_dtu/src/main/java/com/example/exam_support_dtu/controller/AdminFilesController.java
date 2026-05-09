package com.example.exam_support_dtu.controller;

import com.example.exam_support_dtu.entity.ExamRoom;
import com.example.exam_support_dtu.entity.ExamSchedule;
import com.example.exam_support_dtu.entity.Files;
import com.example.exam_support_dtu.enums.FileStatus;
import com.example.exam_support_dtu.repository.ExamInterestRepository;
import com.example.exam_support_dtu.repository.ExamOriginalRepository;
import com.example.exam_support_dtu.repository.ExamScheduleRepository;
import com.example.exam_support_dtu.repository.FilesRepository;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Controller
public class AdminFilesController {
    private final FilesRepository filesRepository;
    private final ExamScheduleRepository examScheduleRepository;
    private final ExamOriginalRepository examOriginalRepository;
    private final ExamInterestRepository examInterestRepository;

    public AdminFilesController(FilesRepository filesRepository, ExamScheduleRepository examScheduleRepository,
            ExamOriginalRepository examOriginalRepository, ExamInterestRepository examInterestRepository) {
        this.filesRepository = filesRepository;
        this.examScheduleRepository = examScheduleRepository;
        this.examOriginalRepository = examOriginalRepository;
        this.examInterestRepository = examInterestRepository;
    }

    // =========================================================
    // 1. HIỂN THỊ GIAO DIỆN (THYMELEAF)
    // =========================================================
    @GetMapping("/admin/files")
    public String showFilesPage(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) Integer minInterest,
            @RequestParam(required = false) String sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size,
            Model model) {

        // Sắp xếp tùy theo tham số sort (dùng tên cột SQL vì là native query)
        Sort sortOrder;
        if ("newest".equals(sort)) {
            sortOrder = Sort.by("uploaded_at").descending();
        } else if ("largest".equals(sort)) {
            sortOrder = Sort.by("file_size").descending();
        } else {
            sortOrder = Sort.by("id").ascending();
        }
        Pageable pageable = PageRequest.of(page, size, sortOrder);

        // 2. Lấy dữ liệu qua Filter (dùng query riêng nếu sort theo lượt quan tâm)
        Page<Files> filePage;
        if ("interest".equals(sort)) {
            Pageable unsortedPageable = PageRequest.of(page, size);
            filePage = filesRepository.searchAndFilterFilesByInterest(search, type, minInterest, unsortedPageable);
        } else {
            filePage = filesRepository.searchAndFilterFiles(search, type, minInterest, pageable);
        }

        // 5. Cập nhật 4 ô Stat-boxes (Giả sử bạn đã có các hàm này trong Repo)
        long totalFiles = examOriginalRepository.count();
        long downloadedFiles = examOriginalRepository.countByStatusIn(List.of("downloaded", "parsed"));
        long parsedFiles = examOriginalRepository.countByStatus(FileStatus.parsed);
        long newUpdatedFiles = examOriginalRepository.countByFoundAtAfter(LocalDateTime.now().minusDays(7));

        double syncRate = 0;
        if (totalFiles > 0) {
            syncRate = (double) parsedFiles / totalFiles * 100;
        }

        model.addAttribute("syncRate", String.format("%.1f", syncRate));
        model.addAttribute("filePage", filePage);
        model.addAttribute("totalFiles", totalFiles);
        model.addAttribute("downloadedFiles", downloadedFiles);
        model.addAttribute("parsedFiles", parsedFiles);
        model.addAttribute("newUpdatedFiles", newUpdatedFiles);

        model.addAttribute("filePage", filePage);
        return "admin-files";
    }

    // =========================================================
    // 2. XEM CHI TIẾT FILE (API TRẢ JSON CHO MODAL)
    // =========================================================
    @GetMapping("/api/admin/files/{id}")
    @ResponseBody
    public ResponseEntity<Files> getFileDetail(@PathVariable Long id) {
        Optional<Files> fileOptional = filesRepository.findById(id);
        if (fileOptional.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(fileOptional.get());
    }

    // =========================================================
    // 3. TẢI FILE XUỐNG (DOWNLOAD)
    // =========================================================
    @GetMapping("/api/admin/files/download/{id}")
    public ResponseEntity<Resource> downloadFile(@PathVariable Long id) {
        Optional<Files> fileOptional = filesRepository.findById(id);
        if (fileOptional.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Files dbFile = fileOptional.get();
        File physicalFile = new File(dbFile.getFilePath());

        if (!physicalFile.exists()) {
            return ResponseEntity.notFound().build();
        }

        Resource resource = new FileSystemResource(physicalFile);

        // 1. XỬ LÝ FALLBACK TÊN FILE (Nếu original_name bị null)
        String fileName = dbFile.getOriginalName();
        if (fileName == null || fileName.trim().isEmpty()) {
            // Lấy đuôi file từ Enum (nếu có)
            String ext = (dbFile.getExtension() != null) ? dbFile.getExtension().name().toLowerCase() : "bin";
            fileName = "tai_lieu_so_" + id + "." + ext;
        }

        // 2. MÃ HÓA TÊN FILE (Chống lỗi font tiếng Việt và dấu cách khi tải qua trình
        // duyệt)
        String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8).replaceAll("\\+", "%20");

        // 3. Trả về Header chuẩn RFC 5987 để ép tải xuống với tên file chính xác
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encodedFileName)
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .contentLength(physicalFile.length())
                .body(resource);
    }

    // =========================================================
    // 4. XÓA FILE (XÓA DB + XÓA Ổ CỨNG)
    // =========================================================
    @DeleteMapping("/api/admin/files/{id}")
    @ResponseBody
    public ResponseEntity<?> deleteFile(@PathVariable Long id) {
        Optional<Files> fileOptional = filesRepository.findById(id);
        if (fileOptional.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Files dbFile = fileOptional.get();

        // LƯU Ý KỸ: Tính năng dọn dẹp ổ cứng (Xóa file vật lý)
        try {
            File physicalFile = new File(dbFile.getFilePath());
            if (physicalFile.exists()) {
                physicalFile.delete(); // Xóa file thật trên server để đỡ tốn dung lượng
            }
        } catch (Exception e) {
            System.out.println("Cảnh báo: Không thể xóa file vật lý: " + e.getMessage());
        }

        // Xóa thông tin trong Database
        filesRepository.deleteById(id);

        return ResponseEntity.ok().body("{\"message\": \"Xóa file thành công!\"}");
    }

    // =========================================================
    // 5. XEM TRƯỚC FILE (PREVIEW TRÊN TAB MỚI)
    // =========================================================
    @GetMapping("/api/admin/files/view/{id}")
    public ResponseEntity<Resource> viewFile(@PathVariable Long id) {
        Optional<Files> fileOptional = filesRepository.findById(id);
        if (fileOptional.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Files dbFile = fileOptional.get();
        File physicalFile = new File(dbFile.getFilePath());

        if (!physicalFile.exists()) {
            return ResponseEntity.notFound().build();
        }

        Resource resource = new FileSystemResource(physicalFile);
        MediaType mediaType = MediaType.APPLICATION_OCTET_STREAM;

        // Gọi .name() để chuyển Enum FileType thành String trước khi so sánh
        if (dbFile.getExtension() != null && "pdf".equalsIgnoreCase(dbFile.getExtension().name())) {
            mediaType = MediaType.APPLICATION_PDF;
        }

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + dbFile.getOriginalName() + "\"")
                .contentType(mediaType)
                .contentLength(physicalFile.length())
                .body(resource);
    }

    // =========================================================
    // 6. API LẤY DỮ LIỆU ĐÃ PARSE (CHUẨN HÓA THEO DATABASE)
    // =========================================================
    @GetMapping("/api/admin/files/{fileId}/parsed-details")
    @ResponseBody
    public ResponseEntity<?> getParsedFileDetails(@PathVariable Long fileId) {

        List<ExamSchedule> schedules = examScheduleRepository.findByFileId(fileId);
        if (schedules.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        ExamSchedule schedule = schedules.get(0);
        List<Object> roomList = new ArrayList<>();

        if (schedule.getRooms() != null) {
            for (ExamRoom r : schedule.getRooms()) {
                roomList.add(new Object() {
                    public final Long id = r.getId();
                    public final String roomName = r.getRoomName();
                    public final Integer capacity = r.getCapacity();
                    public final String time = r.getExamTime();
                    // Đưa location và examDate về đúng vị trí của nó là ở trong từng phòng thi
                    public final String location = r.getLocation();
                    public final String date = r.getExamDate() != null ? r.getExamDate().toString() : "Chưa cập nhật";
                });
            }
        }

        // Header giờ đây sẽ map chuẩn 100% với bảng exam_schedule
        Object responseData = new Object() {
            public final String courseCode = schedule.getCourseCode();
            public final String courseName = schedule.getCourseName();
            public final Integer credit = schedule.getCredit(); // Thêm Tín chỉ
            public final String semester = schedule.getSemester(); // Thêm Học kỳ
            public final List<Object> rooms = roomList;
        };

        return ResponseEntity.ok(responseData);
    }
}
