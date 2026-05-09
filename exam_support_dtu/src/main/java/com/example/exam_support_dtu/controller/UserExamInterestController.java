package com.example.exam_support_dtu.controller;

import com.example.exam_support_dtu.entity.*;
import com.example.exam_support_dtu.repository.*;
import com.example.exam_support_dtu.service.ExamInterestService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Controller
public class UserExamInterestController {

    private final ExamInterestService examInterestService;
    private final ExamInterestRepository examInterestRepository;
    private final ExamScheduleRepository examScheduleRepository;
    private final UserRepository userRepository;
    private final SystemSettingRepository systemSettingRepository;

    public UserExamInterestController(ExamInterestService examInterestService,
            ExamInterestRepository examInterestRepository,
            ExamScheduleRepository examScheduleRepository,
            UserRepository userRepository,
            SystemSettingRepository systemSettingRepository) {
        this.examInterestService = examInterestService;
        this.examInterestRepository = examInterestRepository;
        this.examScheduleRepository = examScheduleRepository;
        this.userRepository = userRepository;
        this.systemSettingRepository = systemSettingRepository;
    }

    // =========================================================
    // 1. HIỂN THỊ TRANG "LỊCH THI CỦA TÔI" (2 Tabs)
    // =========================================================
    @GetMapping("/user/my-exams")
    public String showMyExamsPage(
            @RequestParam(value = "tab", defaultValue = "my") String tab,
            @AuthenticationPrincipal Users currentUser,
            Model model) {

        model.addAttribute("activeTab", tab);
        model.addAttribute("needStudentCode", false); // Mặc định là false

        // Lấy ngày nhắc hẹn mặc định từ hệ thống (Lấy theo key mail.default_notify_days
        // của Admin)
        String defaultDaysStr = systemSettingRepository.findById("mail.default_notify_days")
                .map(s -> s.getValue()).orElse("3");
        model.addAttribute("systemDefaultDays", Integer.parseInt(defaultDaysStr));

        // Lấy User mới nhất từ DB (vì currentUser trong Session có thể bị cũ)
        Users freshUser = userRepository.findById(currentUser.getId()).orElse(currentUser);

        // === TAB 1: LỊCH THI CỦA TÔI (lọc theo MSSV) ===
        if ("my".equals(tab)) {
            String studentCode = freshUser.getStudentCode();

            if (studentCode == null || studentCode.trim().isEmpty()) {
                model.addAttribute("needStudentCode", true);
            } else {
                // Gọi service để đồng bộ
                examInterestService.syncPersonalExams(freshUser);

                // Lấy danh sách hiển thị
                List<ExamInterest> myExams = examInterestService.getMyExams(freshUser.getId());
                model.addAttribute("myExams", myExams);
                model.addAttribute("studentCode", studentCode.trim());
            }
        }

        // === TAB 2: LỊCH THI QUAN TÂM (theo dõi thủ công) ===
        if ("followed".equals(tab)) {
            List<ExamInterest> followedExams = examInterestService.getFollowedExams(currentUser.getId());
            model.addAttribute("followedExams", followedExams);
        }

        return "user-my-exams";
    }

    // =========================================================
    // 2. API: THEO DÕI MỘT LỊCH THI
    // =========================================================
    @PostMapping("/api/user/schedules/{id}/follow")
    @ResponseBody
    public ResponseEntity<?> followSchedule(
            @PathVariable Long id, // Đây là id của ExamSchedule HOẶC ExamOriginal
            @RequestParam(required = false) Long roomId,
            @RequestParam(required = false, defaultValue = "false") Boolean isOriginal,
            @AuthenticationPrincipal Users currentUser) {

        if (currentUser == null)
            return ResponseEntity.status(401).build();

        // Lấy User tươi từ DB
        Users freshUser = userRepository.findById(currentUser.getId()).get();

        if (Boolean.TRUE.equals(isOriginal)) {
            // Theo dõi tệp PDF gốc
            examInterestService.followOriginal(id, freshUser.getId());
            return ResponseEntity.ok(Map.of("status", "followed", "message", "Đã thêm vào danh sách quan tâm!"));
        }

        if (roomId != null) {
            // Trường hợp 1: Theo dõi phòng thi cụ thể (Theo dõi cá nhân - từ nút 'Theo dõi'
            // trong kết quả tra cứu)
            ExamInterest ei = examInterestService.followRoom(id, roomId, freshUser.getId());
            return ResponseEntity.ok(Map.of("status", "followed", "message", "Đã thêm vào lịch thi cá nhân của bạn!"));
        } else {
            // Trường hợp 2: Theo dõi môn học thông minh (Từ nút 'Quan tâm' ở danh sách chung)
            
            // KIỂM TRA TRƯỚC: Nếu đã có trong danh sách rồi
            java.util.List<ExamInterest> existingList = examInterestRepository.findAllByUserIdAndExamScheduleId(freshUser.getId(), id);
            if (!existingList.isEmpty()) {
                ExamInterest existing = existingList.get(0);
                if (existing.getExamRoom() != null) {
                    // Đã có trong Lịch cá nhân (My Exams)
                    return ResponseEntity.ok(Map.of(
                        "status", "already_in_my_exams", 
                        "message", "Môn " + existing.getExamSchedule().getCourseName() + " đã có trong lịch thi cá nhân của bạn rồi!"
                    ));
                } else {
                    // Đã quan tâm rồi
                    return ResponseEntity.ok(Map.of("status", "already_followed", "message", "Bạn đã quan tâm môn này rồi!"));
                }
            }

            ExamInterest ei = examInterestService.followScheduleSmart(id, freshUser);
            
            // Nếu interest mới tạo có examRoom -> tức là nó đã tự động nâng cấp thành Lịch cá nhân
            if (ei.getExamRoom() != null) {
                return ResponseEntity.ok(Map.of("status", "upgraded", "message", "Hệ thống nhận diện bạn có lịch thi môn này. Đã tự động thêm vào lịch cá nhân!"));
            }
            return ResponseEntity.ok(Map.of("status", "followed", "message", "Đã thêm vào danh sách quan tâm!"));
        }
    }

    // =========================================================
    // 3. API: CẬP NHẬT CÀI ĐẶT THÔNG BÁO
    // =========================================================
    @PatchMapping("/api/user/interests/{id}/settings")
    @ResponseBody
    public ResponseEntity<?> updateSettings(
            @PathVariable Long id,
            @RequestBody Map<String, Object> settings,
            @AuthenticationPrincipal Users currentUser) {

        Integer days = (Integer) settings.get("notifyBeforeDays");

        // Cho phép null để quay về mặc định hệ thống
        boolean success = examInterestService.updateNotificationDays(id, currentUser.getId(), days);
        if (success) {
            return ResponseEntity.ok(Map.of("message", "Đã cập nhật cài đặt!"));
        }
        return ResponseEntity.status(403).build();
    }

    // =========================================================
    // 4. API: HỦY THEO DÕI
    // =========================================================
    @DeleteMapping("/api/user/interests/{interestId}")
    @ResponseBody
    public ResponseEntity<?> unfollowExam(
            @PathVariable Long interestId,
            @AuthenticationPrincipal Users currentUser) {

        Optional<ExamInterest> interestOpt = examInterestRepository.findById(interestId);
        if (interestOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        ExamInterest interest = interestOpt.get();
        if (!interest.getUserId().equals(currentUser.getId())) {
            return ResponseEntity.status(403).build();
        }

        examInterestRepository.delete(interest);
        return ResponseEntity.ok(Map.of("message", "Đã hủy theo dõi!"));
    }
}
