package com.example.exam_support_dtu.service;

import com.example.exam_support_dtu.entity.AuditLog;
import com.example.exam_support_dtu.repository.AuditLogRepository;
import org.springframework.stereotype.Service;

@Service
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    public AuditLogService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    // Hàm gốc để lưu Log vào Database
    private void saveLog(String logLevel, String targetType, String action, String details) {
        AuditLog log = new AuditLog();
        log.setLogLevel(logLevel);
        log.setTargetType(targetType);
        log.setAction(action);
        log.setDetails(details);
        // Nếu có Admin đang đăng nhập, bạn có thể lấy ID từ Spring Security set vào đây: log.setUserId(...)
        auditLogRepository.save(log);
    }

    // =========================================================================
    // 6 HÀM TIỆN ÍCH DỰA TRÊN DANH SÁCH BẠN CHỌN
    // =========================================================================

    // 1. Khi chạy Crawler xong
    public void logCrawlerSuccess(int newRecords, int updatedRecords) {
        String details = String.format("Hoàn tất lấy dữ liệu lịch thi. Cập nhật mới: %d records, Thay đổi: %d records.", newRecords, updatedRecords);
        saveLog("SUCCESS", "CRAWLER", "Cào dữ liệu thành công", details);
    }

    // 2. Khi CronJob buổi sáng chạy xong
    public void logCronJobFinished(int emailCount) {
        String details = String.format("CronJob (08:00 AM) đã quét xong. Đưa %d email nhắc nhở vào hàng đợi.", emailCount);
        saveLog("INFO", "CRON_JOB", "Hoàn tất quét lịch thi tự động", details);
    }

    // 3. Khi lỗi gửi Mail (Exception)
    public void logSmtpError(String errorMessage) {
        String details = "Không thể kết nối đến smtp.gmail.com. Chi tiết lỗi: " + errorMessage;
        saveLog("ERROR", "MAIL_SYSTEM", "Thất bại kết nối SMTP", details);
    }

    // 4. Khi Admin bấm Lưu cấu hình hệ thống
    public void logConfigChange(boolean isAutoMailOn, int notifyDays) {
        String status = isAutoMailOn ? "BẬT" : "TẮT";
        String details = String.format("Đã %s tính năng Auto-Mail. Đổi thời gian báo trước thành: %d ngày.", status, notifyDays);
        saveLog("INFO", "SYSTEM_CONFIG", "Thay đổi cấu hình hệ thống", details);
    }

    // 5. Khi phát hiện lịch thi bị dời/đổi phòng
    public void logExamChangeWarning(String courseName, String courseCode, int studentCount) {
        String details = String.format("Môn %s (%s) bị đổi thông tin. Đã kích hoạt cảnh báo cho %d sinh viên đang theo dõi.", courseName, courseCode, studentCount);
        saveLog("WARNING", "EXAM_SCHEDULE", "Phát hiện lịch thi thay đổi", details);
    }

    // 6. Khi thực sự gửi mail nhắc nhở thành công
    public void logExamReminderSent(String courseName, String courseCode, int studentCount) {
        String details = String.format("Môn %s (%s). Đã gửi mail nhắc nhở cho %d sinh viên đang theo dõi.", courseName, courseCode, studentCount);
        saveLog("SUCCESS", "MAIL_SYSTEM", "Gửi mail nhắc nhở thành công", details);
    }
}