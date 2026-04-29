package com.example.exam_support_dtu.service;

import com.example.exam_support_dtu.entity.EmailTemplate;
import com.example.exam_support_dtu.repository.EmailTemplateRepository;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class EmailService {

    // Công cụ gửi mail được Spring Boot cung cấp
    private final JavaMailSender javaMailSender;

    // Repository để lấy nội dung mẫu Email từ Database
    private final EmailTemplateRepository emailTemplateRepository;

    public EmailService(JavaMailSender javaMailSender, EmailTemplateRepository emailTemplateRepository) {
        this.javaMailSender = javaMailSender;
        this.emailTemplateRepository = emailTemplateRepository;
    }

    /**
     * Hàm gửi email cơ bản với định dạng HTML.
     *
     * @param toEmail Email người nhận
     * @param subject Tiêu đề email
     * @param body    Nội dung email (có thể chứa HTML)
     */
    public void sendHtmlEmail(String toEmail, String subject, String body) throws MessagingException {
        // Tạo một đối tượng MimeMessage (dành cho email có định dạng phức tạp như HTML, đính kèm file)
        MimeMessage message = javaMailSender.createMimeMessage();

        // Dùng Helper để set các thông số dễ dàng hơn. True nghĩa là cho phép multipart
        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

        helper.setTo(toEmail); // Người nhận
        helper.setSubject(subject); // Tiêu đề
        // Tham số thứ 2 là true để báo cho Spring biết đây là nội dung HTML
        helper.setText(body, true);

        // Thực hiện gửi
        javaMailSender.send(message);
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
            // Vì đây là gửi test, ta giả lập dữ liệu thay vào các biến [TEN_SV], [MA_SV],
            // v.v.
            if (body != null) {
                body = body.replace("[TEN_SV]", "Sinh Viên Test")
                        .replace("[MA_SV]", "DTU123456")
                        .replace("[TEN_MON]", "Lập trình Web Test")
                        .replace("[PHONG_THI]", "P.302 Test")
                        .replace("[NGAY_THI]", "30/05/2026")
                        .replace("[GIO_THI]", "14:00");
            }

            // Gọi hàm gửi mail thực tế ở trên
            sendHtmlEmail(toEmail, subject, body);

            System.out.println("Đã gửi mail test thành công tới: " + toEmail);
            return true;

        } catch (Exception e) {
            System.err.println("Lỗi khi gửi mail test: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
}
