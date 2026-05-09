package com.example.exam_support_dtu.controller;

import com.example.exam_support_dtu.entity.AuditLog;
import com.example.exam_support_dtu.entity.ExamOriginal;
import com.example.exam_support_dtu.enums.FileStatus;
import com.example.exam_support_dtu.repository.AuditLogRepository;
import com.example.exam_support_dtu.repository.ExamOriginalRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.ui.Model;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class AdminController {
    private final ExamOriginalRepository examOriginalRepository;
    private final AuditLogRepository auditLogRepository;
    private final com.example.exam_support_dtu.repository.ExamInterestRepository examInterestRepository;
    private final com.example.exam_support_dtu.repository.EmailLogRepository emailLogRepository;

    public AdminController(ExamOriginalRepository examOriginalRepository,
            AuditLogRepository auditLogRepository,
            com.example.exam_support_dtu.repository.ExamInterestRepository examInterestRepository,
            com.example.exam_support_dtu.repository.EmailLogRepository emailLogRepository) {
        this.examOriginalRepository = examOriginalRepository;
        this.auditLogRepository = auditLogRepository;
        this.examInterestRepository = examInterestRepository;
        this.emailLogRepository = emailLogRepository;
    }

    @GetMapping("/admin/dashboard")
    public String showDashboard(@RequestParam(defaultValue = "0") int page, // Nhận số trang từ URL (mặc định trang 0)
            Model model) {

        // 1. Cấu hình phân trang: Trang 'page', mỗi trang 15 phần tử, sắp xếp mới nhất
        Pageable pageable = PageRequest.of(page, 15, Sort.by(Sort.Direction.ASC, "foundAt"));
        // 2. Dùng findAll(Pageable) -> Nó sẽ trả về đối tượng Page chứa 5 file
        Page<ExamOriginal> filePage = examOriginalRepository.findAll(pageable);

        long totalFiles = filePage.getTotalElements(); // Lấy tổng số file trong DB
        long totalParsed = examOriginalRepository.countByStatus(FileStatus.parsed);

        // 4. Tính tỷ lệ đồng bộ
        double syncRate = 0;
        if (totalFiles > 0) {
            syncRate = (double) totalParsed / totalFiles * 100;
        }

        List<AuditLog> recentLogs = auditLogRepository.findTop10ByOrderByCreatedAtDesc();

        // 3. Truyền dữ liệu ra HTML
        model.addAttribute("filePage", filePage);
        model.addAttribute("totalFiles", totalFiles);
        model.addAttribute("syncRate", String.format("%.1f", syncRate)); // Format 1 chữ số thập phân

        // Lấy dữ liệu thực từ database thay vì giả lập
        long totalStudents = examInterestRepository.count(); // Tổng số lượt sinh viên quan tâm
        long totalMails = emailLogRepository.countByStatus("sent"); // Tổng số email đã gửi thành công

        model.addAttribute("totalStudents", totalStudents);
        model.addAttribute("totalMails", totalMails);

        model.addAttribute("recentLogs", recentLogs);

        return "admin-dashboard";
    }

    @GetMapping("/api/admin/logs")
    @org.springframework.web.bind.annotation.ResponseBody
    public Page<AuditLog> getLogs(@RequestParam(defaultValue = "0") int page) {
        Pageable pageable = PageRequest.of(page, 10, Sort.by(Sort.Direction.DESC, "createdAt"));
        return auditLogRepository.findAll(pageable);
    }
}
