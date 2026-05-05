//package com.example.exam_support_dtu.controller;
//
//import org.springframework.stereotype.Controller;
//import org.springframework.ui.Model;
//import org.springframework.web.bind.annotation.GetMapping;
//import org.springframework.web.bind.annotation.RequestParam;
//
//@Controller
//public class HomeController {
//
//    // 1. Hàm hiển thị Trang chủ tra cứu
//    @GetMapping("/home")
//    public String showSearchPage() {
//        // Trả về file index.html nằm trong thư mục src/main/resources/templates
//        return "index";
//    }
//
//    // 2. Hàm xử lý khi Sinh viên bấm nút "Tra cứu"
//    @GetMapping("/search")
//    public String searchExam(@RequestParam("studentCode") String studentCode, Model model) {
//        // 'Model' là chiếc túi để Controller bỏ dữ liệu vào và mang ra ngoài giao diện
//        // HTML
//
//        // Tạm thời ta cứ gói mã SV mà người dùng nhập vào túi để mang sang trang sau
//        model.addAttribute("studentCode", studentCode);
//
//        // (Ở phần sau, chỗ này sẽ gọi hàm DB: List<ExamStudent> list = ... roi put vao
//        // model)
//
//        // Trả về file result.html (Trang kết quả - ta sẽ làm ở bước tiếp theo)
//        return "result";
//    }
//}


package com.example.exam_support_dtu.controller;

import com.example.exam_support_dtu.dto.UserExamResultDto;
import com.example.exam_support_dtu.entity.ExamRoom;
import com.example.exam_support_dtu.entity.ExamSchedule;
import com.example.exam_support_dtu.entity.ExamStudent;
import com.example.exam_support_dtu.repository.ExamStudentRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.ArrayList;
import java.util.List;

@Controller
public class HomeController {

    private final ExamStudentRepository examStudentRepository;

    public HomeController(ExamStudentRepository examStudentRepository) {
        this.examStudentRepository = examStudentRepository;
    }

    // =========================================================
    // 1. ĐIỀU HƯỚNG TRANG GỐC VỀ TRANG CHỦ USER
    // =========================================================
    @GetMapping("/")
    public String redirectToUserHome() {
        return "redirect:/user/home";
    }

    // =========================================================
    // 2. XỬ LÝ TRA CỨU LỊCH THI THEO MÃ SINH VIÊN
    // =========================================================
    @GetMapping("/search")
    public String searchExam(@RequestParam("studentCode") String studentCode,
                             Model model) {

        String keywordStudentCode = studentCode != null ? studentCode.trim() : "";

        List<UserExamResultDto> examResults = new ArrayList<>();

        if (keywordStudentCode.isEmpty()) {
            model.addAttribute("studentCode", keywordStudentCode);
            model.addAttribute("examResults", examResults);
            model.addAttribute("totalResults", 0);
            model.addAttribute("message", "Vui lòng nhập mã sinh viên để tra cứu.");
            return "result";
        }

        List<ExamStudent> students =
                examStudentRepository.findByStudentCodeContainingIgnoreCase(keywordStudentCode);

        for (ExamStudent student : students) {

            ExamRoom room = student.getExamRoom();

            if (room == null) {
                continue;
            }

            ExamSchedule schedule = room.getExamSchedule();

            if (schedule == null) {
                continue;
            }

            String fullName = "";

            if (student.getLastName() != null) {
                fullName += student.getLastName();
            }

            if (student.getFirstName() != null) {
                fullName += " " + student.getFirstName();
            }

            UserExamResultDto dto = new UserExamResultDto(
                    schedule.getId(),
                    room.getId(),
                    student.getStudentCode(),
                    fullName.trim(),
                    student.getStudentClass(),
                    student.getClassCode(),
                    student.getSeatNumber(),
                    schedule.getCourseCode(),
                    schedule.getCourseName(),
                    schedule.getCredit(),
                    schedule.getSemester(),
                    schedule.getAttempt(),
                    room.getRoomName(),
                    room.getLocation(),
                    room.getExamDate(),
                    room.getExamTime(),
                    student.getNote()
            );

            examResults.add(dto);
        }

        model.addAttribute("studentCode", keywordStudentCode);
        model.addAttribute("examResults", examResults);
        model.addAttribute("totalResults", examResults.size());

        if (examResults.isEmpty()) {
            model.addAttribute("message", "Không tìm thấy lịch thi phù hợp với mã sinh viên đã nhập.");
        }

        return "result";
    }
}