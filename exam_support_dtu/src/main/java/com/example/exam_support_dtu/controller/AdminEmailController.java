package com.example.exam_support_dtu.controller;

import com.example.exam_support_dtu.annotation.LoggableAction;
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
import java.time.OffsetDateTime;
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

        // 4. Lấy danh sách 10 Lịch sử gửi mail (Cột trái - Phân trang, mới nhất lên đầu)
        Pageable pageable = PageRequest.of(page, 10, Sort.by("sentAt").descending());
        Page<EmailLog> emailPage = emailLogRepository.findAll(pageable); // Bảng lưu lịch sử email

        // 5. Đẩy toàn bộ dữ liệu sang giao diện Thymeleaf
        model.addAttribute("emailPage", emailPage);
        model.addAttribute("totalSent", totalSent);
        model.addAttribute("totalPending", totalPending);
        model.addAttribute("totalFailed", totalFailed);
        model.addAttribute("totalTemplates", totalTemplates);
        model.addAttribute("notifyDays", notifyDays);
        model.addAttribute("isAutoMailOn", isAutoMailOn);
        model.addAttribute("templates", templates);

        return "admin-emails"; // Tên của file HTML bạn vừa tạo ở trên
    }

    // 1. API TRẢ VỀ CHI TIẾT 1 DÒNG LOG CHO MODAL XEM
    @GetMapping("/api/admin/emails/logs/{id}")
    @ResponseBody
    public ResponseEntity<EmailLog> getEmailLogDetail(@PathVariable Long id) {
        Optional<EmailLog> optLog = emailLogRepository.findById(id);
        if (optLog.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(optLog.get());
    }

    // 2. API XỬ LÝ GỬI LẠI (RESEND)
    @PostMapping("/api/admin/emails/resend/{id}")
    @ResponseBody
    @LoggableAction(action = "RESEND_EMAIL", targetType = "EMAIL_LOG", targetId = "#id")
    public ResponseEntity<?> resendFailedEmail(@PathVariable Long id) {
        Optional<EmailLog> optLog = emailLogRepository.findById(id);
        if (optLog.isEmpty()) {
            return ResponseEntity.badRequest().body("Không tìm thấy dữ liệu Log.");
        }

        EmailLog log = optLog.get();
        if (!"failed".equalsIgnoreCase(log.getStatus())) {
            return ResponseEntity.badRequest().body("Email này không ở trạng thái Lỗi.");
        }

        try {
            // LƯU Ý KỸ THUẬT:
            // Vì bảng EmailLog của bạn thiết kế CHỈ LƯU `bodySnippet` (để tiết kiệm DB),
            // Hệ thống KHÔNG CÓ toàn bộ mã HTML gốc để gửi lại 100%.
            // Cách xử lý tốt nhất ở đây là gửi lại dựa vào Text thuần của bodySnippet.

            // Nếu bạn muốn gửi lại ĐÚNG y xì giao diện đỏ chót lúc nãy,
            // Bạn sẽ phải bổ sung cột `fullBody` vào Entity EmailLog trong tương lai nhé!

            emailService.resendEmailLog(log);

            // Xóa log cũ đã bị lỗi (hoặc giữ lại tùy bạn, ở đây mình đổi status cho gọn)
            log.setStatus("sent");
            log.setErrorMessage(null);
            emailLogRepository.save(log);

            return ResponseEntity.ok("Gửi lại thành công!");
        } catch (Exception e) {
            // Nếu gửi lại vẫn lỗi, ghi đè lỗi mới vào
            log.setErrorMessage("Gửi lại thất bại: " + e.getMessage());
            emailLogRepository.save(log);
            return ResponseEntity.badRequest().body("Vẫn không thể gửi. Lỗi: " + e.getMessage());
        }
    }

    // 3. API CHỈNH SỬA VÀ GỬI LẠI (EDIT & RESEND)
    @PostMapping("/api/admin/emails/edit-resend/{id}")
    @ResponseBody
    @LoggableAction(action = "EDIT_RESEND_EMAIL", targetType = "EMAIL_LOG", targetId = "#id", details = "'Sửa và gửi lại tới: ' + #payload['toEmail']")
    public ResponseEntity<?> editAndResendEmail(@PathVariable Long id, @RequestBody Map<String, String> payload) {
        Optional<EmailLog> optLog = emailLogRepository.findById(id);
        if (optLog.isEmpty()) {
            return ResponseEntity.badRequest().body("Không tìm thấy dữ liệu Log.");
        }

        EmailLog log = optLog.get();
        if (!"failed".equalsIgnoreCase(log.getStatus()) && !"pending".equalsIgnoreCase(log.getStatus())) {
            return ResponseEntity.badRequest().body("Chỉ thao tác được trên email Thất bại hoặc Chờ gửi.");
        }

        try {
            log.setToEmail(payload.get("toEmail"));
            log.setSubject(payload.get("subject"));
            log.setBodySnippet(payload.get("bodySnippet"));

            emailService.resendEmailLog(log);
            return ResponseEntity.ok("Đã cập nhật nội dung và Gửi lại thành công!");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Cập nhật và Gửi lại thất bại. Lỗi: " + e.getMessage());
        }
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
    @LoggableAction(action = "SAVE_EMAIL_TEMPLATE", targetType = "EMAIL_TEMPLATE", details = "'Lưu mẫu: ' + #payload['name'] + ' (Code: ' + #payload['code'] + ')'")
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
            return ResponseEntity.ok("{\"message\": \"Lưu mẫu Email thành công!\"}");
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
    @LoggableAction(action = "DELETE_EMAIL_TEMPLATE", targetType = "EMAIL_TEMPLATE", targetId = "#id")
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
    @LoggableAction(action = "UPDATE_EMAIL_CONFIG", targetType = "SYSTEM_CONFIG", details = "'Cập nhật cấu hình: ' + #payload")
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


    // =======================================================
    // API TẠO DỮ LIỆU EMAIL GIẢ (TỪ FORM MOCK)
    // =======================================================
    @PostMapping("/api/admin/emails/mock")
    @ResponseBody
    public ResponseEntity<?> createMockEmail(@RequestBody Map<String, String> payload) {
        String toEmail = payload.get("toEmail");
        String subject = payload.get("subject");
        String status = payload.get("status");

        if (toEmail == null || toEmail.trim().isEmpty()) {
            return ResponseEntity.badRequest().body("Email không được để trống!");
        }

        String finalSubject = subject != null && !subject.isEmpty() ? subject : "[TEST] Không tiêu đề";
        String body = "Kính gửi sinh viên, đây là email mô phỏng do admin tự nhập để test hệ thống.";

        if ("sent".equalsIgnoreCase(status)) {
            try {
                emailService.sendHtmlEmail(toEmail, finalSubject, body);
                return ResponseEntity.ok("Hệ thống đã GỬI THẬT email này và lưu Log thành công!");
            } catch (Exception e) {
                return ResponseEntity.badRequest().body("Gửi mail thật thất bại: " + e.getMessage());
            }
        }

        EmailLog mockLog = new EmailLog();
        mockLog.setToEmail(toEmail);
        mockLog.setSubject(finalSubject);
        mockLog.setBodySnippet(body);
        mockLog.setSentAt(OffsetDateTime.now());
        mockLog.setStatus(status != null ? status : "failed");

        if ("failed".equalsIgnoreCase(status)) {
            mockLog.setErrorMessage("java.net.ConnectException: Kết nối timeout mô phỏng (Không gửi mail thật).");
        }

        emailLogRepository.save(mockLog);

        return ResponseEntity.ok("Tạo log giả với trạng thái [" + status.toUpperCase() + "] thành công!");
    }

    // =========================================================
    // API: TEST GỬI MAIL
    // =========================================================
    @PostMapping("/api/admin/emails/test-send")
    @ResponseBody
    @LoggableAction(action = "TEST_SEND_EMAIL", targetType = "EMAIL_SYSTEM", details = "'Gửi mail test tới: ' + #payload['email']")
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
