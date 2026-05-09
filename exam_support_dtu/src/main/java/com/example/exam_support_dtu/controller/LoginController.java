package com.example.exam_support_dtu.controller;

import com.example.exam_support_dtu.entity.Users;
import com.example.exam_support_dtu.entity.VerificationToken;
import com.example.exam_support_dtu.repository.UserRepository;
import com.example.exam_support_dtu.repository.VerificationTokenRepository;
import com.example.exam_support_dtu.service.EmailService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Random;

@Controller
public class LoginController {

    private final UserRepository userRepository;
    private final VerificationTokenRepository tokenRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;

    public LoginController(UserRepository userRepository,
                           VerificationTokenRepository tokenRepository,
                           EmailService emailService,
                           PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.tokenRepository = tokenRepository;
        this.emailService = emailService;
        this.passwordEncoder = passwordEncoder;
    }


    @GetMapping("/login")
    public String showLoginPage() {
        // Trả về đúng tên file login.html trong thư mục templates
        return "login";
    }

    // ==================================================================
    // TÍNH NĂNG 1: XỬ LÝ KHI BẤM NÚT "GỬI MÃ KHÔI PHỤC"
    // ==================================================================
    @PostMapping("/forgot-password")
    public String processForgotPassword(@RequestParam String email, RedirectAttributes redirectAttributes) {
        Optional<Users> optUser = userRepository.findByEmail(email);

        // 1. Kiểm tra email có tồn tại không
        if (optUser.isEmpty()) {
            redirectAttributes.addAttribute("error", "Email này chưa được đăng ký trong hệ thống!");
            return "redirect:/login"; // Đẩy về trang login (kèm popup/tab quên mật khẩu)
        }

        Users user = optUser.get();

        // 2. Xóa OTP cũ (nếu có) để tránh rác DB
        tokenRepository.deleteByUserId(user.getId());

        // 3. Tạo mã OTP mới 6 số
        String otpCode = String.format("%06d", new Random().nextInt(999999));
        VerificationToken token = new VerificationToken(otpCode, user.getId());
        // Set hạn sử dụng 15 phút cho bảo mật (Ghi đè hạn 1h mặc định trong Entity)
        token.setExpiryDate(LocalDateTime.now().plusMinutes(15));
        tokenRepository.save(token);

        // 4. Gửi mail bằng Template "OTP_FORGOT_PASSWORD"
        Map<String, String> variables = new HashMap<>();
        variables.put("FULL_NAME", user.getFullName());
        variables.put("OTP_CODE", otpCode);

        boolean isSent = emailService.sendEmailUsingTemplate(user.getEmail(), "OTP_FORGOT_PASSWORD", variables);

        if (isSent) {
            redirectAttributes.addAttribute("success", "Mã xác thực đã được gửi. Vui lòng kiểm tra Email!");
            redirectAttributes.addAttribute("email", user.getEmail());
            // Chuyển hướng sang trang nhập OTP và đổi mật khẩu mới
            return "redirect:/reset-password";
        } else {
            redirectAttributes.addAttribute("error", "Lỗi gửi mail hệ thống. Vui lòng báo cho Admin.");
            return "redirect:/login";
        }
    }

    // ==================================================================
    // TÍNH NĂNG 2: HIỂN THỊ TRANG NHẬP OTP VÀ ĐẶT LẠI MẬT KHẨU
    // ==================================================================
    @GetMapping("/reset-password")
    public String showResetPasswordPage(@RequestParam(value = "email", required = false) String email, Model model) {
        if (email == null || email.isEmpty()) {
            return "redirect:/login"; // Ngăn người dùng tự gõ URL
        }
        model.addAttribute("email", email);
        return "reset-password"; // Trả về giao diện reset-password.html
    }

    // ==================================================================
    // TÍNH NĂNG 3: XỬ LÝ LƯU MẬT KHẨU MỚI KHI SUBMIT
    // ==================================================================
    @GetMapping("/forgot-password")
    public String showForgotPasswordPage() {
        return "forgot-password";
    }

    @PostMapping("/reset-password")
    public String processResetPassword(@RequestParam String email,
                                       @RequestParam String otp,
                                       @RequestParam String newPassword,
                                       @RequestParam String confirmPassword,
                                       RedirectAttributes redirectAttributes) {

        // 1. Kiểm tra 2 mật khẩu có khớp không
        if (!newPassword.equals(confirmPassword)) {
            redirectAttributes.addAttribute("error", "Mật khẩu nhập lại không khớp!");
            redirectAttributes.addAttribute("email", email);
            return "redirect:/reset-password";
        }

        Optional<Users> optUser = userRepository.findByEmail(email);
        if (optUser.isEmpty()) return "redirect:/login";

        Users user = optUser.get();

        // 2. Tìm và đối chiếu mã OTP
        Optional<VerificationToken> optToken = tokenRepository.findByUserIdAndToken(user.getId(), otp);
        if (optToken.isEmpty()) {
            redirectAttributes.addAttribute("error", "Mã xác thực không chính xác!");
            redirectAttributes.addAttribute("email", email);
            return "redirect:/reset-password";
        }

        VerificationToken token = optToken.get();

        // 3. Kiểm tra hạn sử dụng của mã
        if (token.getExpiryDate().isBefore(LocalDateTime.now())) {
            redirectAttributes.addAttribute("error", "Mã xác thực đã hết hạn (quá 15 phút)!");
            redirectAttributes.addAttribute("email", email);
            return "redirect:/reset-password";
        }

        // 4. Mã ĐÚNG -> Tiến hành cập nhật mật khẩu mới
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        // 5. Dọn dẹp mã OTP đã sử dụng
        tokenRepository.delete(token);

        redirectAttributes.addAttribute("success", "Đổi mật khẩu thành công! Vui lòng đăng nhập lại với mật khẩu mới.");
        return "redirect:/login";
    }

}