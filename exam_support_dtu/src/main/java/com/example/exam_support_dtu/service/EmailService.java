package com.example.exam_support_dtu.service;

import com.example.exam_support_dtu.entity.EmailLog;
import com.example.exam_support_dtu.entity.EmailTemplate;
import com.example.exam_support_dtu.repository.EmailLogRepository;
import com.example.exam_support_dtu.repository.EmailTemplateRepository;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Optional;

@Service
public class EmailService {

    // Công cụ gửi mail được Spring Boot cung cấp
    private final JavaMailSender javaMailSender;

    // Repository để lấy nội dung mẫu Email từ Database
    private final EmailTemplateRepository emailTemplateRepository;
    private final EmailLogRepository emailLogRepository;
    public EmailService(JavaMailSender javaMailSender, EmailTemplateRepository emailTemplateRepository, EmailLogRepository emailLogRepository) {
        this.javaMailSender = javaMailSender;
        this.emailTemplateRepository = emailTemplateRepository;
        this.emailLogRepository = emailLogRepository;
    }

    /**
     * Hàm gửi email cơ bản với định dạng HTML.
     *
     * @param toEmail Email người nhận
     * @param subject Tiêu đề email
     * @param body    Nội dung email (có thể chứa HTML)
     */

    public void sendHtmlEmail(String toEmail, String subject, String body) throws MessagingException {
        sendHtmlEmail(toEmail, subject, body, null);
    }

    public void sendHtmlEmail(String toEmail, String subject, String body,Long templateId) throws MessagingException {
        // 1. Tạo Log chuẩn bị lưu
        EmailLog log = new EmailLog();
        log.setToEmail(toEmail);
        log.setSubject(subject);

        // Cắt lấy một đoạn ngắn của nội dung để lưu vào bodySnippet (ví dụ 255 ký tự) tránh tràn DB
        String snippet = body.replaceAll("<[^>]*>", ""); // Xóa bớt thẻ HTML cho dễ đọc
        log.setBodySnippet(snippet.length() > 250 ? snippet.substring(0, 250) + "..." : snippet);

        log.setSentAt(OffsetDateTime.now());
        if (templateId != null) {
            log.setTemplateId(templateId);
        }
        try {
            // Tạo một đối tượng MimeMessage (dành cho email có định dạng phức tạp như HTML, đính kèm file)
            MimeMessage message = javaMailSender.createMimeMessage();
            //  True nghĩa là cho phép multipart
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(toEmail); // Người nhận
            helper.setSubject(subject); // Tiêu đề
            // Tham số thứ 2 là true để báo cho Spring biết đây là nội dung HTML
            helper.setText(body, true);

            // Thực hiện gửi
            javaMailSender.send(message);

            // Lưu trạng thái thành công
            log.setStatus("sent");
            emailLogRepository.save(log);
        }catch (Exception e){
            // Lưu trạng thái thất bại và nguyên nhân
            log.setStatus("failed");
            log.setErrorMessage(e.getMessage());
            emailLogRepository.save(log);

            throw new MessagingException("Lỗi gửi mail: " + e.getMessage());
        }
    }

    public boolean sendEmailUsingTemplate(String toEmail, String templateCode, Map<String, String> variables) {
        try {
            // 1. Tìm template theo mã Code (VD: "OTP_REGISTER")
            Optional<EmailTemplate> optTemplate = emailTemplateRepository.findByCode(templateCode);
            if (optTemplate.isEmpty()) {
                System.err.println("Lỗi: Không tìm thấy mẫu email có mã " + templateCode);
                return false;
            }

            EmailTemplate template = optTemplate.get();
            String subject = template.getSubject();
            String body = template.getBodyHtml();

            // 2. Vòng lặp thay thế TẤT CẢ các biến động có trong Map
            // Ví dụ biến "[FULL_NAME]" -> "Nguyễn Văn A"
            for (Map.Entry<String, String> entry : variables.entrySet()) {
                String placeholder = "[" + entry.getKey() + "]";
                String realValue = entry.getValue();

                if (subject != null) {
                    subject = subject.replace(placeholder, realValue);
                }
                if (body != null) {
                    body = body.replace(placeholder, realValue);
                }
            }

            // 3. Gửi đi
            sendHtmlEmail(toEmail, subject, body,template.getId());
            return true;

        } catch (Exception e) {
            System.err.println("Lỗi khi gửi mail từ Template: " + e.getMessage());
            return false;
        }
    }

    // --- HÀM MỚI BỔ SUNG ĐỂ SỬA LỖI TRÙNG LOG KHI RESEND ---
    public void resendEmailLog(EmailLog existingLog) throws MessagingException {
        try {
            MimeMessage message = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(existingLog.getToEmail());
            helper.setSubject(existingLog.getSubject());
            helper.setText(existingLog.getBodySnippet(), true);

            javaMailSender.send(message);

            existingLog.setStatus("sent");
            existingLog.setErrorMessage(null);
            existingLog.setSentAt(OffsetDateTime.now());
            emailLogRepository.save(existingLog);

        } catch (Exception e) {
            existingLog.setStatus("failed");
            existingLog.setErrorMessage("Gửi lại thất bại: " + e.getMessage());
            existingLog.setSentAt(OffsetDateTime.now());
            emailLogRepository.save(existingLog);
            throw new MessagingException("Lỗi gửi mail: " + e.getMessage());
        }
    }



    /**
     * Hàm Test gửi mail sử dụng ID của Mẫu Email (Template).
     * Sẽ thay thế các biến giả lập trước khi gửi.
     */
    public boolean sendTestEmail(String toEmail, Long templateId) {
        try {
            // Lấy mẫu từ DB
            Optional<EmailTemplate> optTemplate = emailTemplateRepository.findById(templateId);
            if (optTemplate.isEmpty()) {
                System.out.println("Không tìm thấy mẫu email với ID: " + templateId);
                return false;
            }

            EmailTemplate template = optTemplate.get();
            String subject = template.getSubject();

            // Lấy nội dung HTML (nếu không có thì lấy Text thường)
            String body = template.getBodyHtml();
            if (body == null || body.isEmpty()) {
                body = template.getBodyText();
            }

            // --- BƯỚC THAY THẾ BIẾN (REPLACE PLACEHOLDERS) ---
            if (body != null) {
                body = body.replace("[TEN_SV]", "Sinh Viên Test")
                        .replace("[MA_SV]", "DTU123456")
                        .replace("[TEN_MON]", "Lập trình Web Test")
                        .replace("[PHONG_THI]", "P.302 Test")
                        .replace("[NGAY_THI]", "30/05/2026")
                        .replace("[GIO_THI]", "14:00");
            }

            // Gọi hàm gửi mail thực tế ở trên
            sendHtmlEmail(toEmail, subject, body,template.getId());

            System.out.println("Đã gửi mail test thành công tới: " + toEmail);
            return true;

        } catch (Exception e) {
            System.err.println("Lỗi khi gửi mail test: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
}
