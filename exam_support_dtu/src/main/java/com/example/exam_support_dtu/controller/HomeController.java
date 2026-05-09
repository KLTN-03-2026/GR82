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

}
