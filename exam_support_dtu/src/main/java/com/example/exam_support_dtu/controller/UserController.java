//package com.example.exam_support_dtu.controller;
//
//import com.example.exam_support_dtu.dto.UserExamResultDto;
//import com.example.exam_support_dtu.repository.ExamScheduleRepository;
//import com.example.exam_support_dtu.repository.ExamStudentRepository;
//import com.example.exam_support_dtu.repository.FilesRepository;
//import org.springframework.stereotype.Controller;
//import org.springframework.ui.Model;
//import org.springframework.web.bind.annotation.GetMapping;
//
//import java.util.ArrayList;
//
//@Controller
//public class UserController {
//
//    private final ExamScheduleRepository examScheduleRepository;
//    private final ExamStudentRepository examStudentRepository;
//    private final FilesRepository filesRepository;
//
//    public UserController(ExamScheduleRepository examScheduleRepository,
//                          ExamStudentRepository examStudentRepository,
//                          FilesRepository filesRepository) {
//        this.examScheduleRepository = examScheduleRepository;
//        this.examStudentRepository = examStudentRepository;
//        this.filesRepository = filesRepository;
//    }
//
//    // =========================================================
//    // 1. HIỂN THỊ TRANG CHỦ USER
//    // =========================================================
//    @GetMapping("/user/home")
//    public String showUserHome(Model model) {
//
//        long totalSchedules = examScheduleRepository.count();
//        long totalStudents = examStudentRepository.count();
//        long totalDocuments = filesRepository.count();
//
//        model.addAttribute("totalSchedules", totalSchedules);
//        model.addAttribute("totalStudents", totalStudents);
//        model.addAttribute("totalDocuments", totalDocuments);
//
//        // Dữ liệu mặc định cho form tra cứu
//        model.addAttribute("studentCode", "");
//        model.addAttribute("examResults", new ArrayList<UserExamResultDto>());
//        model.addAttribute("totalResults", 0);
//
//        return "user-home";
//    }
//}


package com.example.exam_support_dtu.controller;

import com.example.exam_support_dtu.dto.UserExamResultDto;
import com.example.exam_support_dtu.repository.ExamScheduleRepository;
import com.example.exam_support_dtu.repository.ExamStudentRepository;
import com.example.exam_support_dtu.repository.FilesRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.ArrayList;

@Controller
public class UserController {

    private final ExamScheduleRepository examScheduleRepository;
    private final ExamStudentRepository examStudentRepository;
    private final FilesRepository filesRepository;

    public UserController(ExamScheduleRepository examScheduleRepository,
                          ExamStudentRepository examStudentRepository,
                          FilesRepository filesRepository) {
        this.examScheduleRepository = examScheduleRepository;
        this.examStudentRepository = examStudentRepository;
        this.filesRepository = filesRepository;
    }

    // =========================================================
    // 1. HIỂN THỊ TRANG CHỦ USER
    // =========================================================
    @GetMapping("/user/home")
    public String showUserHome(Model model) {

        long totalSchedules = examScheduleRepository.count();
        long totalStudents = examStudentRepository.count();
        long totalDocuments = filesRepository.count();

        model.addAttribute("totalSchedules", totalSchedules);
        model.addAttribute("totalStudents", totalStudents);
        model.addAttribute("totalDocuments", totalDocuments);

        model.addAttribute("studentCode", "");
        model.addAttribute("semester", "");
        model.addAttribute("courseFilter", "");
        model.addAttribute("examResults", new ArrayList<UserExamResultDto>());
        model.addAttribute("totalResults", 0);

        return "index";
    }
}