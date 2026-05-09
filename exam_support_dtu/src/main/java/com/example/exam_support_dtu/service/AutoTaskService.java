package com.example.exam_support_dtu.service;

import com.example.exam_support_dtu.annotation.LoggableAction;
import com.example.exam_support_dtu.entity.ExamInterest;
import com.example.exam_support_dtu.entity.ExamRoom;
import com.example.exam_support_dtu.entity.Users;
import com.example.exam_support_dtu.repository.ExamInterestRepository;
import com.example.exam_support_dtu.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AutoTaskService {

    private final ExamCrawlService examCrawlService;
    private final ExamInterestRepository examInterestRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;
    private final com.example.exam_support_dtu.repository.ExamRoomRepository examRoomRepository;
    private final com.example.exam_support_dtu.repository.SystemSettingRepository systemSettingRepository;

    /**
     * Tự động cào dữ liệu lúc 2 giờ sáng hàng ngày
     */
    @LoggableAction(action = "AUTO_CRAWL", targetType = "SYSTEM", details = "'Tự động cào dữ liệu. Kết quả: ' + #result")
    public String autoCrawlTask() {
        int pages = systemSettingRepository.findById("crawl.auto_pages")
                .map(s -> Integer.parseInt(s.getValue()))
                .orElse(5);
        return examCrawlService.CrawlMultiplePages(pages);
    }

    /**
     * Tự động gửi thông báo nhắc nhở lúc 6 giờ sáng hàng ngày
     */
    @LoggableAction(action = "AUTO_REMINDER", targetType = "SYSTEM", details = "'Gửi thông báo nhắc nhở tự động. Đã gửi: ' + #result + ' email.'")
    public int autoReminderTask() {
        // Kiểm tra xem tính năng Auto-Mail có đang bật không
        boolean isEnabled = systemSettingRepository.findById("mail.auto_enabled")
                .map(s -> Boolean.parseBoolean(s.getValue()))
                .orElse(true);

        if (!isEnabled) {
            System.out.println(">>> AUTO REMINDER: Feature is DISABLED in settings.");
            return 0;
        }

        List<ExamInterest> pendingReminders = examInterestRepository.findByNotifiedFalse();
        LocalDate today = LocalDate.now();

        int sentCount = 0;

        for (ExamInterest interest : pendingReminders) {
            Users user = userRepository.findById(interest.getUserId()).orElse(null);
            if (user == null || user.getEmail() == null)
                continue;

            ExamRoom room = interest.getExamRoom();
            int notifyDays = (interest.getStudentNotifyDays() != null) ? interest.getStudentNotifyDays() : 4;

            if (room != null) {
                // TRƯỜNG HỢP 1: LỊCH THI CỦA TÔI (Có phòng thi cụ thể)
                // Logic: Gửi 1 lần duy nhất trước ngày thi N ngày
                LocalDate examDate = room.getExamDate();
                if (examDate != null) {
                    long daysUntilExam = ChronoUnit.DAYS.between(today, examDate);
                    if (daysUntilExam <= notifyDays && daysUntilExam >= 0) {
                        if (sendReminderEmail(interest, user, true)) {
                            interest.setNotified(true);
                            interest.setLastNotifiedAt(java.time.OffsetDateTime.now());
                            examInterestRepository.save(interest);
                            sentCount++;
                        }
                    }
                }
            } else if (interest.getExamSchedule() != null || interest.getExamOriginal() != null) {
                // TRƯỜNG HỢP 2: LỊCH THI QUAN TÂM (Theo dõi môn học/file)
                // Logic: Gửi N lần trong N ngày liên tiếp kể từ khi quan tâm
                int currentCount = (interest.getNotifyCount() != null) ? interest.getNotifyCount() : 0;

                if (currentCount < notifyDays) {
                    // Kiểm tra xem hôm nay đã gửi chưa (tránh gửi 2 lần/ngày nếu task chạy lại)
                    boolean alreadySentToday = false;
                    if (interest.getLastNotifiedAt() != null) {
                        alreadySentToday = interest.getLastNotifiedAt().toLocalDate().isEqual(today);
                    }

                    if (!alreadySentToday) {
                        if (sendReminderEmail(interest, user, false)) {
                            interest.setNotifyCount(currentCount + 1);
                            interest.setLastNotifiedAt(java.time.OffsetDateTime.now());
                            if (interest.getNotifyCount() >= notifyDays) {
                                interest.setNotified(true);
                            }
                            examInterestRepository.save(interest);
                            sentCount++;
                        }
                    }
                }
            }
        }
        System.out.println(">>> AUTO REMINDER: Sent " + sentCount + " emails.");
        return sentCount;
    }

    private boolean sendReminderEmail(ExamInterest interest, Users user, boolean isPersonal) {
        try {
            Map<String, String> variables = new HashMap<>();
            variables.put("TEN_SV", user.getFullName());
            variables.put("MA_SV", user.getStudentCode());

            if (isPersonal) {
                // Trường hợp 1: Có phòng thi cụ thể (Lịch thi của tôi)
                ExamRoom room = interest.getExamRoom();
                variables.put("TEN_MON", interest.getExamSchedule().getCourseName());
                variables.put("PHONG_THI", room.getRoomName());
                variables.put("NGAY_THI", room.getExamDate().toString());
                variables.put("GIO_THI", room.getExamTime());
                return emailService.sendEmailUsingTemplate(user.getEmail(), "EXAM_REMINDER", variables);
            } else {
                // Trường hợp 2: Theo dõi môn học (Lịch thi quan tâm)
                String courseName = (interest.getExamSchedule() != null)
                        ? interest.getExamSchedule().getCourseName()
                        : (interest.getExamOriginal() != null ? interest.getExamOriginal().getFileOriginalName()
                                : "Môn học đã quan tâm");

                variables.put("TEN_MON", courseName);
                variables.put("LAN_NHAC",
                        String.valueOf((interest.getNotifyCount() != null ? interest.getNotifyCount() : 0) + 1));

                return emailService.sendEmailUsingTemplate(user.getEmail(), "EXAM_FOLLOW_REMINDER", variables);
            }
        } catch (Exception e) {
            System.err.println("Lỗi khi gửi mail nhắc nhở tự động: " + e.getMessage());
            return false;
        }
    }
}
