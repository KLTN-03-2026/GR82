package com.example.exam_support_dtu.controller;

import com.example.exam_support_dtu.entity.EmailLog;
import com.example.exam_support_dtu.entity.EmailTemplate;
import com.example.exam_support_dtu.entity.SystemSetting;
import com.example.exam_support_dtu.repository.EmailLogRepository;
import com.example.exam_support_dtu.repository.EmailTemplateRepository;
import com.example.exam_support_dtu.repository.SystemSettingRepository;
import com.example.exam_support_dtu.service.EmailService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Controller
public class AdminEmailController {
    private final EmailLogRepository emailLogRepository;
    private final EmailTemplateRepository emailTemplateRepository;
    private final SystemSettingRepository systemSettingRepository;
    private final EmailService emailService;

    public AdminEmailController(EmailLogRepository emailLogRepository,
            EmailTemplateRepository emailTemplateRepository,
            SystemSettingRepository systemSettingRepository,
            EmailService emailService) {
        this.emailLogRepository = emailLogRepository;
        this.emailTemplateRepository = emailTemplateRepository;
        this.systemSettingRepository = systemSettingRepository;
        this.emailService = emailService;
    }

    @GetMapping("/admin/emails")
    public String showEmailManagement(@RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size,
            Model model) {

        // 1. Lấy dữ liệu cho 4 ô thống kê
        long totalSent = emailLogRepository.countByStatus("sent");
        long totalPending = emailLogRepository.countByStatus("pending");
        long totalFailed = emailLogRepository.countByStatus("failed");
        long totalTemplates = emailTemplateRepository.count();

        // 2. Lấy cấu hình hệ thống (Số ngày báo trước mặc định)
        SystemSetting notifySetting = systemSettingRepository.findById("mail.default_notify_days").orElse(null);
        String notifyDays = (notifySetting != null) ? notifySetting.getValue() : "3"; // Mặc định là 3 ngày

        // Lấy trạng thái công tắc Auto-Mail
        SystemSetting autoMailSetting = systemSettingRepository.findById("mail.auto_enabled").orElse(null);
        boolean isAutoMailOn = autoMailSetting == null || Boolean.parseBoolean(autoMailSetting.getValue());

        // 3. Lấy danh sách Mẫu Email (Cột phải)
        List<EmailTemplate> templates = emailTemplateRepository.findAll(Sort.by(Sort.Direction.ASC, "id"));

        // 4. Lấy danh sách Lịch sử gửi mail (Cột trái - Phân trang, mới nhất lên đầu)
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "sentAt"));
        Page<EmailLog> emailLogPage = emailLogRepository.findAll(pageable);

        // 5. Đẩy toàn bộ dữ liệu sang giao diện Thymeleaf
        model.addAttribute("totalSent", totalSent);
        model.addAttribute("totalPending", totalPending);
        model.addAttribute("totalFailed", totalFailed);
        model.addAttribute("totalTemplates", totalTemplates);
        model.addAttribute("notifyDays", notifyDays);
        model.addAttribute("isAutoMailOn", isAutoMailOn);
        model.addAttribute("templates", templates);
        model.addAttribute("emailLogPage", emailLogPage);

        return "admin-emails"; // Tên của file HTML bạn vừa tạo ở trên
    }

    // =========================================================
    // API: LẤY CHI TIẾT 1 MẪU EMAIL ĐỂ ĐỔ VÀO MODAL SỬA
    // =========================================================
    @GetMapping("/api/admin/emails/templates/{id}")
    @ResponseBody
    public ResponseEntity<?> getTemplate(@PathVariable Long id) {
        Optional<EmailTemplate> tpl = emailTemplateRepository.findById(id);
        if (tpl.isEmpty())
            return ResponseEntity.notFound().build();
        return ResponseEntity.ok(tpl.get());
    }

    // =========================================================
    // API: LƯU HOẶC CẬP NHẬT MẪU EMAIL
    // =========================================================
    @PostMapping("/api/admin/emails/templates/save")
    @ResponseBody
    public ResponseEntity<?> saveTemplate(@RequestBody Map<String, String> payload) {
        try {
            EmailTemplate tpl;
            String idStr = payload.get("id");

            if (idStr != null && !idStr.isEmpty()) {
                // Chế độ Sửa (Update)
                tpl = emailTemplateRepository.findById(Long.parseLong(idStr))
                        .orElseThrow(() -> new RuntimeException("Không tìm thấy mẫu email!"));
            } else {
                // Chế độ Thêm Mới (Create)
                tpl = new EmailTemplate();
            }

            // Ép mã Code thành chữ IN HOA và xóa khoảng trắng
            String code = payload.get("code").trim().toUpperCase().replaceAll("\\s+", "_");

            tpl.setCode(code);
            tpl.setName(payload.get("name"));
            tpl.setSubject(payload.get("subject"));
            tpl.setBodyHtml(payload.get("bodyHtml"));

            emailTemplateRepository.save(tpl);
            return org.springframework.http.ResponseEntity.ok("{\"message\": \"Lưu mẫu Email thành công!\"}");
        } catch (Exception e) {
            // Thường lỗi do trùng cột 'code' (Unique constraint)
            return ResponseEntity.badRequest()
                    .body("{\"message\": \"Lỗi: Mã Code này đã tồn tại hoặc dữ liệu không hợp lệ!\"}");
        }
    }

    // =========================================================
    // API: XÓA MẪU EMAIL
    // =========================================================
    @DeleteMapping("/api/admin/emails/templates/{id}")
    @ResponseBody
    public ResponseEntity<?> deleteTemplate(@PathVariable Long id) {
        try {
            // Kiểm tra xem mẫu có tồn tại không
            if (!emailTemplateRepository.existsById(id)) {
                return ResponseEntity.status(404).body(Map.of("message", "Mẫu email không tồn tại!"));
            }

            // Xóa
            // Lịch sử gửi mail (EmailLog) có thể đang chứa template_id trỏ về mẫu này.
            // Phải ngắt liên kết (set null) trước khi xóa mẫu để không bị lỗi DB
            // Constraint.
            List<EmailLog> relatedLogs = emailLogRepository.findByTemplateId(id);
            if (!relatedLogs.isEmpty()) {
                for (EmailLog log : relatedLogs) {
                    log.setTemplateId(null);
                }
                emailLogRepository.saveAll(relatedLogs);
            }

            // Thực hiện xóa mẫu
            emailTemplateRepository.deleteById(id);
            return ResponseEntity.ok(Map.of("message", "Đã xóa mẫu email thành công!"));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("message", "Lỗi khi xóa mẫu email: " + e.getMessage()));
        }
    }

    // =========================================================
    // API: LƯU CẤU HÌNH HỆ THỐNG (SỐ NGÀY BÁO TRƯỚC & AUTO-MAIL)
    // =========================================================
    @PostMapping("/api/admin/emails/config/save")
    @ResponseBody
    public ResponseEntity<?> saveSystemConfig(@RequestBody java.util.Map<String, String> payload) {
        try {
            // 1. Lưu số ngày báo trước
            if (payload.containsKey("notifyDays")) {
                SystemSetting notifySetting = systemSettingRepository.findById("mail.default_notify_days")
                        .orElse(new SystemSetting());
                notifySetting.setKey("mail.default_notify_days");
                notifySetting.setValue(payload.get("notifyDays"));
                notifySetting.setDescription("Số ngày báo trước lịch thi mặc định");
                systemSettingRepository.save(notifySetting);
            }

            // 2. Lưu trạng thái công tắc Auto-Mail
            if (payload.containsKey("autoMailOn")) {
                SystemSetting autoSetting = systemSettingRepository.findById("mail.auto_enabled")
                        .orElse(new SystemSetting());
                autoSetting.setKey("mail.auto_enabled");
                // Lưu thẳng chữ "true" hoặc "false" vào DB
                autoSetting.setValue(payload.get("autoMailOn"));
                autoSetting.setDescription("Trạng thái công tắc gửi mail tự động");
                systemSettingRepository.save(autoSetting);
            }

            return org.springframework.http.ResponseEntity.ok("{\"message\": \"Lưu cấu hình thành công!\"}");
        } catch (Exception e) {
            e.printStackTrace();
            return org.springframework.http.ResponseEntity.badRequest()
                    .body("{\"message\": \"Lỗi khi lưu cấu hình!\"}");
        }
    }

    // =========================================================
    // API: TEST GỬI MAIL
    // =========================================================
    @PostMapping("/api/admin/emails/test-send")
    @ResponseBody
    public ResponseEntity<?> testSendEmail(@RequestBody Map<String, String> payload) {
        try {
            String toEmail = payload.get("email");
            String templateIdStr = payload.get("templateId");

            if (toEmail == null || toEmail.trim().isEmpty() || templateIdStr == null
                    || templateIdStr.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("message", "Vui lòng cung cấp đủ email nhận và ID mẫu email!"));
            }

            Long templateId = Long.parseLong(templateIdStr);

            // Gọi qua EmailService
            boolean success = emailService.sendTestEmail(toEmail, templateId);

            if (success) {
                return ResponseEntity.ok(Map.of("message",
                        "Đã gửi mail test thành công tới " + toEmail + "! Vui lòng kiểm tra hộp thư."));
            } else {
                return ResponseEntity.badRequest()
                        .body(Map.of("message", "Lỗi: Không thể gửi mail. (Có thể sai cấu hình SMTP hoặc lỗi mạng)"));
            }

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500)
                    .body(Map.of("message", "Lỗi máy chủ khi gửi mail test: " + e.getMessage()));
        }
    }

}
