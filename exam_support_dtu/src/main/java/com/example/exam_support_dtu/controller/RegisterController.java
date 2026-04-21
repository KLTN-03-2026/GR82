package com.example.exam_support_dtu.controller;

import com.example.exam_support_dtu.entity.User;
import com.example.exam_support_dtu.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class RegisterController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder; // Dùng để băm mật khẩu

    public RegisterController(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // 1. Hiển thị form đăng ký
    @GetMapping("/register")
    public String showRegisterForm() {
        return "register";
    }

    // 2. Xử lý khi bấm nút Đăng Ký
    @PostMapping("/register")
    public String processRegister(@RequestParam String fullName,
                                  @RequestParam String email,
                                  @RequestParam String password,
                                  @RequestParam String confirmPassword,
                                  RedirectAttributes redirectAttributes) {

        // Kiểm tra xem 2 mật khẩu có khớp không
        if (!password.equals(confirmPassword)) {
            redirectAttributes.addAttribute("error", "Mật khẩu nhập lại không khớp!");
            return "redirect:/register";
        }

        // Kiểm tra xem Email đã tồn tại trong DB chưa
        if (userRepository.findByEmail(email).isPresent()) {
            redirectAttributes.addAttribute("error", "Email này đã được sử dụng!");
            return "redirect:/register";
        }

        // Tạo User mới và lưu vào DB
        User newUser = new User();
        newUser.setFullName(fullName);
        newUser.setEmail(email);
        // Cực kỳ quan trọng: Phải băm mật khẩu trước khi lưu
        newUser.setPasswordHash(passwordEncoder.encode(password));
        newUser.setRole("student"); // Mặc định tài khoản đăng ký mới là sinh viên
        newUser.setActive(true);

        userRepository.save(newUser);

        // Chuyển hướng về trang Login kèm thông báo thành công
        redirectAttributes.addAttribute("success", "Đăng ký thành công! Vui lòng đăng nhập.");
        return "redirect:/login";
    }
}