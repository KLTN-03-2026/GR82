package com.example.exam_support_dtu.controller;

import com.example.exam_support_dtu.entity.ExamOriginal;
import com.example.exam_support_dtu.enums.FileStatus;
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

    public AdminController(ExamOriginalRepository examOriginalRepository) {
        this.examOriginalRepository = examOriginalRepository;
    }

    @GetMapping("/admin/dashboard")
    public String showDashboard(@RequestParam(defaultValue = "0") int page, // Nhận số trang từ URL (mặc định trang 0)
                                Model model) {

        // 1. Cấu hình phân trang: Trang 'page', mỗi trang 15 phần tử, sắp xếp mới nhất
        Pageable pageable = PageRequest.of(page, 15, Sort.by(Sort.Direction.DESC, "foundAt"));
        // 2. Dùng findAll(Pageable) -> Nó sẽ trả về đối tượng Page chứa 5 file
        Page<ExamOriginal> filePage = examOriginalRepository.findAll(pageable);

        long totalFiles = filePage.getTotalElements();  // Lấy tổng số file trong DB
        long totalParsed = examOriginalRepository.countByStatus(FileStatus.parsed);

        // 4. Tính tỷ lệ đồng bộ
        double syncRate = 0;
        if (totalFiles > 0) {
            syncRate = (double) totalParsed / totalFiles * 100;
        }

        // 3. Truyền dữ liệu ra HTML
        model.addAttribute("filePage", filePage);
        model.addAttribute("totalFiles", totalFiles);
        model.addAttribute("syncRate", String.format("%.1f", syncRate)); // Format 1 chữ số thập phân

        // Giả lập hoặc gọi Service khác cho SV và Mail (nếu chưa có DB cho phần này)
        model.addAttribute("totalStudents", 12500);
        model.addAttribute("totalMails", 8402);

        return "admin-dashboard";
    }
}
