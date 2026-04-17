package com.example.exam_support_dtu.controller;

import com.example.exam_support_dtu.entity.ExamRoom;
import com.example.exam_support_dtu.entity.ExamSchedule;
import com.example.exam_support_dtu.entity.Files;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Controller
public class AdminFilesController {
    private final FilesRepository filesRepository;
    private final ExamScheduleRepository examScheduleRepository;

    public AdminFilesController(FilesRepository filesRepository, ExamScheduleRepository examScheduleRepository) {
        this.filesRepository = filesRepository;
        this.examScheduleRepository = examScheduleRepository;
    }


    // =========================================================
    // 1. HIỂN THỊ GIAO DIỆN (THYMELEAF)
    // =========================================================
    @GetMapping("/admin/files")
    public String showFilesPage(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Model model) {

        // Sắp xếp file mới nhất lên đầu bảng
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());
        Page<Files> filePage = filesRepository.findAll(pageable);

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

        // Nếu file vật lý trên ổ cứng đã bị xóa mất
        if (!physicalFile.exists()) {
            return ResponseEntity.notFound().build();
        }

        Resource resource = new FileSystemResource(physicalFile);

        // Báo cho trình duyệt biết đây là file đính kèm để ép tải xuống
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + dbFile.getOriginalName() + "\"")
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

        // ĐÃ FIX: Gọi .name() để chuyển Enum FileType thành String trước khi so sánh
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
            public final Integer credit = schedule.getCredit();     // Thêm Tín chỉ
            public final String semester = schedule.getSemester();  // Thêm Học kỳ
            public final List<Object> rooms = roomList;
        };

        return ResponseEntity.ok(responseData);
    }
}
