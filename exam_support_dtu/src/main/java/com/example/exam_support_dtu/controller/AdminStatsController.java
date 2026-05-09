package com.example.exam_support_dtu.controller;

import com.example.exam_support_dtu.dto.DashboardStatsDto;
import com.example.exam_support_dtu.repository.*;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Controller
public class AdminStatsController {

    private final PageVisitRepository pageVisitRepository;
    private final UserRepository userRepository;
    private final ExamInterestRepository examInterestRepository;
    private final DocumentRepository documentRepository;
    private final EmailLogRepository emailLogRepository;
    private final ExamOriginalRepository examOriginalRepository;
    private final AuditLogRepository auditLogRepository;

    public AdminStatsController(PageVisitRepository pageVisitRepository,
                                UserRepository userRepository,
                                ExamInterestRepository examInterestRepository,
                                DocumentRepository documentRepository,
                                EmailLogRepository emailLogRepository,
                                ExamOriginalRepository examOriginalRepository,
                                AuditLogRepository auditLogRepository) {
        this.pageVisitRepository = pageVisitRepository;
        this.userRepository = userRepository;
        this.examInterestRepository = examInterestRepository;
        this.documentRepository = documentRepository;
        this.emailLogRepository = emailLogRepository;
        this.examOriginalRepository = examOriginalRepository;
        this.auditLogRepository = auditLogRepository;
    }

    @GetMapping("/admin/stats")
    public String showAdminStats(org.springframework.ui.Model model) {
        model.addAttribute("recentLogs", auditLogRepository.findTop10ByOrderByCreatedAtDesc());
        return "admin-stats";
    }

    @GetMapping("/api/admin/dashboard/stats")
    @ResponseBody
    public ResponseEntity<DashboardStatsDto> getDashboardStats(@RequestParam(defaultValue = "7") int days) {
        LocalDateTime since = LocalDateTime.now().minusDays(days);
        if (days == 0) since = LocalDateTime.of(1970, 1, 1, 0, 0);

        DashboardStatsDto dto = new DashboardStatsDto();

        // 1. Stat Boxes
        dto.setTotalPageViews(pageVisitRepository.countByVisitTimeAfter(since));
        dto.setNewUsers(userRepository.countByCreatedAtAfter(since.atOffset(java.time.ZoneOffset.UTC)));
        dto.setNewInterests(examInterestRepository.countByCreatedAtAfter(since.atOffset(java.time.ZoneOffset.UTC)));
        dto.setPendingDocuments(documentRepository.countByStatus("PENDING"));
        dto.setFailedEmails(emailLogRepository.countByStatus("failed"));
        dto.setCrawlErrors(examOriginalRepository.countByStatus(com.example.exam_support_dtu.enums.FileStatus.error));

        // 2. Doughnut Chart
        dto.setApprovedDocs(documentRepository.countByStatus("APPROVED"));
        dto.setPendingDocs(documentRepository.countByStatus("PENDING"));
        dto.setRejectedDocs(documentRepository.countByStatus("REJECTED"));

        // 3. Line Chart Data (Last X days)
        int chartDays = (days == 0) ? 30 : days; // Default to 30 if "All" is selected
        List<String> labels = new ArrayList<>();
        List<Long> pageViewData = new ArrayList<>();
        List<Long> userData = new ArrayList<>();
        List<Long> interestData = new ArrayList<>();

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM");
        LocalDate today = LocalDate.now();

        for (int i = chartDays - 1; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            labels.add(date.format(formatter));
            
            LocalDateTime startOfDay = date.atStartOfDay();
            LocalDateTime endOfDay = date.atTime(23, 59, 59);

            pageViewData.add(pageVisitRepository.countByVisitTimeBetween(startOfDay, endOfDay));
            userData.add(userRepository.countByCreatedAtBetween(startOfDay.atOffset(java.time.ZoneOffset.UTC), endOfDay.atOffset(java.time.ZoneOffset.UTC)));
            interestData.add(examInterestRepository.countByCreatedAtBetween(startOfDay.atOffset(java.time.ZoneOffset.UTC), endOfDay.atOffset(java.time.ZoneOffset.UTC)));
        }

        dto.setLabels(labels);
        dto.setPageViewData(pageViewData);
        dto.setUserData(userData);
        dto.setInterestData(interestData);

        return ResponseEntity.ok(dto);
    }
}
