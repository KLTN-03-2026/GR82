package com.example.exam_support_dtu.controller;

import com.example.exam_support_dtu.dto.UserExamResultDto;
import com.example.exam_support_dtu.entity.ExamOriginal;
import com.example.exam_support_dtu.repository.ExamOriginalRepository;
import com.example.exam_support_dtu.repository.ExamScheduleRepository;
import com.example.exam_support_dtu.repository.ExamStudentRepository;
import com.example.exam_support_dtu.repository.FilesRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.core.io.Resource;
import org.springframework.core.io.FileSystemResource;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.HashSet;

@Controller
public class UserController {

    private final ExamScheduleRepository examScheduleRepository;
    private final ExamStudentRepository examStudentRepository;
    private final FilesRepository filesRepository;
    private final ExamOriginalRepository examOriginalRepository;
    private final com.example.exam_support_dtu.repository.ExamInterestRepository examInterestRepository;
    private final com.example.exam_support_dtu.repository.DocumentRepository documentRepository;

    public UserController(ExamScheduleRepository examScheduleRepository,
            ExamStudentRepository examStudentRepository,
            FilesRepository filesRepository,
            ExamOriginalRepository examOriginalRepository,
            com.example.exam_support_dtu.repository.ExamInterestRepository examInterestRepository,
            com.example.exam_support_dtu.repository.DocumentRepository documentRepository) {
        this.examScheduleRepository = examScheduleRepository;
        this.examStudentRepository = examStudentRepository;
        this.filesRepository = filesRepository;
        this.examOriginalRepository = examOriginalRepository;
        this.examInterestRepository = examInterestRepository;
        this.documentRepository = documentRepository;
    }

    // =========================================================
    // 1. HIỂN THỊ TRANG CHỦ USER
    // =========================================================
    @GetMapping("/user/home")
    public String showUserHome(
            @RequestParam(defaultValue = "0") int page,
            @org.springframework.security.core.annotation.AuthenticationPrincipal com.example.exam_support_dtu.entity.Users currentUser,
            Model model) {
        int size = 15;

        long totalSchedules = examScheduleRepository.count();
        long totalStudents = examStudentRepository.count();
        long totalDocuments = filesRepository.count();

        model.addAttribute("totalSchedules", totalSchedules);
        model.addAttribute("totalStudents", totalStudents);
        model.addAttribute("totalDocuments", totalDocuments);

        // Lấy danh sách ID đã quan tâm
        Set<Long> followedScheduleIds = new HashSet<>();
        Set<Long> followedOriginalIds = new HashSet<>();
        List<com.example.exam_support_dtu.entity.ExamInterest> sidebarInterests = new ArrayList<>();
        List<com.example.exam_support_dtu.entity.Documents> myDocs = new ArrayList<>();

        if (currentUser != null) {
            List<com.example.exam_support_dtu.entity.ExamInterest> interests = examInterestRepository
                    .findAllByUserId(currentUser.getId());
            sidebarInterests = interests;
            for (com.example.exam_support_dtu.entity.ExamInterest ei : interests) {
                if (ei.getExamSchedule() != null) {
                    followedScheduleIds.add(ei.getExamSchedule().getId());
                }
                if (ei.getExamOriginal() != null) {
                    followedOriginalIds.add(ei.getExamOriginal().getId());
                }
            }
            // Lấy thêm 10 tài liệu mới nhất của user để hỗ trợ cuộn
            myDocs = documentRepository.findByUserIdOrderByCreatedAtDesc(currentUser.getId());
            if (myDocs.size() > 10) {
                myDocs = myDocs.subList(0, 10);
            }
        }
        model.addAttribute("followedScheduleIds", followedScheduleIds);
        model.addAttribute("followedOriginalIds", followedOriginalIds);
        model.addAttribute("sidebarInterests", sidebarInterests);
        model.addAttribute("mySidebarDocs", myDocs);

        // Lấy danh sách tệp gốc mới nhất (15 tệp mỗi trang)
        org.springframework.data.domain.Page<com.example.exam_support_dtu.entity.ExamOriginal> originalPage = examOriginalRepository
                .findAll(
                        PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "foundAt")));

        model.addAttribute("latestSchedules", originalPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", originalPage.getTotalPages());
        model.addAttribute("totalItems", originalPage.getTotalElements());

        model.addAttribute("studentCode", "");
        model.addAttribute("semester", "");
        model.addAttribute("courseFilter", "");
        model.addAttribute("examResults", new ArrayList<UserExamResultDto>());
        model.addAttribute("totalResults", 0);

        return "user-home";
    }

    // =========================================================
    // 2. XEM TRƯỚC TỆP PDF
    // =========================================================
    @GetMapping("/api/user/files/view/{id}")
    @ResponseBody
    public ResponseEntity<Resource> viewFile(@PathVariable Long id) {
        Optional<com.example.exam_support_dtu.entity.Files> fileOptional = filesRepository.findById(id);
        if (fileOptional.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        com.example.exam_support_dtu.entity.Files dbFile = fileOptional.get();
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
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=\"" + dbFile.getOriginalName() + "\"")
                .contentType(mediaType)
                .contentLength(physicalFile.length())
                .body(resource);
    }
}