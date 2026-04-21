package com.example.exam_support_dtu.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class LoginController {

    @GetMapping("/login")
    public String showLoginPage() {
        // Trả về đúng tên file login.html trong thư mục templates
        return "login";
    }
}