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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Controller
public class UserDocumentsController {

    private final FilesRepository filesRepository;
    private final ExamScheduleRepository examScheduleRepository;

    public UserDocumentsController(FilesRepository filesRepository,
                                   ExamScheduleRepository examScheduleRepository) {
        this.filesRepository = filesRepository;
        this.examScheduleRepository = examScheduleRepository;
    }

    // =========================================================
    // 1. HIỂN THỊ TRANG TÀI LIỆU USER
    // =========================================================
    @GetMapping("/user/documents")
    public String showUserDocumentsPage(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Model model) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());
        Page<Files> filePage = filesRepository.findAll(pageable);

        model.addAttribute("filePage", filePage);

        return "user-documents";
    }

    // =========================================================
    // 2. XEM CHI TIẾT TÀI LIỆU
    // =========================================================
    @GetMapping("/api/user/documents/{id}")
    @ResponseBody
    public ResponseEntity<Files> getDocumentDetail(@PathVariable Long id) {

        Optional<Files> fileOptional = filesRepository.findById(id);

        if (fileOptional.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(fileOptional.get());
    }

    // =========================================================
    // 3. TẢI TÀI LIỆU XUỐNG
    // =========================================================
    @GetMapping("/api/user/documents/download/{id}")
    public ResponseEntity<Resource> downloadDocument(@PathVariable Long id) {

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

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + dbFile.getOriginalName() + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .contentLength(physicalFile.length())
                .body(resource);
    }

    // =========================================================
    // 4. XEM TRƯỚC TÀI LIỆU
    // =========================================================
    @GetMapping("/api/user/documents/view/{id}")
    public ResponseEntity<Resource> viewDocument(@PathVariable Long id) {

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
    // 5. XEM DỮ LIỆU LỊCH THI ĐÃ PARSE TỪ TÀI LIỆU
    // =========================================================
    @GetMapping("/api/user/documents/{fileId}/parsed-details")
    @ResponseBody
    public ResponseEntity<?> getDocumentParsedDetails(@PathVariable Long fileId) {

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
                    public final String location = r.getLocation();
                    public final String date = r.getExamDate() != null ? r.getExamDate().toString() : "Chưa cập nhật";
                });
            }
        }

        Object responseData = new Object() {
            public final String courseCode = schedule.getCourseCode();
            public final String courseName = schedule.getCourseName();
            public final Integer credit = schedule.getCredit();
            public final String semester = schedule.getSemester();
            public final List<Object> rooms = roomList;
        };

        return ResponseEntity.ok(responseData);
    }
}